package it.uniroma3.pipelineIntegrity.utils.chain

class Commit(val n: Int, private val chain: Chain) : Comparable<Commit> {

    /**
     * Lista delle operazioni da effettuare nel commit
     */
    private val operations = mutableListOf<ChainStrategy>()

    /**
     * Aggiunge l'operazione al commit
     */
    fun addOperation(operation: String, params: Array<ByteArray>) {
        val className = "${operation.capitalize()}Strategy"
        val cs = Class.forName("it.uniroma3.pipelineIntegrity.utils.chain.$className").newInstance() as ChainStrategy
        cs.params = params
        operations.add(cs)
    }

    /**
     * Applica il commit alla chain
     */
    fun apply() {
        operations.forEach { it.applyOperation(chain) }
    }

    override fun compareTo(other: Commit) = this.n.compareTo(other.n)

}