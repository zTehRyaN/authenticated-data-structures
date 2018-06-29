package it.uniroma3.pipelineIntegrity.server

import akka.actor.Props
import akka.actor.UntypedAbstractActor
import it.uniroma3.pipelineIntegrity.server.Messages.Log
import java.io.BufferedWriter
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LogManager : UntypedAbstractActor() {

    companion object {
        fun props() = Props.create(LogManager::class.java)
    }

    private val queue: Queue<Pair<String, Long>> = LinkedList<Pair<String, Long>>()

    override fun onReceive(message: Any?) {
        when (message) {
            is Log -> {
                val (property, value) = message
                queue.add(Pair(property, value))
            }
        }
    }

    override fun postStop() {
        // prima di morire salva su file i log

        val path = System.getProperty("server.log.path")
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        val file = "$path/log${LocalDateTime.now().format(formatter)}.log"

        val bw = BufferedWriter(FileWriter(file))

        println("Scrivo il log su $file")

        for ((property, value) in queue) {
            bw.write("$property\t$value\n")
        }

        bw.close()

        println("File scritto correttamente!")

    }

}