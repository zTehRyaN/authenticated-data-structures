package it.uniroma3.pipelineIntegrity.client

import java.io.Serializable

/**
 * Messages class
 *
 * Contains all messages that can be sent from the client actors
 *
 */
class Messages {

    data class Read(val key: String) : Serializable

    data class Update(val key: String, val value: Any) : Serializable

}