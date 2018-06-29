package it.uniroma3.pipelineIntegrity.utils.chain

abstract class ChainStrategy {

    var params: Array<ByteArray>? = null

    /**
     * Applies the operation to the chain
     *
     * @param chain to apply the operation to
     */
    abstract fun applyOperation(chain: Chain)

}