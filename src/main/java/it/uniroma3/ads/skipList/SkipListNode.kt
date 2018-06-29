package it.uniroma3.ads.skipList

class SkipListNode<K: Comparable<K>, V>(key: K, value: V) {

    /**
        Allow nodes with value == null
        and nodes that point to null
    */

    private var info: V         // Creating a node with value == null is allowed
    private var key: K
    var next: SkipListNode<K, V>?
    var prev: SkipListNode<K, V>?
    var up: SkipListNode<K, V>?
    var down: Any

    init {
        this.key = key
        this.info = value
        this.next = null
        this.prev = null
        this.up = null
        this.down = "Underlying value"
    }

    fun getValue(): V {
        return info
    }

    fun getUnderlyingValue(): Any{
        return this.down
    }

    fun setUnderlyingValue(uValue: Any){
        this.down = uValue
    }

    fun setValue(value: V){
        this.info = value
    }

    fun getKey(): K {
        return key
    }

    fun setKey(key: K){
        this.key = key
    }

    override fun toString(): String{
        return "Node: [K = " + key + ", V = " + info + "]"
    }
}