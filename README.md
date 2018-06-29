# Authenticated Data Structure

Authenticated skip list with a pipelined Akka-based distributed authentication backend.

This repository implements the prototype for the paper:

> **Pipeline-integrity: Scaling the use of authenticated data structures up to the cloud**
Pennino, Pizzonia, Griscioli. *Future Generation Computer Systems*, Elsevier (2018).
https://www.sciencedirect.com/science/article/abs/pii/S0167739X18328048

It was developed as my bachelor thesis at Roma Tre, in collaboration with the paper authors, contributing the practical implementation that backed the experimental section of the paper.

>[!WARNING]  
>This codebase is academic research code, not production software. It was the heavy-lifting groundwork for the experiments in the paper and for > downstream PhD theses. The focus was correctness of the protocol and the cryptographic structure, not the polish you would expect from a product:
> 
> - No automated test suite. Verification was done by running the three distributed components together and inspecting behavior.
> - No retry logic, no fault tolerance, no defensive validation at boundaries.
> - Some prototype scaffolding, hardcoded constants, and minimal logging.
> 
>If you want to understand the protocol, this is the right place. If you want to deploy authenticated storage, you almost certainly want something > else.

## The problem

Untrusted cloud storage. A client outsources a key-value store to a cloud provider that it does not trust. Whenever the client reads a value, it wants a cryptographic proof that the answer is consistent with every previous write the client has made, even though the storage is operated by someone potentially malicious.

The standard solution is an **authenticated data structure (ADS)**: the data structure carries a small digest (a "root hash") that summarizes the entire state. Any read comes with a proof that lets the client recompute the digest from just the touched portion. If the proof recomputes the digest the client has signed, the answer must be authentic.

The contribution of the paper, and of this implementation, is making that scheme **pipelined** so that the authenticator (the trusted component that signs the digest) does not become a bottleneck. New writes can keep being collected and prepared for the next batch while the previous batch is still being authenticated.

## Background: skip lists

A skip list is a probabilistic, ordered key-value store. Conceptually it is a sorted linked list at level 0, with several "express lanes" stacked on top: each higher level skips over more nodes, so a search starting at the top moves quickly across the list and only descends when it overshoots.

Each inserted key gets a tower of random height (a fair coin is flipped until tails appears, so on average half the keys exist only at level 0, a quarter reach level 1, an eighth reach level 2, and so on). Searches, insertions and deletions all run in O(log n) expected time without any rebalancing.

```
Level 3:  H -----------------> 17 -------------------> NIL
          |                    |
Level 2:  H ------> 9 -------> 17 -------> 25 -------> NIL
          |         |          |           |
Level 1:  H -> 6 -> 9 -> 12 -> 17 -> 21 -> 25 -> 30 -> NIL
          |    |    |    |     |     |     |     |
Level 0:  H -> 6 -> 9 -> 12 -> 17 -> 21 -> 25 -> 30 -> NIL
```

`H` is the left sentinel (the `negativeInfinity` head). To look up `21`, you start at the top-left, walk right until the next key would overshoot, drop down a level, repeat.

## What makes it authenticated

Each node carries **two** SHA-256 hashes:

- `firstHash  = SHA-256(value)`
- `secondHash = SHA-256(firstHash_right || secondHash_right)`

`secondHash` chains rightward across same-level neighbors, so the leftmost node at the topmost level (the head) ends up holding a digest that depends on every value in the structure. That is the **root hash**.

```
   head              n1               n2               NIL
 +------+         +------+         +------+         +------+
 |  f   |         |  f   |         |  f   |         | null |
 |  s   |  --->   |  s   |  --->   |  s   |  --->   | null |
 +------+         +------+         +------+         +------+
                                                         ^
                                                         secondHash is null at the right edge
```

A change to any value invalidates the `firstHash` of its node, which invalidates the `secondHash` of its left neighbor, which invalidates the `secondHash` of *that* node's left neighbor, and so on, all the way up the towers and leftward across plateaus until it reaches the head. There is no way to mutate the structure without changing the root hash.

This is **Tamassia's authenticated skip list scheme** (2003). The clever part is that a read query can produce a proof that is just the small subgraph the search path touched: the towers visited, plus the plateau neighbors needed to recompute every `secondHash` along the way. The proof is logarithmic in the size of the structure.

### Proofs

`SkipListProof` is the minimal authenticated subgraph for one or more queried keys. It supports:

- `rootHash()`: recompute the digest from the proof. The client compares this to a digest signed by the authenticator. If they match, the values inside the proof are authentic.
- `union(other)`: merge two proofs into one minimal subgraph. The server unions the per-key proofs of every key touched by a write batch.
- `rootHash(updates)`: apply hypothetical updates to the proof and return the new root hash. This is what the authenticator uses to compute the post-batch digest without ever touching the live ADS.

### Delta-based functional model

`add(k, v)` and `del(k)` do not mutate the live skip list. They append a `Triple(op, key, value)` onto a `deltas` queue and return immediately. `applyDeltas()` deep-clones the current ADS, replays the deltas in order, and returns the new ADS. The original is untouched.

This means a stable snapshot is always available for proof generation while the next batch of writes is being accumulated. It is the foundation of the pipeline.

## The distributed protocol

Three Akka actor systems, intended to run as separate processes (potentially on separate hosts):

```
+----------+              +-----------+              +---------------+
|  Client  |  <-------->  |  Server   |  <-------->  | Authenticator |
+----------+              +-----------+              +---------------+
  holds public key         untrusted                   trusted signer
  verifies proofs          stores ADS                  signs root hashes
                           batches writes
```

The client is the only consumer of authenticity. The server is assumed untrusted: it stores the ADS and serves queries, but every reply it sends is independently verifiable. The authenticator is the only component holding the private signing key. It is treated as trusted but is also designed to do as little work as possible: just verifying proofs and signing digests.

### Read flow

```
Client            Server              ServerRead             ADSManager
  |                 |                     |                      |
  |-- read(k) ----->|                     |                      |
  |                 |-- forward --------->|                      |
  |                 |                     |-- getProof(k) ------>|
  |                 |                     |<-- proof, subchain --|
  |                 |<-- proof, subchain -|                      |
  |<- value, proof, subchain              |                      |
  |
  | verify subchain with the authenticator's public key
  | recompute proof.rootHash() and compare to the latest signed digest
  | extract the value from the proof
```

The client never trusts the server. It receives the proof and the **subchain** (the relevant tail of the chain of signed root hashes) and does all verification locally.

### Write flow (batched and pipelined)

`ADSManager` is a state machine driven by its inbox:

```
UPGRADABLE  --(first write arrives, batch timer starts)-->  WAITING_END
WAITING_END --(timer expires, StopUpdates sent)---------->  FROZEN
FROZEN      --(authentication completes)---------------->  READABLE
```

Just before transitioning to FROZEN, the ADSManager **deep-clones itself** into a new UPGRADABLE instance and hands it back to ServerUpdate. The next batch starts accumulating immediately, while the previous batch is still being authenticated. This is the "clone-on-commit" pipeline.

```
Client    ServerUpdate    ADSManager(n)    AuthManager    Authenticator
  |            |                |               |               |
  |- write --->|                |               |               |
  |            |- enqueue ----->|               |               |
  |            |   ...batch...  |               |               |
  |            |                | clone self -> ADSManager(n+1) starts UPGRADABLE
  |            |                |               |               |
  |            |                |- proof + updates -> ManageAuth|
  |            |                |               |- Authentic -->|
  |            |                |               |               | (4 children
  |            |                |               |               |  in parallel:
  |            |                |               |               |  ProofVerifier
  |            |                |               |               |  RootHashCalc
  |            |                |               |               |  ChainVerifier
  |            |                |               |               |  ChainCompactor)
  |            |                |               |<- signed -----|
  |            |                |<- ChainUpdated|               |
  |            |                | -> announce to ServerRead, become READABLE
```

Inside the authenticator, four children run in parallel for every batch:

- **ProofVerifier**: checks that `proof.rootHash() == oldRootHash` (the proof is consistent with the digest already signed).
- **RootHashCalculator**: waits for ProofVerifier, then computes `newRootHash = proof.rootHash(updates)` and signs it. For batch 0 it signs `newRootHash` directly; for batch *n > 0* it signs `oldRootHash || newRootHash`, a *dependency signature* that links this batch to the previous one.
- **ChainVerifier**: verifies every signature already in the chain.
- **ChainCompactor**: waits for ChainVerifier, then if the chain has more than one entry, signs the latest root hash to produce a compacted anchor that can replace the old prefix of the chain.

Verification and compaction overlap with each other and with the next batch being collected.

### The chain

The chain is parallel `ArrayList<ByteArray>` for root hashes and signatures.

- **Batch 0** (`addRootHashCompacted`): clears the chain and inserts `(rootHash_0, sign(rootHash_0))`.
- **Batch n > 0** (`addDependency`): appends `(rootHash_n, sign(rootHash_{n-1} || rootHash_n))`. Note that SHA-256 with concatenation is **non-commutative**: `H(A || B) != H(B || A)`. Any gap or reorder in the chain breaks signature verification.
- **After compaction** (`truncate`): removes the prefix of the chain that is now superseded by the compacted anchor.

`SubChain` is the serializable view of the chain that gets sent to clients with every read reply. The client only ever needs the authenticator's public key plus a SubChain to verify any proof.

Commits are applied in batch-id order even if they arrive out of order; AuthManager buffers and replays them in sequence.

## Build

Maven with Kotlin 1.2.41.

```bash
mvn clean package
```

This produces a fat jar in `target/`.

## Run

The fat jar takes a `--type` argument selecting which of the three roles to start. Each role reads its actor system configuration from `src/main/resources/application.conf` and tunables from `src/main/resources/config.properties`.

```bash
java -jar target/<jar-name>.jar --type server
java -jar target/<jar-name>.jar --type authenticator
java -jar target/<jar-name>.jar --type client
```

Start them in roughly that order. The three components discover each other through the Akka remoting addresses configured in `application.conf`.

## Repository layout

```
src/main/java/it/uniroma3/
  ads/                          authenticated skip list and proof construction
    auth/                       AuthSkipList, AuthSkipListNode (the ADS)
    proof/                      SkipListProof (verifiable subgraph)
    skipList/                   plain skip list base classes
  pipelineIntegrity/
    main/                       entry point and CLI
    client/                     client actor
    server/                     ServerRead, ServerUpdate, ADSManager, AuthManager, LogManager
    authenticator/              Authenticator and its 4 verification children
    utils/chain/                Chain, SubChain, Commit, ChainStrategy and friends
src/main/resources/
  application.conf              Akka actor system configuration
  config.properties             actor names and runtime tunables
```
