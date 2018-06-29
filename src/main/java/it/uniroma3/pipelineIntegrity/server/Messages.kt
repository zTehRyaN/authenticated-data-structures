package it.uniroma3.pipelineIntegrity.server

import akka.actor.ActorRef
import it.uniroma3.ads.proof.Proof
import it.uniroma3.pipelineIntegrity.utils.chain.SubChain
import java.io.Serializable

/**
 * Messages class
 *
 * Contains all messages that can be sent from the server actors
 *
 */
class Messages {

    /**
     * Authentic message
     *
     * Sent from the AuthManager actor to the Authenticator actor as an authentication request
     *
     * @param chain
     * @param rootHashOld
     * @param signatureRootHash
     * @param proof
     * @param updates
     * @param oldValues
     */
    data class Authentic(val chain: SubChain, val rootHashOld: ByteArray, val signatureRootHash: ByteArray?, val proof: Proof<ByteArray, String, Any>, val updates: List<Update>, val oldValues: List<Pair<String, Any?>>, val id: Int, val auth_number: Int) : Serializable

    /**
     * Read message
     *
     * Sent from the ServerRead actor to the ADSManager actor as a read request
     *
     * @param key
     */
    data class Read(val key: String)

    /**
     * Reply message
     *
     * Sent from ADSManager to the client that issued the read
     */
    data class Reply(val value: Any?, val proof: Proof<ByteArray, String, Any>, val chain: SubChain)

    /**
     * Update message
     *
     * Sent from the ServerUpdate actor to the ADSManager actor as an update request
     *
     * @param key
     * @param value
     */
    data class Update(val key: String, val value: Any) : Serializable

    /**
     * NewUpgradableActor message
     *
     * Sent from the ADSManager actor to the ServerUpdate actor to announce the new actor that should receive updates.
     *
     * @param actor
     */
    data class NewUpgradableActor(val actor: ActorRef)

    /**
     * NewReadableActor message
     *
     * Sent from the ADSManager actor to the ServerRead actor to announce the new actor that can receive reads.
     *
     * @param actor
     */
    data class NewReadableActor(val actor: ActorRef)

    /**
     * EndUpdates message
     *
     * Sent from the ServerUpdate actor to the ADSManager actor to signal the end of the update stream
     *
     */
    class EndUpdates

    /**
     * StopUpdates message
     *
     * Sent from the ADSManager actor to the ServerUpdate actor to instruct it to stop the update stream
     *
     */
    class StopUpdates

    data class ManageAuthentication(val rootHashOld: ByteArray, val signatureRootHash: ByteArray?, val proof: Proof<ByteArray, String, Any>, val updates: List<Update>, val oldValues: List<Pair<String, Any?>>, val ID: Int)

    data class ChainUpdated(val chain: SubChain)

    data class Log(val property: String, val value: Long)

}