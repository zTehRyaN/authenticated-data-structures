package it.uniroma3.pipelineIntegrity.authenticator

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedAbstractActor
import akka.event.Logging
import akka.event.LoggingAdapter
import it.uniroma3.ads.proof.Proof
import it.uniroma3.pipelineIntegrity.authenticator.Messages.ProofVerified
import it.uniroma3.pipelineIntegrity.authenticator.Messages.VerifyProof

class ProofVerifier(private val rootHashCalculator: ActorRef) : UntypedAbstractActor() {

    companion object {
        fun props(rootHashCalculator: ActorRef) : Props = Props.create(ProofVerifier::class.java, rootHashCalculator)
    }

    private val log : LoggingAdapter = Logging.getLogger(context.system, this) //utilizzato per logging

    override fun preStart() {
        //log.info("ProofVerifier started")
    }

    override fun onReceive(message: Any?) {
        when (message) {
            is VerifyProof -> {
                val (proof, rootHashOld, oldValues) = message

                //log.info("Received message VerifyProof from $sender")

                if (verifyProof(proof, rootHashOld, oldValues)) {

                    //log.info("proof Verified!")

                    rootHashCalculator.tell(ProofVerified(), self)
                } else {
                    //inviare al rootHashCalculator esito negativo
                    //log.error("Problems in proof!!!")
                    throw Exception("Proof problem")
                }
            }
        }
    }

    /**
     * La funzione verifica la proof
     */
    private fun verifyProof(proof: Proof<ByteArray, String, Any>, rootHashOld: ByteArray, oldValues: List<Pair<String, Any?>>) : Boolean {
        //calcolo il root hash a partire dai vecchi valori
        val rootHashFromOldValues = proof.rootHash()
        // verifico se i due root hash solo uguali
        //log.info("RootHash from old values: [${rootHashFromOldValues.asList().hashCode()}]")
        //log.info("RootHash old received: [${rootHashOld.asList().hashCode()}]")

        return rootHashFromOldValues contentEquals rootHashOld
    }
}