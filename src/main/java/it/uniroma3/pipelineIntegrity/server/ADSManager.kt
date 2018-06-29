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
     * ChangeToWaitingEnd message
     *
     * Sent from ADSManager to itself to transition from UPGRADABLE to WAITING_END
     */
    private class ChangeToWaitingEnd

    var state = State.UPGRADABLE
        private set

    private val log: LoggingAdapter = Logging.getLogger(context.system, this) // used for logging

    private val updates: MutableList<Update> = mutableListOf()
    private val oldValues: MutableList<Pair<String, Any?>> = mutableListOf()

    private val myID = ID

    private val rootHashOld = dataSet.rootHash() // old root hash before any updates, to be forwarded later to the authenticator
    private val signatureRootHash = if (myID == 0) "signature".toByteArray() else null

    private lateinit var chain: SubChain // chain used while in the READABLE state

    private val serverUpdate = context.actorSelection("/user/server-update")
    private val serverRead = context.actorSelection("/user/server-read")
    private val authManager = context.actorSelection("/user/auth-manager")


    companion object {

        /**
         * Actor creation
         *
         * @param serverUpdate, the server actor that forwards updates
         * @param serverRead, the server actor that forwards read requests
         */
        fun props(serverSide: ServerSide, dataSet: ADS<ByteArray, String, Any>): Props = Props.create(ADSManager::class.java, serverSide, dataSet)

        var ID = 0

    }

    override fun preStart() {
        log.info("ADSManager started. RootHashOld ---> ${rootHashOld.asList().hashCode()}")
    }

    override fun postStop() {
        log.info("ADSManager stopped")
    }

    override fun onReceive(message: Any?) {
        when (state) {
            State.UPGRADABLE -> {

                when (message) {
                    is Update -> {
                        manageUpdate(message)
                    }

                    is ChangeToWaitingEnd -> {

                        log.info("Change state to WAITING_END, I'm waiting for StopUpdates message")

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

                // when the update stream ends, clone the current ADSManager and send a message to ServerUpdate
                // to announce the new actor that is ready to receive updates
                    is EndUpdates -> {

                        log.info("Received EndUpdates and cloning ADSManager...")

                        state = State.FROZEN

                        authentic()

                        val newADSManager = clone()

                        log.info("ADSManager cloned")

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
     * Creates a copy of this actor that will take over receiving updates, in the UPGRADABLE state
     */
    private fun clone(): ActorRef {
        val clonedDataSet = dataSet.applyDeltas()
        dataSet = clonedDataSet
        return context.system.actorOf(ADSManager.props(serverSide, clonedDataSet), "ads-manager-${++ID}")
    }

    /**
     * After a configured number of milliseconds, sends itself a message to transition to WAITING_END,
     * also notifying ServerUpdate
     */
    private fun schedule() {
        val system = context.system
        system.scheduler().scheduleOnce(Duration.create(serverSide.schedule_duration, TimeUnit.MILLISECONDS), self, ChangeToWaitingEnd(), system.dispatcher(), self)
        log.info("Started timer of ${serverSide.schedule_duration} milliseconds")
    }

    /**
     * Handles an incoming update
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

        log.info("Received update ($key, $value) from $sender, during $state state")

    }

    /**
     * Carries out all operations needed to issue an authentication request
     */
    private fun authentic() {

        // extract the keys from the updates
        val updateKey = updates.map { u -> u.key }

        log.info("UpdateKey = $updateKey")

        // compute the proof for those keys
        val proof = dataSet.getProof(updateKey)

        // request authentication
        authManager.tell(ManageAuthentication(rootHashOld, signatureRootHash, proof, updates, oldValues, myID), self)

        log.info("Send authentication request to ${authManager.path()}")

    }

}