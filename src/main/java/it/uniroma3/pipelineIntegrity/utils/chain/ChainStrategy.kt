package it.uniroma3.pipelineIntegrity.utils.chain

abstract class ChainStrategy {

    var params: Array<ByteArray>? = null

    /**
     * Applica l'operazione alla chain
     *
     * @param chain a cui applicare l'operazione
     */
    abstract fun applyOperation(chain: Chain)

}