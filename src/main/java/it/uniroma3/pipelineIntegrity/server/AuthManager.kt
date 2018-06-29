package it.uniroma3.pipelineIntegrity.server

import akka.actor.ActorSelection
import akka.actor.Props
import akka.actor.UntypedAbstractActor
import akka.event.Logging
import akka.event.LoggingAdapter
import it.uniroma3.ads.proof.Proof
import it.uniroma3.pipelineIntegrity.authenticator.Authenticator.Companion.State.BUSY
import it.uniroma3.pipelineIntegrity.authenticator.Authenticator.Companion.State.READY
import it.uniroma3.pipelineIntegrity.authenticator.Messages.AuthenticationFinished
import it.uniroma3.pipelineIntegrity.server.Messages.ManageAuthentication
import it.uniroma3.pipelineIntegrity.utils.chain.Chain
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class AuthManager(private val serverSide: ServerSide) : UntypedAbstractActor() {

    companion object {

        fun props(serverSide: ServerSide): Props = Props.create(AuthManager::class.java, serverSide)

    }

    private val authenticators = Array(serverSide.number_authenticator, { _ -> READY })

    private val log: LoggingAdapter = Logging.getLogger(context.system, this) //utilizzato per logging

    private var chain = Chain() //catena contenuta nell'attore

    private lateinit var auth: ActorSelection
    //private var auth_number: Int = -1

    override fun preStart() {
        //log.info("AuthManager started")
    }

    override fun postStop() {
        //log.info("AuthManager stopped")
    }

    override fun onReceive(message: Any?) {

        when (message) {

            is ManageAuthentication -> {

                val (rootHashOld, signatureRootHash, proof, updates, oldValues, id) = message
                authentic(rootHashOld, signatureRootHash, proof, updates, oldValues, id)

            }

            is AuthenticationFinished -> {
                val (rootHashCompacted, signatureRHCompacted, newRootHash, signatureNewRootHash, signatureDependency, id, auth_number) = message

                setReady(auth_number)
                serverSide.logManager.tell(Messages.Log("[$id] end_authentication", System.currentTimeMillis() - serverSide.systemStartup), self)

                //log.warning("Chain prima degli aggiornamenti ${chain.toString()}")

                this.chain.initCommit(id)

                if (signatureNewRootHash != null) {

                    //log.warning("NewRootHash firmato ---> rootHash: ${newRootHash.asList().hashCode()}")

                    this.chain.addOperation("addRootHashCompacted", newRootHash, signatureNewRootHash)

                } else {

                    //log.warning("NewRootHash non firmato ---> rootHash: ${newRootHash.asList().hashCode()}")


                    this.chain.addOperation("addDependency", newRootHash, signatureDependency!!)

                    if (rootHashCompacted != null) {
                        this.chain.addOperation("truncate", rootHashCompacted, signatureRHCompacted!!)
                    }


                }




                this.chain.endCommit()

                val n = this.chain.applyCommits()

                //log.info("id = $id, n = $n, chain = $chain")

                //log.warning("Chain dopo gli aggiornamenti ${chain.toString()}")

                if (n >= id) {
                    val adsManager = context.actorSelection("/user/ads-manager-$n")
                    adsManager.tell(Messages.ChainUpdated(chain.subChain()), self)
                }


            }

        }

    }

    private fun authentic(rootHashOld: ByteArray, signatureRootHash: ByteArray?, proof: Proof<ByteArray, String, Any>, updates: List<Messages.Update>, oldValues: List<Pair<String, Any?>>, id: Int) {
        //devo cercare uno slot authenticator disponibile
        log.warning(authenticators.toString())
        val auth_number = findFirstReadyAuthenticator()

        if (auth_number != -1) {
            setBusy(auth_number)

            serverSide.logManager.tell(Messages.Log("[$id] commit_trigger", System.currentTimeMillis() - serverSide.systemStartup), self)

            val authenticatorName = System.getProperty("system.authenticator.name")
            val auth_ip = serverSide.auth_ip
            val auth_port = serverSide.auth_port
            val authenticatorActorName = System.getProperty("system.authenticator.authenticator_actor.name")


            auth = context.actorSelection("akka.tcp://${System.getProperty("system.authenticator.name")}@${serverSide.auth_ip}:${serverSide.auth_port}/user/$authenticatorActorName$auth_number")
            auth.tell(Messages.Authentic(chain.subChain(), rootHashOld, signatureRootHash, proof, updates, oldValues, id, auth_number), self)

            //log.info("Send authentication request to ${auth.pathString()}")
        } else {
            // riprovo più tardi
            val system = context.system
            system.scheduler().scheduleOnce(Duration.create(serverSide.try_again_time, TimeUnit.MILLISECONDS),
                    Runnable { authentic(rootHashOld, signatureRootHash, proof, updates, oldValues, id) },
                    system.dispatcher())

            log.info("All authenticator actors are busies, I will try again in ${serverSide.try_again_time} ms")
        }

    }

    /**
     * Cerca il primo attore Authenticator disponibile e restituisce il numero identificativo, -1 se sono tutti in stato BUSY
     *
     * @return int
     */
    private fun findFirstReadyAuthenticator(): Int {
        for (index in authenticators.indices) {
            if (authenticators[index] == READY) return index
        }
        return -1
    }

    /**
     * Setta l'authenticator n BUSY
     */
    private fun setBusy(n: Int) {
        authenticators[n] = BUSY
    }

    /**
     * Setta l'authenticator n READY
     */
    private fun setReady(n: Int) {
        authenticators[n] = READY
    }

}