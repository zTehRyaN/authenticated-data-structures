package it.uniroma3.ads.skipList

import java.util.*

class SkipList<K: Comparable<K>, V>{

    private var start: SkipListNode<K, V>
    private var head: SkipListNode<K, V>
    private var maxLevel: Int
    private var size: Int

    private var _probability = 0.5

    private var stack = Stack<Pair<SkipListNode<K, V>, String>>()

    init {
        size = 0                                                // Number of distinct elements
        maxLevel = 0                                            // Height of the highest node (head)
        start = SkipListNode(Double.NEGATIVE_INFINITY as K, Double.NEGATIVE_INFINITY as V)     // Prevents garbage collection
        head = start
    }

    fun getHead(): SkipListNode<K, V> {
        return this.head
    }

    /**
     * Returns the node, starting from a given node and staying on the same level, with the highest value
     * for a key less than e
     *
     * @param e
     * @param current
     *
     * @return SkipListNode<K, V>
     */

    fun findNext(e: K, current: SkipListNode<K, V>): SkipListNode<K, V> {
        var next: SkipListNode<K, V>? = current.next  // pointer to the next node
        var localCurrent = current

        while(next != null && !lessThan(e,next.getKey())){
            val key: K = next.getKey()
            if(lessThan(e,key))
                break
            stack.add(Pair(localCurrent, "right"))
            localCurrent = next
            next = localCurrent.next
        }
        return localCurrent
    }

    /**
     * Extends the search performed by findNext to all nodes.
     * Returns the lowest node with key less than or equal to e.
     *
     * @see findNext
     *
     * @param e
     * @param current
     *
     * @return SkipListNode<K, V>
     *
     */

    fun find(e: K, current: SkipListNode<K, V>): SkipListNode<K, V> {

        var localCurrent = current

        localCurrent = findNext(e, localCurrent)

        while (localCurrent.down.javaClass.kotlin == localCurrent.javaClass.kotlin) {
            stack.add(Pair(localCurrent, "down"))
            localCurrent = localCurrent.down as SkipListNode<K, V>     // if the types differ, we will not enter the loop and will not raise cast exceptions
            localCurrent = findNext(e, localCurrent)
        }
        return localCurrent
    }

    /**
     * Generalizes the find search by always starting from the head node, placed at the maximum height
     *
     *
     * @see findNext
     * @see find
     *
     * @param e
     *
     * @return SkipListNode<K, V>
     *
     */

    fun find(e: K): SkipListNode<K, V> {
        return find(e, this.head)
    }

    /**
     * Searches the entire skiplist to check whether an element with the given key exists
     *
     * @see find
     * @see findNext
     *
     * @param k
     *
     * @return false if the element is not present, true otherwise
     *
     */

    fun containsKey(k: K): Boolean{
        val node: SkipListNode<K, V> = find(k)
        return node != null && node.getKey() != null && equalTo(node.getKey(), k)
    }

    /**
     * Given a key, searches the structure and returns the value associated with it
     *
     * @see find
     * @see findNext
     *
     * @param k
     *
     * @return V if the key is present, null otherwise
     *
     */

    fun getValue(k: K): V?{
        val node: SkipListNode<K, V> = find(k)
        if (node != null && node.getKey() != null && equalTo(node.getKey(), k))
            return node.getValue()
        return null
    }


    /**
     * Adds a node to the skipList based on the given key
     *
     * If the key is not present, a new node is created and inserted into the skipList
     * If the key is already present and the value is the same it does nothing, otherwise it updates it
     *
     * @see containsKey
     * @see getValue
     *
     * @param k
     * @param v
     *
     * @return true if the addition/update succeeded, false otherwise
     */

    fun add(k: K, v: V): Boolean{

        // Useful while testing
        stack.clear()

        if (containsKey(k) && getValue(k) == v) return false

        if (containsKey(k) && getValue(k) != v) { // need to update the value associated with the key
            var target: SkipListNode<K, V> = find(k)
            target.setValue(v)

            var up = target.up

            while (up != null){
                up.setValue(v)
                up = up.up
            }
            return true
        }

        // Empty the stack to discard the search paths for e
        stack.clear()

        // One element will be added
        size++

        // Run and count the Fair Coin Flips
        var level = 0
        while (Math.random() < _probability)
            level++
        // Create more sentinels, if needed
        while (level > maxLevel) {
            this.maxLevel++
            this.head.up = SkipListNode(Double.NEGATIVE_INFINITY as K, Double.NEGATIVE_INFINITY as V)
            // helper variable to wire up the double-linked list
            var tmp = this.head
            // tmp will point to the "old" node
            this.head = head.up!!
            // head therefore matches the maximum height of the skipList
            this.head.down = tmp
        }

        var previous: SkipListNode<K, V> = find(k)
        // At this point we need to insert the node with x between previous and previous.next
        // This operation is always performed, no matter what, because all elements are inserted into the BaseList

        // This is the node to insert into the BaseList; additional nodes (with the same value) may be stacked on top of it
        var newNode: SkipListNode<K, V> = SkipListNode(k, v)

        // Insertion into the BaseList + pointer adjustment
        newNode.next = previous.next
        previous.next?.prev = newNode
        previous.next = newNode
        newNode.prev = previous

        // While loop that, for each popped HEAD, walks back the path from the stack and positions itself on the previous node of the immediately upper level
        // The number of levels is assumed correct because it has been updated above

        var prevTower = newNode

        while (level > 0){

            var found: Boolean = false
            var currentPair: Pair<SkipListNode<K, V>, String>

            do {
                currentPair = stack.pop()
                if (currentPair.second=="down"){
                    found = true
                }
            } while (!found)

            // Perform the addition
            var block: SkipListNode<K, V> = SkipListNode(k, v)

            block.next = currentPair.first.next
            if (currentPair.first.next != null) {
                currentPair.first.next!!.prev = block
            }
            currentPair.first.next = block
            block.prev = currentPair.first              //if i want double links


            // Update prevTower.up and block.down
            block.down = prevTower
            prevTower.up = block                        //if i want double links

            // Update prevTower
            prevTower = block

            // Last step before exiting the while
            level--
        }

        stack.clear()
        return true
    }

    /**
     * Removes a node from the skipList based on the given key
     *
     * @see containsKey
     *
     * @param k
     *
     * @return true if the removal was performed, false otherwise
     */

    fun delete(k: K): Boolean{

        // Useful while testing
        stack.clear()

        if (!containsKey(k)) return false

        // Empty the stack to discard the search path of k
        stack.clear()

        // One element will be deleted
        size--


        var element: SkipListNode<K, V> = find(k)

        // Delete the node from the BaseList updating the various pointers
        // This operation is always performed, no matter what, because all elements are present in the BaseList

        // Removal from the BaseList + pointer adjustment
        element.prev?.next = element.next
        element.next?.prev = element.prev


        // While loop that walks back the path from the stack and positions itself on the node to delete at the immediately upper level

        var level = 0

        // here we remove the upper nodes

        while (level < maxLevel && element.up != null){

            var found: Boolean = false
            var currentPair: Pair<SkipListNode<K, V>, String>

            do {
                currentPair = stack.pop()
                if (currentPair.second=="down"){
                    found = true
                }
            } while (!found)

            if (!currentPair.first.getKey().equals(k))
                break

            // By clearing .down we drop the node below (Garbage Collected)
            currentPair.first.down = "Underlying value"

            currentPair.first.prev?.next = currentPair.first.next
            currentPair.first.next?.prev = currentPair.first.prev

            // Last step before exiting the while
            level++
        }

        // Keep the maxLevel of the skipList up to date
        if(level != 0 && level == maxLevel) maxLevel--

        stack.clear()
        return true
    }

    fun getSize(): Int{
        return this.size
    }

    fun getLevel(): Int{
        return this.maxLevel
    }

    fun getStack(): Stack<Pair<SkipListNode<K, V>, String>>{
        return this.stack
    }


    private fun lessThan(a: K, b: K): Boolean {
        return a.compareTo(b) < 0
    }

    private fun equalTo(a: K, b: K): Boolean {
        return a.compareTo(b) === 0
    }

    override fun toString(): String {
        var s = "skipList: "
        var iterator = start
        while (iterator.next != null) {
            s += iterator.next.toString() + ", "
            iterator = iterator.next!!
        }
        // Print the result making sure to drop the trailing ", "
        return s.substring(0, s.length - 2)
    }
}
