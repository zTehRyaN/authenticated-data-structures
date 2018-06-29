package it.uniroma3.pipelineIntegrity.authenticator

import it.uniroma3.ads.proof.Proof
import it.uniroma3.pipelineIntegrity.server.Messages.Update
import it.uniroma3.pipelineIntegrity.utils.chain.SubChain
import java.io.Serializable

/**
 * Messages class
 *
 * Contains all messages that can be sent from the authenticator actors
 *
 */
class Messages {

    /**
     * CalculateRootHash message
     *
     * Sent from the Authenticator actor to the RootHashCalculator actor to request the root hash computation
     *
     * @param proof
     * @param rootHashOld
     */
    data class CalculateRootHash(val proof: Proof<ByteArray, String, Any>, val rootHashOld: ByteArray, val signatureRootHash: ByteArray? = null, val updates: List<Update>)

    /**
     * CompactChain message
     *
     * Sent from the Authenticator actor to the ChainCompactor actor to compact the chain
     *
     * @param chain
     */
    data class CompactChain(val chain: SubChain)

    /**
     * RootHashCalculated message
     *
     * Sent from the RootHashCalculator actor to the Authenticator actor, containing the new root hash rnew
     * and the signature for the dependency between rold and rnew
     *
     * @param chain
     */
    data class RootHashCalculated(val rootHash: ByteArray, val signatureRotHash: ByteArray?, val signatureDependency: ByteArray?)

    /**
     * ChainCompacted message
     *
     * Sent from the ChainCompactor actor to the Authenticator actor, containing the chain compacted into a root hash
     *
     * @param chain
     */
    data class ChainCompacted(val rootHash: ByteArray?, val signature: ByteArray?)

    /**
     * VerifyProof message
     *
     * Sent from the Authenticator actor to the ProofVerifier actor, containing the proof whose signature must be verified
     *
     * @param proof
     */
    data class VerifyProof(val proof: Proof<ByteArray, String, Any>, val rootHashOld: ByteArray, val oldValues: List<Pair<String, Any?>>)

    /**
     * ProofVerified message
     *
     * Sent from the ProofVerifier actor to the RootHashCalculator actor as a positive outcome of the proof signature verification
     *
     */
    class ProofVerified

    /**
     * VerifyChain message
     *
     * Sent from the Authenticator actor to the ChainVerifier actor, containing the chain whose signature must be verified
     *
     * @param chain
     */
    data class VerifyChain(val chain: SubChain)

    /**
     * ChainVerified message
     *
     * Sent from the ChainVerifier actor to the ChainCompactor actor as a positive outcome of the chain signature verification
     */
    class ChainVerified

    /**
     * AuthenticationFinished message
     *
     * Sent from the Authenticator actor to the AuthManager actor as a reply to the server's authentication request
     *
     * @param rootHashCompacted -> root hash obtained by compacting the chain
     * @param signatureRHCompacted -> signature of the root hash obtained by compacting the chain
     * @param newRootHash -> new root hash of the new values
     * @param signatureDependency -> signature of the dependency root hash old | root hash new
     */
    data class AuthenticationFinished(val rootHashCompacted: ByteArray?, val signatureRHCompacted: ByteArray?, val newRootHash: ByteArray, val signatureNewRootHash: ByteArray? = null, val signatureDependency: ByteArray?, val id: Int, val auth_number: Int) : Serializable

}