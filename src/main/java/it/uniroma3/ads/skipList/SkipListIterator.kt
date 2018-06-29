package it.uniroma3.ads.skipList

//class SkipListIterator<E : Comparable<E>>: Iterator<E> {
//
//    internal var list: main.skipList.skipList<E>
//    internal var current: main.skipList.SkipListNode<E>
//
//    constructor(list: main.skipList.skipList<E>) {
//        this.list = list
//        this.current = list.getHead()
//    }
//
//    override fun hasNext(): Boolean {
//        return current.nextNodes?.get(0) != null ?: false
//    }
//
//    override fun next(): E {
//        this.current = this.current.nextNodes!!.get(0)!!
//        return current.getValue()!!
//    }
//}