package it.uniroma3.pipelineIntegrity.utils.chain

class TruncateStrategy : ChainStrategy() {

    override fun applyOperation(chain: Chain) {
        val rootHash = params!![0]
        val signature = params!![1]

        val pos = chain.rootHashes.indexOfFirst { b -> b contentEquals rootHash }

        //println("Tronco a ${rootHash.asList().hashCode()}")

        if (pos != -1) {

            chain.rootHashes = ArrayList(chain.rootHashes.subList(pos, chain.rootHashes.size))

            chain.signatures = ArrayList(chain.signatures.subList(pos, chain.signatures.size))

            chain.signatures[0] = signature
        }

    }

}