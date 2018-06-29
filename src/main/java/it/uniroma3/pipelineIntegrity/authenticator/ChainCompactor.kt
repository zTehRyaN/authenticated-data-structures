package it.uniroma3.pipelineIntegrity.authenticator

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedAbstractActor
import akka.event.Logging
import akka.event.LoggingAdapter
import it.uniroma3.pipelineIntegrity.authenticator.Messages.*
import it.uniroma3.pipelineIntegrity.utils.chain.Chain
import it.uniroma3.pipelineIntegrity.utils.KeyUtils
import it.uniroma3.pipelineIntegrity.utils.chain.SubChain
import java.security.Signature


class ChainCompactor(private val authenticator: ActorRef) : UntypedAbstractActor() {

    private var chainCompacted: Pair<ByteArray, ByteArray>? = null // compacted chain; null until it has been computed
    private var chainVerified: Boolean = false // chain verified; false if the verification has not yet completed or failed, true if it succeeded
    private lateinit var chain : SubChain

    companion object {
        fun props(authenticator: ActorRef): Props = Props.create(ChainCompactor::class.java, authenticator)
    }

    override fun preStart() {
        log.info("ChainCompactor started")
    }

    private val log: LoggingAdapter = Logging.getLogger(context.system, this) // used for logging

    override fun onReceive(message: Any?) {
        when (message) {
            is CompactChain -> {
                val (chain) = message
                this.chain = chain

                log.info("Received message CompactChain from $sender")

                if (chain.size() > 1)
                    chainCompacted = compactChain(chain)

                log.info("Chain compacted")

                if (chainVerified) {
                    reply()
                    chainCompacted = null
                    chainVerified = false
                }
            }
            is ChainVerified -> {
                chainVerified = true

                log.info("Chain verified")

                if (chainCompacted != null || chain.size() < 2) {
                    reply()
                    chainVerified = false
                    chainCompacted = null
                }
            }
        }
    }

    /**
     * Replies to the authenticator with the compacted chain
     */
    private fun reply() {
        if (chain.size() > 1) {

            log.info("chain = $chain, compacted = ${chainCompacted!!.first.asList().hashCode()}")

            authenticator.tell(ChainCompacted(chainCompacted!!.first, chainCompacted!!.second), self)
            log.info("Send message ChainCompacted to $sender")
        } else {
            authenticator.tell(ChainCompacted(null, null), self)
            log.info("Nothing to compact")
        }
    }

    /**
     * Takes a chain as argument, compacts it and signs it
     *
     * @return Pair<rootHashCompacted: ByteArray, signature_rootHashCompacted: ByteArray>
     */
    private fun compactChain(chain: SubChain): Pair<ByteArray, ByteArray> {
        val s = Signature.getInstance("SHA256withRSA")
        s.initSign(KeyUtils.privateKey("${System.getProperty("cert.dir")}/private_key_pkcs8.pem"))
        return chain.compact(s)
    }

}