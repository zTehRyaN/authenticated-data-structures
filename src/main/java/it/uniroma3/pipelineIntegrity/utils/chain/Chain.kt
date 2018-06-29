package it.uniroma3.pipelineIntegrity.utils.chain

import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

class Chain() : Serializable {

    var rootHashes: ArrayList<ByteArray> = ArrayList()   //lista dei root hash della chain
    var signatures: ArrayList<ByteArray> = ArrayList()   //lista delle firme dei root hash della chain

    private val commits: SortedSet<Commit> = TreeSet<Commit>()
    private lateinit var currentCommit: Commit
    private var lastIdCommit: Int = -1

    /**
     * Crea un nuovo commit per la chain.
     *
     * @param id
     */
    fun initCommit(id: Int) {
        this.currentCommit = Commit(id, this)
    }

    /**
     * Aggiunge l'operazione operation al commit creato in precedenza.
     *
     * @see initCommit(Int)
     */
    fun addOperation(operation: String, vararg params: ByteArray) {
        this.currentCommit.addOperation(operation, arrayOf(*params))
    }

    /**
     * Chiude il commit corrente
     */
    fun endCommit() {
        this.commits.add(this.currentCommit)
    }

    /**
     * Applica i commit fin quando può applicarli
     */
    fun applyCommits(): Int {
        var cont = lastIdCommit
        val removed = mutableListOf<Commit>()

        for (commit in commits) {
            cont++
            if (cont == commit.n) {
                commit.apply()
                removed.add(commit)
                lastIdCommit = cont
            }
        }
        this.commits.removeAll(removed)
        return lastIdCommit
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder("[")
        for (i in rootHashes.indices) {
            stringBuilder.append("   {")
            stringBuilder.append(rootHashes[i].asList().hashCode())

            stringBuilder.append("; ")
            stringBuilder.append(signatures[i].asList().hashCode())

            stringBuilder.append("}   ")

            if (i < rootHashes.size - 1) {
                stringBuilder.append(", ")
            }
        }
        stringBuilder.append("]")
        return stringBuilder.toString()
    }

    fun size() = rootHashes.size

    fun subChain() : SubChain {
        return SubChain(rootHashes.clone() as ArrayList<ByteArray>, signatures.clone() as ArrayList<ByteArray>)
    }

}