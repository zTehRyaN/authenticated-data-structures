package it.uniroma3.pipelineIntegrity.server

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.BootstrapSetup
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import it.uniroma3.ads.auth.ADS
import it.uniroma3.ads.auth.AuthSkipList

class ServerSide private constructor(val ip: String, val port: Int, val auth_ip: String, val auth_port: Int, val schedule_duration: Long, val number_authenticator: Int, val try_again_time: Long) {

    init {
        init()
    }

    lateinit var logManager : ActorRef //attore per logging
    val systemStartup = System.currentTimeMillis()

    companion object {
        private var instance: ServerSide? = null

        @Synchronized
        fun with(ip: String, port: Int, auth_ip: String, auth_port: Int, schedule_duration: Long, number_authenticator: Int, try_again_time: Long): ServerSide {
            if (instance == null)
                instance = ServerSide(ip, port, auth_ip, auth_port, schedule_duration, number_authenticator, try_again_time)
            return instance!!
        }

        fun createConfig(ip: String, port: Int): Config {
            val map = mutableMapOf<String, String>()
            map["akka.actor.provider"] = "akka.remote.RemoteActorRefProvider"
            map["akka.remote.transport"] = "akka.remote.netty.NettyRemoteTransport"
            map["akka.remote.netty.tcp.hostname"] = ip
            map["akka.remote.netty.tcp.port"] = port.toString()
            map["akka.remote.netty.tcp.maximum-frame-size"] = "1280000"
            map["akka.remote.netty.tcp.message-frame-size"] = "1280000"
            return ConfigFactory.parseMap(map)
        }

    }

    private fun init() {
        val system = ActorSystem.create(System.getProperty("system.server.name"), createConfig(ip, port))

        val serverUpdate = system.actorOf(ServerUpdate.props(), System.getProperty("system.server.serverUpdate_actor.name"))
        val serverRead = system.actorOf(ServerRead.props(), System.getProperty("system.server.serverRead_actor.name"))
        val authManager = system.actorOf(AuthManager.props(this), System.getProperty("system.server.authManager_actor.name"))
        val adsManager = system.actorOf(ADSManager.props(this, createADS(System.getProperty("server.ads.initial_values").toInt())), "${System.getProperty("system.server.ADSManager_actor.name")}0")
        logManager = system.actorOf(LogManager.props(), System.getProperty("system.server.LogManager_actor.name"))

        serverUpdate.tell(it.uniroma3.pipelineIntegrity.server.Messages.NewUpgradableActor(adsManager), null)

        println("Server Side started")
    }

    /**
     * Crea un ads con initial_values valori iniziali
     */
    private fun createADS(initial_values: Int): ADS<ByteArray, String, Any> {
        val ads = AuthSkipList()

        for (i in 0 until initial_values) {
            ads.add("key$i", "initial_value$i")
        }

        return ads.applyDeltas()
    }

}