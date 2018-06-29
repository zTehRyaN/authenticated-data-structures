package it.uniroma3.pipelineIntegrity.server

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedAbstractActor
import akka.event.Logging
import akka.event.LoggingAdapter
import it.uniroma3.ads.auth.ADS
import it.uniroma3.pipelineIntegrity.server.Messages.*
import it.uniroma3.pipelineIntegrity.utils.chain.SubChain
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class ADSManager(private val serverSide: ServerSide, private var dataSet: ADS<ByteArray, String, Any>) : UntypedAbstractActor() {

    enum class State {
        UPGRADABLE,
        WAITING_END,
        FROZEN,
        READABLE
    }

    /**
     * Messaggio Change to WAITING_END
     *
     * Inviato da ADSManager a se stesso per il passaggio da UPGRADABLE ad WAITING_END
     */
    private class ChangeToWaitingEnd

    var state = State.UPGRADABLE
        private set

    private val log: LoggingAdapter = Logging.getLogger(context.system, this) //utilizzato per logging

    private val updates: MutableList<Update> = mutableListOf()
    private val oldValues: MutableList<Pair<String, Any?>> = mutableListOf()

    private val myID = ID

    private val rootHashOld = dataSet.rootHash() //root hash old prima di tutti gli aggiornamenti, da inviare successiamente all'authenticator
    private val signatureRootHash = if (myID == 0) "firma".toByteArray() else null

    private lateinit var chain: SubChain // chain utilizzato nello stato READABLE

    private val serverUpdate = context.actorSelection("/user/server-update")
    private val serverRead = context.actorSelection("/user/server-read")
    private val authManager = context.actorSelection("/user/auth-manager")


    companion object {

        /**
         * Creazione attore
         *
         * @param serverUpdate, ovvero l'attore del server che invia gli aggiornamenti
         * @param serverRead, ovvero l'attore del server che riceve le richieste di read
         */
        fun props(serverSide: ServerSide, dataSet: ADS<ByteArray, String, Any>): Props = Props.create(ADSManager::class.java, serverSide, dataSet)

        var ID = 0

    }

    override fun preStart() {
        //log.info("ADSManager started. RootHashOld ---> ${rootHashOld.asList().hashCode()}")
    }

    override fun postStop() {
        //log.info("ADSManager stopped")
    }

    override fun onReceive(message: Any?) {
        when (state) {
            State.UPGRADABLE -> {

                when (message) {
                    is Update -> {
                        manageUpdate(message)
                    }

                    is ChangeToWaitingEnd -> {

                        //log.info("Change state to WAITING_END, I'm waiting for StopUpdates message")

                        state = State.WAITING_END
                        serverUpdate.tell(StopUpdates(), self)

                    }

                    else -> {
                        log.info("Dropped message $message")
                    }
                }

            }

            State.WAITING_END -> {
                when (message) {

                // se termina il flusso di update, clono l'attuale ADSManager e invio un messaggio a ServerUpdate,
                // per informarlo del nuovo attore che è pronto a ricevere gli update
                    is EndUpdates -> {

                        //log.info("Received EndUpdates and cloning ADSManager...")

                        state = State.FROZEN

                        authentic()

                        val newADSManager = clone()

                        //log.info("ADSManager cloned")

                        serverUpdate.tell(NewUpgradableActor(newADSManager), self)

                    }

                    is Update -> {
                        manageUpdate(message)
                    }

                }
            }

            State.FROZEN -> {
                when (message) {
                    is ChainUpdated -> {

                        val (chain) = message
                        this.chain = chain

                        state = State.READABLE

                        log.info("ADS authenticated. Now the actor is in READABLE state")
                        serverRead.tell(NewReadableActor(self), self)

                    }
                    else -> {
                        log.info("Dropped message because of I'm in FROZEN state")
                    }
                }
            }

            State.READABLE -> {

                when (message) {
                    is Read -> {
                        val (key) = message

                        val (value, proof) = dataSet.getWithProof(key)

                        sender.tell(Reply(value, proof, chain), self)

                    }
                    else -> {
                        log.info("Dropped message because of I can receive only Read messages")
                    }
                }

            }

        }

    }

    /**
     * Crea un attore copia di se stesso che riceverà lui gli aggiornamenti, con stato UPGRADABLE
     */
    private fun clone(): ActorRef {
        val clonedDataSet = dataSet.applyDeltas()
        dataSet = clonedDataSet
        return context.system.actorOf(ADSManager.props(serverSide, clonedDataSet), "ads-manager-${++ID}")
    }

    /**
     * Dopo tot millisecondi si autoinvia un messaggio per passare allo stato WAITING_END, informando anche ServerUpdate
     */
    private fun schedule() {
        val system = context.system
        system.scheduler().scheduleOnce(Duration.create(serverSide.schedule_duration, TimeUnit.MILLISECONDS), self, ChangeToWaitingEnd(), system.dispatcher(), self)
        //log.info("Started timer of ${serverSide.schedule_duration} milliseconds")
    }

    /**
     * Metodo per la gestione dell'update in arrivo
     */
    private fun manageUpdate(update: Update) {
        updates.add(update)
        if (updates.size == 1) {
            serverSide.logManager.tell(Log("[$myID] first_message", System.currentTimeMillis() - serverSide.systemStartup), self)
            schedule()
        }

        val (key, value) = update
        oldValues.add(Pair(key, dataSet.getValue(key)))

        dataSet.add(key, value)

        //log.info("Received update ($key, $value) from $sender, during $state state")

    }

    /**
     * Svolge tutte le operazioni per la richiesta di autenticazione
     */
    private fun authentic() {

        // estrapolo le chiavi dagli update
        val updateKey = updates.map { u -> u.key }

        //log.info("UpdateKey = $updateKey")

        // calcolo la proof delle chiavi
        val proof = dataSet.getProof(updateKey)

        // richiedo l'autenticazione
        authManager.tell(ManageAuthentication(rootHashOld, signatureRootHash, proof, updates, oldValues, myID), self)

        //log.info("Send authentication request to ${authManager.path()}")

    }

}