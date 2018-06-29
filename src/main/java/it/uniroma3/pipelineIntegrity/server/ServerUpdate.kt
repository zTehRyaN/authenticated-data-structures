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
    private var canSend = true // indica se può inviare update ad ADSManager, se false inserisce gli update nella coda
    private val updates : Queue<Update> = LinkedList<Update>() //coda degli aggiornamenti

    private val log : LoggingAdapter = Logging.getLogger(context.system, this) //utilizzato per logging

    override fun preStart() {
        //log.info("ServerUpdate started")
    }

    override fun postStop() {
        //log.info("ServerUpdate stopped")
    }

    override fun onReceive(message: Any?) {
        when (message) {
            is Update -> {
                val (key, value) = message

                //log.info("Update ($key, $value) received from $sender")

                if (canSend)
                    updateReceiver.tell(Messages.Update(key, value), self)
                else
                    updates.add(message)
            }

            is NewUpgradableActor -> {
                // aggiorno il nuovo attore a cui inviare gli update
                updateReceiver = message.actor
                canSend = true

                //log.info("New UPGRADABLE actor is $updateReceiver")

                // invio tutti gli update ricevuti nel frattempo al nuovo attore ADSManager
                updates.forEach { update ->
                    val (key, value) = update
                    updateReceiver.tell(Messages.Update(key, value), self)
                }
                updates.removeAll({true})
            }

            is StopUpdates -> {

                //log.info("I cannot send updates to $updateReceiver")

                canSend = false
                updateReceiver.tell(EndUpdates(), self)
            }

        }
    }

}