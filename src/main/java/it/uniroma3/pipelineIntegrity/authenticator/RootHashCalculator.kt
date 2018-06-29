package it.uniroma3.pipelineIntegrity.authenticator

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedAbstractActor
import akka.event.Logging
import akka.event.LoggingAdapter
import it.uniroma3.ads.proof.Proof
import it.uniroma3.pipelineIntegrity.authenticator.Messages.*
import it.uniroma3.pipelineIntegrity.server.Messages.Update
import it.uniroma3.pipelineIntegrity.utils.KeyUtils
import java.security.Signature

class RootHashCalculator(private val authenticator : ActorRef) : UntypedAbstractActor() {

    private var newRootHash: ByteArray? = null
    private var proofVerified: Boolean? = null
    private lateinit var rootHashOld : ByteArray
    private var signatureRootHashOld: ByteArray? = null

    companion object {
        fun props(authenticator: ActorRef): Props = Props.create(RootHashCalculator::class.java, authenticator)
    }

    private val log : LoggingAdapter = Logging.getLogger(context.system, this) //utilizzato per logging

    override fun preStart() {
        //log.info("RootHashCalculator started")
    }

    override fun onReceive(message: Any?) {
        when (message) {
            is CalculateRootHash -> {
                val (proof, rootHashOld, signatureRootHashOld, updates) = message

                this.rootHashOld = rootHashOld
                this.signatureRootHashOld = signatureRootHashOld

                //log.info("Received message CalculateRootHash from $sender, (rootHashOld: ${rootHashOld.asList().hashCode()}, signatureRootHashOld: ${signatureRootHashOld?.asList()?.hashCode()}")

                newRootHash = calculateRootHash(proof, updates)


                //log.info("RootHash calculated. newRootHash = ${newRootHash?.asList()?.hashCode()}")

                if (proofVerified == true) {

                    replyToAuth()

                    //log.info("A. Send message RootHashCalculated to $sender")

                }
            }
            is ProofVerified -> {
                proofVerified = true

                //log.info("proof verified")

                if (newRootHash != null) {

                    //log.info("B. Send message RootHashCalculated to $sender, signature=${signatureRootHashOld?.asList().hashCode()}, rootHashOld=${rootHashOld.asList().hashCode()}")

                    replyToAuth()

                }
            }
            else -> {
                log.info("Dropped message")
            }

        }
    }

    private fun replyToAuth() {
        if (signatureRootHashOld == null) {

            //log.warning("RootHashOld in RootHashCalculator: ${rootHashOld.asList().hashCode()}, newRootHash=${newRootHash?.asList()?.hashCode()}")


            authenticator.tell(RootHashCalculated(newRootHash!!, null, calculateSignatureDependecy(rootHashOld, newRootHash!!)), self)
        } else {

            //log.warning("SignatureRootHashOld non è null, rootHashNew = ${newRootHash?.asList()?.hashCode()}")


            authenticator.tell(RootHashCalculated(newRootHash!!, sign(newRootHash!!), null), self)
        }
    }

    /**
     * Calcola il nuovo root hash
     */
    private fun calculateRootHash(proof: Proof<ByteArray, String, Any>, updates: List<Update>) : ByteArray {
        return proof.rootHash(updates.map { u -> Pair(u.key, u.value) }).second
    }

    private fun sign(what: ByteArray) : ByteArray {
        val s = Signature.getInstance("SHA256withRSA")
        s.initSign(KeyUtils.privateKey("${System.getProperty("cert.dir")}/private_key_pkcs8.pem"))
        s.update(what)
        return s.sign()
    }

    private fun calculateSignatureDependecy(rootHashOld: ByteArray, rootHashNew: ByteArray) : ByteArray {
        //log.warning("dependency --> rootHashOld = ${rootHashOld.asList().hashCode()}, rootHashNew = ${rootHashNew.asList().hashCode()}")

        return this.sign(rootHashOld + rootHashNew)
    }

}
