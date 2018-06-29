package it.uniroma3.pipelineIntegrity.client

import akka.actor.ActorRef
import akka.actor.ActorSelection
import akka.actor.Props
import akka.actor.UntypedAbstractActor
import it.uniroma3.pipelineIntegrity.client.Messages.Update
import scala.concurrent.duration.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class Client(private val clientSide: ClientSide) : UntypedAbstractActor() {

    private lateinit var serverUpdate: ActorSelection
    private lateinit var serverRead: ActorRef

    companion object {

        fun props(clientSide: ClientSide): Props = Props.create(Client::class.java, clientSide)
    }

    override fun onReceive(message: Any?) {
        TODO("not implemented")
    }

    /**
     * Invia dei messaggi al server in base alla frequenza
     */
    private fun send_messages() {

        val system = context.system

        var i = 0
        val values = System.getProperty("server.ads.initial_values").toInt()
        val rand = Random()

        system.scheduler().schedule(Duration.create(1, TimeUnit.SECONDS),
                Duration.create(clientSide.frequency, TimeUnit.MILLISECONDS), Runnable {

            i = rand.nextInt(values)
            serverUpdate.tell(Update("key$i", "value-${System.nanoTime()}"), self)


        }, system.dispatcher())

    }

    override fun preStart() {
        serverUpdate = context.actorSelection("akka.tcp://${System.getProperty("system.server.name")}@${clientSide.server_ip}:${clientSide.server_port}/user/${System.getProperty("system.server.serverUpdate_actor.name")}")
        send_messages()
    }

}