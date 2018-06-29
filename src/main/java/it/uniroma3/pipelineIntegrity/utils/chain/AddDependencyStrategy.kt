package it.uniroma3.pipelineIntegrity.utils.chain

class AddDependencyStrategy : ChainStrategy() {

    override fun applyOperation(chain: Chain) {
        val rootHash = params!![0]
        val signature = params!![1]

        //println("Aggiungo dipendenza ${rootHash.asList().hashCode()}")

        chain.rootHashes.add(rootHash)
        chain.signatures.add(signature)
    }

}