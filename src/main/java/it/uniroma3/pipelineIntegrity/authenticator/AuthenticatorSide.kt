package it.uniroma3.pipelineIntegrity.authenticator

import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
 * Rappresenta il sistema dell'autenticatore
 */
class AuthenticatorSide private constructor(private val ip: String, private val port: Int, private val number_autenticators: Int, private val dir_certificates: String) {

    init {
        init()
    }

    companion object {
        private var instance: AuthenticatorSide? = null

        /**
         * Crea un sistema per l'authenticator side
         *
         * @param number_autenticators -> numero di autenticatori nel sistema
         * @param dir_certificates -> directory contentente i certificati usati dall'authenticator
         */
        @Synchronized
        fun with(ip: String, port: Int, number_autenticators: Int, dir_certificates: String): AuthenticatorSide {
            if (instance == null)
                instance = AuthenticatorSide(ip, port, number_autenticators, dir_certificates)
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

    /**
     * Inizializza il sistema con number_authenticator attori Authenticator
     */
    private fun init() {
        System.setProperty("cert.dir", dir_certificates)
        val system = ActorSystem.create(System.getProperty("system.authenticator.name"), createConfig(ip, port))

        for (i in 0 until number_autenticators) {
            system.actorOf(Authenticator.props(), "${System.getProperty("system.authenticator.authenticator_actor.name")}$i")
        }
    }

}
