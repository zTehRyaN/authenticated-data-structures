package it.uniroma3.ads.skipList

class SkipListNode<K: Comparable<K>, V>(key: K, value: V) {

    /**
        Garantire l'esistenza di nodi con value == null
        e di nodi che puntano a null
    */

    private var info: V         // E' permessa la creazione di nodo con value == null
    private var key: K
    //var nextNodes: MutableList<main.skipList.SkipListNode<E>?>?
    var next: SkipListNode<K, V>?
    var prev: SkipListNode<K, V>?
    var up: SkipListNode<K, V>?
    var down: Any

    init {
        this.key = key
        this.info = value
        //this.nextNodes = mutableListOf()
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