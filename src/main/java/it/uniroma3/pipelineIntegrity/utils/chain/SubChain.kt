package it.uniroma3.pipelineIntegrity.utils.chain

import java.io.Serializable
import java.security.PublicKey
import java.security.Signature

class SubChain(private val rootHashes: ArrayList<ByteArray>, private val signatures: ArrayList<ByteArray>) : Serializable {

    /**
     * Compacts the chain (without verifying that the signatures are correct)
     *
     * @param s used to sign the compacted root hash
     *
     * @return pair of rootHash / signature
     */
    fun compact(s: Signature): Pair<ByteArray, ByteArray> {
        val rootHash = rootHashes.elementAt(rootHashes.size - 1)
        s.update(rootHash)
        val sign = s.sign()
        return Pair(rootHash, sign)
    }

    /**
     * Verifies the chain using the public key
     *
     * @param publicKey -> public key used for verification
     * @param algorithm -> algorithm used for the signature
     *
     * @return bool -> true if successful, false otherwise
     */
    fun verify(publicKey: PublicKey, algorithm: String): Boolean {
        if (rootHashes.size != signatures.size) return false

        // verify the first root hash
        val s = Signature.getInstance(algorithm)
        s.initVerify(publicKey)
        s.update(rootHashes[0])
        var res = s.verify(signatures[0])

        var i = 1

        while (i < signatures.size && res) {
            s.initVerify(publicKey)
            s.update(rootHashes[i - 1] + rootHashes[i])
            res = s.verify(signatures[i])
            i++
        }

        return res
    }

    fun size() = this.rootHashes.size

    fun clone() : SubChain {
        return SubChain(this.rootHashes.clone() as ArrayList<ByteArray>, this.signatures.clone() as ArrayList<ByteArray>)
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

}