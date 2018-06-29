package it.uniroma3.pipelineIntegrity.utils.chain

class AddRootHashCompactedStrategy : ChainStrategy() {

    override fun applyOperation(chain: Chain) {
        val rootHash = params!![0]
        val signature = params!![1]

        chain.rootHashes.clear()
        chain.signatures.clear()

        chain.rootHashes.add(0, rootHash)
        chain.signatures.add(0, signature)
    }

}