package it.uniroma3.pipelineIntegrity.client

import java.io.Serializable

/**
 * Classe Messages
 *
 * Contiene tutti i messaggi che è possibile inviare dagli attori del client
 *
 */
class Messages {

    data class Read(val key: String) : Serializable

    data class Update(val key: String, val value: Any) : Serializable

}