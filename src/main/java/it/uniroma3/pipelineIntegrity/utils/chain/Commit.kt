package it.uniroma3.pipelineIntegrity.utils.chain

class Commit(val n: Int, private val chain: Chain) : Comparable<Commit> {

    /**
     * List of operations to apply in this commit
     */
    private val operations = mutableListOf<ChainStrategy>()

    /**
     * Adds an operation to this commit
     */
    fun addOperation(operation: String, params: Array<ByteArray>) {
        val className = "${operation.capitalize()}Strategy"
        val cs = Class.forName("it.uniroma3.pipelineIntegrity.utils.chain.$className").newInstance() as ChainStrategy
        cs.params = params
        operations.add(cs)
    }

    /**
     * Applies the commit to the chain
     */
    fun apply() {
        operations.forEach { it.applyOperation(chain) }
    }

    override fun compareTo(other: Commit) = this.n.compareTo(other.n)

}