package it.uniroma3.pipelineIntegrity.authenticator

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedAbstractActor
import akka.event.Logging
import akka.event.LoggingAdapter
import it.uniroma3.pipelineIntegrity.authenticator.Messages.ChainVerified
import it.uniroma3.pipelineIntegrity.authenticator.Messages.VerifyChain
import it.uniroma3.pipelineIntegrity.utils.KeyUtils
import it.uniroma3.pipelineIntegrity.utils.chain.SubChain

class ChainVerifier(private val chainCompactor: ActorRef) : UntypedAbstractActor() {

    companion object {
        fun props(chainCompactor: ActorRef): Props = Props.create(ChainVerifier::class.java, chainCompactor)
    }

    private val log: LoggingAdapter = Logging.getLogger(context.system, this) //utilizzato per logging

    override fun preStart() {
        //log.info("ChainVerifier started")
    }

    override fun onReceive(message: Any?) {
        when (message) {
            is VerifyChain -> {
                val (chain) = message

                //log.info("Received message VerifyChain from $sender")

                if (verifyChain(chain)) {

                    //log.info("Chain verified!")

                    chainCompactor.tell(ChainVerified(), self)
                } else {
                    //inviare al chain compactor esito negativo
                    //log.error("Problems in chain!!! Chain: $chain")
                    throw Exception("Chain problem $chain")

                }
            }
        }
    }

    /**
     * La funzione verifica le firme della chain.
     */
    private fun verifyChain(chain: SubChain): Boolean {
        return chain.size() == 0 || chain.verify(KeyUtils.publicKey("${System.getProperty("cert.dir")}/public_key.pem"), "SHA256withRSA")
    }

}