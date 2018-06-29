package it.uniroma3.pipelineIntegrity.authenticator

import it.uniroma3.ads.proof.Proof
import it.uniroma3.pipelineIntegrity.server.Messages.Update
import it.uniroma3.pipelineIntegrity.utils.chain.SubChain
import java.io.Serializable

/**
 * Classe Messages
 *
 * Contiene tutti i messaggi che è possibile inviare dagli attori dell'authenticator
 *
 */
class Messages {

    /**
     * Messaggio Calculate Root Hash
     *
     * Inviato dall'attore Authenticator all'attore RootHashCalculator per richiedere il calcolo del root hash
     *
     * @param proof
     * @param rootHashOld
     */
    data class CalculateRootHash(val proof: Proof<ByteArray, String, Any>, val rootHashOld: ByteArray, val signatureRootHash: ByteArray? = null, val updates: List<Update>)

    /**
     * Messaggio Compact Chain
     *
     * Inviato dall'attore Authenticator all'attore ChainCompactor per la compattazione della chain
     *
     * @param chain
     */
    data class CompactChain(val chain: SubChain)

    /**
     * Messaggio Root Hash Calculated
     *
     * Inviato dall'attore RootHashCalculator all'attore Authenticator contenente il root hash rnew e la firma della dipendenza tra rold e rnew
     *
     * @param chain
     */
    data class RootHashCalculated(val rootHash: ByteArray, val signatureRotHash: ByteArray?, val signatureDependency: ByteArray?)

    /**
     * Messaggio Chain Compacted
     *
     * Inviato dall'attore ChainCompactor all'attore Authenticator contenente la catena compattata in un root hash
     *
     * @param chain
     */
    data class ChainCompacted(val rootHash: ByteArray?, val signature: ByteArray?)

    /**
     * Messaggio Verify proof
     *
     * Inviato dall'attore Authenticator all'attore ProofVerifier contenente la proof di cui verificare la firma
     *
     * @param proof
     */
    data class VerifyProof(val proof: Proof<ByteArray, String, Any>, val rootHashOld: ByteArray, val oldValues: List<Pair<String, Any?>>)

    /**
     * Messaggio proof Verified
     *
     * Inviato dall'attore ProofVerifier all'attore RootHashCalculator come esito positivo per la verifica della firma della proof
     *
     */
    class ProofVerified

    /**
     * Messaggio Verify Chain
     *
     * Inviato dall'attore Authenticator all'attore ChainVerifier contenente la chain di cui verificare la firma
     *
     * @param chain
     */
    data class VerifyChain(val chain: SubChain)

    /**
     * Messaggio Chain Verified
     *
     * Inviato dall'attore ChainVerifier all'attore ChainCompactor come esito positivo per la verifica della firma della chain
     */
    class ChainVerified

    /**
     * Messaggio Authentication Finished
     *
     * Inviato dall'attore Authenticator all'attore AuthManager come risposta alla richiesta di autenticazione da parte del server
     *
     * @param rootHashCompacted -> root hash compattato dalla chain
     * @param signatureRHCompacted -> firma del root hash compattato dalla chain
     * @param newRootHash -> nuovo root hash dei nuovi valori
     * @param signatureDependency -> firma della dipendenza root hash old | root hash new
     */
    data class AuthenticationFinished(val rootHashCompacted: ByteArray?, val signatureRHCompacted: ByteArray?, val newRootHash: ByteArray, val signatureNewRootHash: ByteArray? = null, val signatureDependency: ByteArray?, val id: Int, val auth_number: Int) : Serializable

}