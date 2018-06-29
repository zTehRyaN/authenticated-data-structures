package it.uniroma3.pipelineIntegrity.server

import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.UntypedAbstractActor
import akka.event.Logging
import akka.event.LoggingAdapter
import it.uniroma3.pipelineIntegrity.client.Messages.Read
import it.uniroma3.pipelineIntegrity.server.Messages.NewReadableActor

class ServerRead : UntypedAbstractActor() {

    companion object {
        fun props(): Props = Props.create(ServerRead::class.java)
    }

    private var readReceiver: ActorRef? = null

    private val log: LoggingAdapter = Logging.getLogger(context.system, this) // utilizzato per logging

    override fun preStart() {
        //log.info("ServerRead started")
    }

    override fun postStop() {
        //log.info("ServerRead stopped")
    }

    override fun onReceive(message: Any?) {
        when (message) {
            is Read -> {
                val (key) = message

                //log.info("Received read ($key) message from $sender")

                readReceiver?.tell(Messages.Read(key), sender)
            }

            is NewReadableActor -> {

                // uccido il precedente adsManager
                if (readReceiver != null) {
                    readReceiver?.tell(PoisonPill.getInstance(), sender)
                }

                readReceiver = message.actor

                //log.info("New READABLE actor is $readReceiver")
            }

        }
    }


}