package it.uniroma3.ads.proof

import java.io.Serializable

interface Proof<H, K, V> : Serializable {

    /**
     * Interface property tracking the keys this proof is associated with.
     */
    var keys: MutableSet<K>

    /**
     * Returns the rootHash of the Proof
     *
     * @return rootHash H
     *
     */
    fun rootHash(): H

    /**
     * Merges two proofs that refer to the same structure at the same point in time
     *
     * @param proof, the other proof to merge
     *
     * @return proof<H, K, V>, the union of the two proofs
     *
     */
    fun union(proof: Proof<H, K, V>): Proof<H, K, V>

    /**
     * If no values are provided, the operation is read-only, so the rootHash is computed and returned.
     * If V != null is also present, the operation is a write. An updated structure is returned together with its new rootHash.
     *
     * @param list
     *
     * @return rootHash
     *
     */
    fun rootHash(list: List<Pair<K, V?>>): Pair<Proof<H, K, V>, H>

    /**
     *  Prints to screen the path for every key the proof is composed of
     */
    fun printProof()

}