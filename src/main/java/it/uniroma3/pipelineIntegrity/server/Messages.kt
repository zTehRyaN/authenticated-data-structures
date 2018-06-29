package it.uniroma3.pipelineIntegrity.server

import akka.actor.ActorRef
import it.uniroma3.ads.proof.Proof
import it.uniroma3.pipelineIntegrity.utils.chain.SubChain
import java.io.Serializable

/**
 * Classe Messages
 *
 * Contiene tutti i messaggi che è possibile inviare dagli attori del server
 *
 */
class Messages {

    /**
     * Messaggio Authentic
     *
     * Inviato dall'attore AuthManager all'attore Authenticar come richiesta di autenticazione
     *
     * @param chain
     * @param rootHashOld
     * @oaram signatureRootHash
     * @param proof
     * @param updates
     * @param oldValues
     */
    data class Authentic(val chain: SubChain, val rootHashOld: ByteArray, val signatureRootHash: ByteArray?, val proof: Proof<ByteArray, String, Any>, val updates: List<Update>, val oldValues: List<Pair<String, Any?>>, val id: Int, val auth_number: Int) : Serializable

    /**
     * Messaggio Read
     *
     * Inviato dall'attore ServerRead all'attore ADSManager come richiesta di lettura
     *
     * @param key
     */
    data class Read(val key: String)

    /**
     * Messaggio Reply
     *
     * Inviato da ADSManager al client che ha richiesto la lettura
     */
    data class Reply(val value: Any?, val proof: Proof<ByteArray, String, Any>, val chain: SubChain)

    /**
     * Messaggio Update
     *
     * Inviato dall'attore ServerUpdate all'attore ADSManager come richiesta di update
     *
     * @param key
     * @param value
     */
    data class Update(val key: String, val value: Any) : Serializable

    /**
     * Messaggio NewUpgradableActor
     *
     * Inviato dall'attore ADSManager all'attore ServerUpdate per indicare il nuovo attore a cui inviare gli aggiornamenti.
     *
     * @param actor
     */
    data class NewUpgradableActor(val actor: ActorRef)

    /**
     * Messaggio NewReadableActor
     *
     * Inviato dall'attore ADSManager all'attore ServerRead per indicare il nuovo attore che può ricevere delle read.
     *
     * @param actor
     */
    data class NewReadableActor(val actor: ActorRef)

    /**
     * Messaggio EndUpdate
     *
     * Inviato dall'attore ServerUpdate all'attore ADSManager per indicare che è finito il flusso di update
     *
     */
    class EndUpdates

    /**
     * Messaggio Last Updates
     *
     * Inviato dall'attore ADSManager all'attore ServerUpdate per indicare che quest'ultimo deve stoppare il flusso di update
     *
     */
    class StopUpdates

    data class ManageAuthentication(val rootHashOld: ByteArray, val signatureRootHash: ByteArray?, val proof: Proof<ByteArray, String, Any>, val updates: List<Update>, val oldValues: List<Pair<String, Any?>>, val ID: Int)

    data class ChainUpdated(val chain: SubChain)

    data class Log(val property: String, val value: Long)

}