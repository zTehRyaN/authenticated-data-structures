package it.uniroma3.pipelineIntegrity.main

import it.uniroma3.pipelineIntegrity.authenticator.AuthenticatorSide
import it.uniroma3.pipelineIntegrity.client.ClientSide
import it.uniroma3.pipelineIntegrity.server.ServerSide

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        usage()
        return
    }

    System.getProperties().load(ClassLoader.getSystemResourceAsStream("config.properties"))

    when (args[0]) {

        "client" -> {
            ClientSide.with(args[1], args[2].toInt(), args[3], args[4].toInt(), args[5].toLong(), args[6].toInt())
        }

        "server" -> {
            ServerSide.with(args[1], args[2].toInt(), args[3], args[4].toInt(), args[5].toLong(), args[6].toInt(), args[7].toLong())
        }

        "auth" -> {
            AuthenticatorSide.with(args[1], args[2].toInt(), args[3].toInt(), args[4])
        }

        "help" -> {
            usage()
            println("\n\tclient\tclient_ip client_port server_ip server_port frequency client_number")
            println("\n\tserver\tserver_ip server_port auth_ip auth_port schedule_duration auth_number try_again_time")
            println("\n\tauth\tauth_ip auth_port auth_number dir_certificates\n")
        }

        else -> {
            println("Paramentro non riconosciuto")
        }

    }


}

fun usage() {
    println("Usage: ${System.getProperty("sun.java.command")} client|server|auth [params...]")
}