package it.uniroma3.pipelineIntegrity.client

import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class ClientSide private constructor(private val ip: String, private val port: Int, val server_ip: String, val server_port: Int, val frequency: Long, private val client_number: Int) {

    init {
        init()
    }

    companion object {

        private var instance: ClientSide? = null

        /**
         * Crea un sistema di client_number attori client
         */
        @Synchronized
        fun with(ip: String, port: Int, server_ip: String, server_port: Int, frequency: Long, client_number: Int): ClientSide {
            if (instance == null)
                instance = ClientSide(ip, port, server_ip, server_port, frequency, client_number)
            return instance!!
        }

        fun createConfig(ip: String, port: Int): Config {
            val map = mutableMapOf<String, String>()
            map["akka.actor.provider"] = "akka.remote.RemoteActorRefProvider"
            map["akka.remote.transport"] = "akka.remote.netty.NettyRemoteTransport"
            map["akka.remote.netty.tcp.hostname"] = ip
            map["akka.remote.netty.tcp.port"] = port.toString()
            return ConfigFactory.parseMap(map)
        }

    }

    private fun init() {
        val system = ActorSystem.create(System.getProperty("system.client.name"), createConfig(ip, port))

        for (i in 0 until client_number) {
            system.actorOf(Client.props(this), "${System.getProperty("system.client.client_actor.name")}$i")
        }
    }

}


