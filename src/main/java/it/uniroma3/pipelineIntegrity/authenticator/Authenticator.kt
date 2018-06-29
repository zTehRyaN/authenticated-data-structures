package it.uniroma3.pipelineIntegrity.authenticator

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.UntypedAbstractActor
import akka.event.Logging
import akka.event.LoggingAdapter
import it.uniroma3.pipelineIntegrity.authenticator.Messages.*
import it.uniroma3.pipelineIntegrity.server.Messages.Authentic

/**
 * Authenticator actor
 *
 * Receives authentication requests from the server and dispatches the work
 * to four other actors:
 *
 *  - ChainCompactor -> compacts the chain
 *  - ChainVerifier -> verifies the chain
 *  - RootHashCalculator -> computes the root hash from the proof
 *  - ProofVerifier -> verifies the proof
 *
 */
class Authenticator : UntypedAbstractActor() {

    private lateinit var rootHashCalculator: ActorRef
    private lateinit var chainCompactor: ActorRef
    private lateinit var chainVerifier: ActorRef
    private lateinit var proofVerifier: ActorRef

    private lateinit var authenticationRequest: ActorRef

    private val log: LoggingAdapter = Logging.getLogger(context.system, this) // used for logging

    override fun preStart() {
        log.info("Authenticator started")

        rootHashCalculator = context.actorOf(RootHashCalculator.props(self), "RootHashCalculator")
        chainCompactor = context.actorOf(ChainCompactor.props(self), "ChainCompactor")

        chainVerifier = context.actorOf(ChainVerifier.props(chainCompactor), "ChainVerifier")
        proofVerifier = context.actorOf(ProofVerifier.props(rootHashCalculator), "ProofVerifier")
    }

    /**
     * actor state
     */
    var state = State.READY
        private set

    private var messages = 0 // number of reply messages received after dispatching the work to the 4 worker actors

    private var rootHashCompacted: ByteArray? = null
    private var signatureRHCompacted: ByteArray? = null

    private lateinit var newRootHash: ByteArray
    private var signatureDependency: ByteArray? = null
    private var signatureRootHash: ByteArray? = null

    private var id: Int = 0
    private var auth_number: Int = -1

    companion object {

        fun props(): Props = Props.create(Authenticator::class.java)

        enum class State {
            /**
             * The Authenticator actor can receive authentication requests
             */
            READY,
            /**
             * The Authenticator actor cannot receive authentication requests because it is busy with another one
             */
            BUSY
        }
    }

    override fun onReceive(message: Any?) {
        if (state == State.READY) {
            when (message) {
                is Authentic -> {

                    state = State.BUSY
                    val (chain, rootHashOld, signatureRootHashOld, proof, updates, oldValues, id, auth_number) = message

                    log.info("Received message ($chain, ${rootHashOld.asList().hashCode()}, $proof, $updates, $id) from $sender")

                    authenticationRequest = sender
                    this.id = id
                    this.auth_number = auth_number

                    messages = 0

                    rootHashCalculator.tell(CalculateRootHash(proof, rootHashOld, signatureRootHashOld, updates), self)
                    chainCompactor.tell(CompactChain(chain.clone()), self)

                    chainVerifier.tell(VerifyChain(chain.clone()), self)
                    proofVerifier.tell(VerifyProof(proof, rootHashOld, oldValues), self)

                }
            }
        } else if (state == State.BUSY) {

            when (message) {
                is RootHashCalculated -> {
                    val (newRootHash, signatureRootHash, signatureDependency) = message

                    this.newRootHash = newRootHash
                    this.signatureDependency = signatureDependency
                    this.signatureRootHash = signatureRootHash

                    messages++
                    log.info("RootHash calculated")

                    reply()
                }
                is ChainCompacted -> {
                    val (rootHashCompacted, signatureRHCompacted) = message

                    this.rootHashCompacted = rootHashCompacted
                    this.signatureRHCompacted = signatureRHCompacted

                    messages++
                    log.info("Chain compacted")

                    reply()

                }
                else -> {
                    log.info("Authenticator actor is busy")
                    self.tell(message, sender)
                }
            }

        }
    }

    private fun reply() {
        if (messages == 2) {
            authenticationRequest.tell(AuthenticationFinished(rootHashCompacted, signatureRHCompacted, newRootHash!!, signatureRootHash, signatureDependency, id, auth_number), self)
            state = State.READY
        }
    }

}