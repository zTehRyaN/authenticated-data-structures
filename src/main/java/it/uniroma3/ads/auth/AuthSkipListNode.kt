package it.uniroma3.ads.auth

import java.io.Serializable

class AuthSkipListNode<K: Comparable<K>, H>(key: K, firstHash: H? = null, secondHash: H? = null) : Serializable {

    /*
     *    Garantire l'esistenza di nodi con fistHash == null e secondHash == null
     *    e di nodi che puntano a null
     */

    private var firstHash: H?
    private var secondHash: H?
    private var key: K
    var next: AuthSkipListNode<K, H>?
    var prev: AuthSkipListNode<K, H>?
    var up: AuthSkipListNode<K, H>?
    var down: Any

    init {
        this.key = key
        this.firstHash = firstHash
        this.secondHash = secondHash
        this.next = null
        this.prev = null
        this.up = null
        this.down = "Underlying value"
    }

    fun getFirstHash() = firstHash

    fun setFirstHash(firstHash: H?){
        this.firstHash = firstHash
    }

    fun getSecondHash() = secondHash

    fun setSecondHash(secondHash: H?){
        this.secondHash = secondHash
    }

    fun getUnderlyingValue() = this.down

    fun setUnderlyingValue(uValue: Any){
        this.down = uValue
    }

    fun getKey() = this.key

    fun setKey(key: K){
        this.key = key
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other?.javaClass != javaClass) return false

        other as AuthSkipListNode<K, H>

        if(     this.next == other.next &&
                this.prev == other.prev &&
                this.down == other.down &&
                this.up == other.up &&
                this.firstHash == other.firstHash &&
                this.secondHash == other.secondHash &&
                this.key == other.key)
            return true
        return false
    }

    override fun hashCode() = 0

    override fun toString() = "Node: [K = $key, First Hash = " + toHex(firstHash)+ ", Second Hash = " + toHex(secondHash)+"]"

    /**
     * Metodo per consentire corretta visualizzazione degli hash in formato byteArray
     * Utile in fase di testing.
     *
     * Per tornare a rendere parametrico AuthSkipListNode basta eliminare metodo e riportare il toString a condizioni originarie
     *
     */
    fun toHex(array: H?): String {

        if (array == null) return "null"

        array as ByteArray
        val result = StringBuffer()

        for (b in array) {
            val st = String.format("%02X", b)
            result.append(st)
        }

        return result.toString()
    }
}