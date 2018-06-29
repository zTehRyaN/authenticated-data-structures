package it.uniroma3.pipelineIntegrity.server

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedAbstractActor
import akka.event.Logging
import akka.event.LoggingAdapter
import it.uniroma3.pipelineIntegrity.client.Messages.Update
import it.uniroma3.pipelineIntegrity.server.Messages.*
import java.util.*

class ServerUpdate : UntypedAbstractActor() {

    companion object {
        fun props() : Props = Props.create(ServerUpdate::class.java)
    }

    private lateinit var updateReceiver: ActorRef
    private var canSend = true // whether updates can be forwarded to ADSManager; if false, updates are queued
    private val updates : Queue<Update> = LinkedList<Update>() // queue of pending updates

    private val log : LoggingAdapter = Logging.getLogger(context.system, this) // used for logging

    override fun preStart() {
        log.info("ServerUpdate started")
    }

    override fun postStop() {
        log.info("ServerUpdate stopped")
    }

    override fun onReceive(message: Any?) {
        when (message) {
            is Update -> {
                val (key, value) = message

                log.info("Update ($key, $value) received from $sender")

                if (canSend)
                    updateReceiver.tell(Messages.Update(key, value), self)
                else
                    updates.add(message)
            }

            is NewUpgradableActor -> {
                // switch to the new actor that should receive updates
                updateReceiver = message.actor
                canSend = true

                log.info("New UPGRADABLE actor is $updateReceiver")

                // forward all updates received in the meantime to the new ADSManager actor
                updates.forEach { update ->
                    val (key, value) = update
                    updateReceiver.tell(Messages.Update(key, value), self)
                }
                updates.removeAll({true})
            }

            is StopUpdates -> {

                log.info("I cannot send updates to $updateReceiver")

                canSend = false
                updateReceiver.tell(EndUpdates(), self)
            }

        }
    }

}