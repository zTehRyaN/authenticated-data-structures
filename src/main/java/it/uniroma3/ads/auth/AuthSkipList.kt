package it.uniroma3.ads.auth

import it.uniroma3.ads.proof.Proof
import it.uniroma3.ads.proof.SkipListProof
import it.uniroma3.constants.Constants
import java.util.*
import kotlin.collections.HashMap

class AuthSkipList: ADS<ByteArray, String, Any> {

    private var start: AuthSkipListNode<String, ByteArray>
    private var head: AuthSkipListNode<String, ByteArray>
    private var maxLevel: Int = 0   // Height of the tallest node (head)
    private var size: Int = 0       // Number of distinct elements

    private var deltas: MutableList<Triple<String, String, Any?>> = mutableListOf()

    private var stack = Stack<Pair<AuthSkipListNode<String, ByteArray>, String>>()

    init {
        start = AuthSkipListNode(Constants._keyForNegativeInfinity, HashUtils.sha256(Constants._valueForNegativeInfinity.toByteArray()))     // Prevents garbage collection
        head = start
    }

    /**
     * Starting from the given node and staying on the same level, returns the node with the highest key
     * still less than e
     *
     * @param e
     * @param current
     *
     * @return SkipListNode<K, V>
     */
    private fun findNext(e: String, current: AuthSkipListNode<String, ByteArray>): AuthSkipListNode<String, ByteArray> {
        var next: AuthSkipListNode<String, ByteArray>? = current.next
        var localCurrent = current

        while(next != null && (!lessThan(e, next.getKey()))){
            if(lessThan(e, next.getKey()))
                break
            stack.add(Pair(localCurrent, "right"))
            localCurrent = next
            next = localCurrent.next
        }
        return localCurrent
    }

    /**
     * Extends the search performed by findNext across all levels.
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
    private fun find(e: String, current: AuthSkipListNode<String, ByteArray>): AuthSkipListNode<String, ByteArray> {

        var localCurrent = current

        localCurrent = findNext(e, localCurrent)

        while (localCurrent.down.javaClass.kotlin == localCurrent.javaClass.kotlin) {
            stack.add(Pair(localCurrent, "down"))
            localCurrent = localCurrent.down as AuthSkipListNode<String, ByteArray>     // if the types differ, the loop is skipped and no cast exception is raised
            localCurrent = findNext(e, localCurrent)
        }
        return localCurrent
    }

    /**
     * Generalizes the find search by always starting from the head, at the maximum height
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
    private fun find(e: String) = find(e, this.head)

    /**
     * Searches the entire authenticated skip list to check whether a key is present
     *
     * @see find
     * @see findNext
     *
     * @param k
     *
     * @return false if the element is not present, true otherwise
     *
     */
    private fun containsKey(k: String): Boolean{
        val node: AuthSkipListNode<String, ByteArray> = find(k)
        return equalTo(node.getKey(), k)
    }

    /**
     * Looks up a key and returns the value associated with it
     *
     * @see find
     * @see findNext
     *
     * @param k
     *
     * @return V if the key is present, null otherwise
     *
     */
    override fun getValue(k: String): Any? {
        val node: AuthSkipListNode<String, ByteArray> = find(k)
        if (equalTo(node.getKey(), k))
            return node.getUnderlyingValue()
        return null
    }

    /**
     * Adds a node to the authenticated skip list based on the given key
     *
     * If the key is not present, a new node is created and inserted into the skip list.
     * If the key is already present and the value is the same, nothing happens; otherwise the value is updated.
     *
     * @see containsKey
     * @see getValue
     *
     * @param key
     * @param value
     *
     * @return updated rootHash
     */
    private fun applyAdd(key: String, value: Any?): ByteArray{

        // Testing
        stack.clear()

        if (getValue(key) == value){

            stack.clear()

            if (this.head.getSecondHash() != null){
                return HashUtils.sha256(this.head.getFirstHash() as ByteArray, this.head.getSecondHash() as ByteArray)
            } else {
                return this.head.getFirstHash() as ByteArray
            }
        }

        if (containsKey(key) && getValue(key) != value) {                    // need to update the value associated with the key and all hashes

            stack.clear()

            val target: AuthSkipListNode<String, ByteArray> = find(key)

            // Update the underlying value
            target.setUnderlyingValue(value as Any)

            // Update the hash for the new value
            target.setFirstHash(HashUtils.sha256(value.toString().toByteArray()))

            var stillInBaseList = true

            // current is initialized to HEAD because, if the while loop is not entered, there is only one element
            var current: Pair<AuthSkipListNode<String, ByteArray>, String> = Pair(getHead(), "right")

            while(!stack.isEmpty()){
                current = stack.pop()

                if(current.second == "down")    stillInBaseList = false

                if(stillInBaseList){            // Handle here nodes on the BaseList, which can be more than one
                    val nextNode: AuthSkipListNode<String, ByteArray>? = current.first.next
                    if(nextNode == null || nextNode.up != null) {
                        current.first.setSecondHash(null)
                    }
                    else{
                        if(nextNode.getSecondHash() != null){
                            current.first.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                        } else {
                            current.first.setSecondHash(nextNode.getFirstHash())
                        }
                    }
                }
                else{                           // Handle here all upper nodes, all in the same way
                    val belowNode = current.first.down as AuthSkipListNode<String, ByteArray>
                    if(current.first.next == null || current.first.next!!.up != null){        // there is a tower or null on the right
                        current.first.setFirstHash(belowNode.getFirstHash())
                        current.first.setSecondHash(belowNode.getSecondHash())
                    }
                    else {                               // there is a plateau on the right
                        val nextNode = current.first.next
                        if(belowNode.getSecondHash() != null){
                            current.first.setFirstHash(HashUtils.sha256(belowNode.getFirstHash() as ByteArray, belowNode.getSecondHash() as ByteArray))
                        } else {
                            current.first.setFirstHash(belowNode.getFirstHash())
                        }
                        if(nextNode!!.getSecondHash() != null){
                            current.first.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                        } else {
                            current.first.setSecondHash(nextNode.getFirstHash())
                        }
                    }
                }
            }

            if(current.first.getSecondHash() != null){
                return HashUtils.sha256(current.first.getFirstHash() as ByteArray, current.first.getSecondHash() as ByteArray)
            } else {
                return current.first.getFirstHash() as ByteArray
            }

        }

        // Clear the stack to drop the search path for e
        stack.clear()

        // One element will be added
        size++

        // Run and count the fair coin flips
        var level = 0
        while (Math.random() < Constants.probability)
            level++
        // Create additional sentinels if necessary
        while (level > maxLevel) {
            this.maxLevel++
            this.head.up = AuthSkipListNode(Constants._keyForNegativeInfinity)
            // for the moment the new sentinel nodes are copies of the starting node below
            this.head.up!!.setFirstHash(this.head.getFirstHash())
            this.head.up!!.setSecondHash(this.head.getSecondHash())
            // temporary variable for the double-linked list
            val tmp = this.head
            // tmp now points to the "old" node
            this.head = head.up!!
            // head therefore corresponds to the maximum height of the main.skipList.skipList
            this.head.down = tmp
        }


        val previous: AuthSkipListNode<String, ByteArray> = find(key)
        // At this point we insert the node with x between previous and previous.next.
        // This is always performed, because all elements live on the BaseList.

        // This is the node to insert into the BaseList, on top of which we may stack additional nodes (with the same value)
        val newNode: AuthSkipListNode<String, ByteArray> = AuthSkipListNode(key)

        // Insert into the BaseList and adjust pointers
        newNode.next = previous.next
        previous.next?.prev = newNode
        previous.next = newNode
        newNode.prev = previous

        newNode.setUnderlyingValue(value as Any)
        newNode.setFirstHash(HashUtils.sha256(value.toString().toByteArray()))

        // Handle the three cases for the second hash on the BaseList
        val nextNodeHash: AuthSkipListNode<String, ByteArray>? = newNode.next
        if(nextNodeHash == null) {
            newNode.setSecondHash(HashUtils.sha256(Constants._valueForPositiveInfinity.toByteArray()))
        }
        else if(nextNodeHash.up != null){
            newNode.setSecondHash(nextNodeHash.getFirstHash())
        }
        else{
            newNode.setSecondHash(HashUtils.sha256(nextNodeHash.getFirstHash() as ByteArray, nextNodeHash.getSecondHash() as ByteArray))
        }

        // Tamassia's condition removed for nodes on the BaseList; handle the two remaining cases

        val nextNode: AuthSkipListNode<String, ByteArray>? = newNode.next
        if(nextNode == null || nextNode.up != null){
            newNode.setSecondHash(null)
        }
        else {
            if(nextNode.getSecondHash() != null){
                newNode.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
            } else {
                newNode.setSecondHash(nextNode.getFirstHash())
            }
        }

        // While loop that, for every HEAD popped, derives the reverse path from the stack and lands on the previous node of the level immediately above.
        // The required number of levels is assumed correct because it was already updated above.

        val updateHashes = stack.clone() as Stack<Pair<AuthSkipListNode<String, ByteArray>, String>>
        var prevTower = newNode

        while (level > 0){

            var found = false
            var currentPair: Pair<AuthSkipListNode<String, ByteArray>, String>

            do {
                currentPair = stack.pop()
                if (currentPair.second == "down"){
                    found = true
                }
            } while (!found)

            // Perform the insertion
            val block: AuthSkipListNode<String, ByteArray> = AuthSkipListNode(key)

            block.next = currentPair.first.next
            if (currentPair.first.next != null) {
                currentPair.first.next!!.prev = block
            }
            currentPair.first.next = block
            block.prev = currentPair.first              //if i want double links


            // Update prevTower.up and block.down
            block.down = prevTower
            prevTower.up = block                        //if i want double links


            // Now that pointers are in place, update the hashes of the block node
            if(block.next == null || block.next!!.up != null){        // there is a tower or null on the right
                block.setFirstHash(prevTower.getFirstHash())
                block.setSecondHash(prevTower.getSecondHash())
            }
            else {                               // there is a plateau on the right
                val nextNode = block.next
                if(prevTower.getSecondHash() != null){
                    block.setFirstHash(HashUtils.sha256(prevTower.getFirstHash() as ByteArray, prevTower.getSecondHash() as ByteArray))
                } else {
                    block.setFirstHash(prevTower.getFirstHash())
                }
                if(nextNode!!.getSecondHash() != null){
                    block.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                } else {
                    block.setSecondHash(nextNode.getFirstHash())
                }
            }

            // Update prevTower
            prevTower = block

            // Last step before exiting the while
            level--
        }

        stack.clear()

        if(updateHashes.isEmpty()){
            if(head.next?.getSecondHash() != null){
                head.setSecondHash(HashUtils.sha256(head.next?.getFirstHash() as ByteArray, head.next?.getSecondHash() as ByteArray))
            } else {
                head.setSecondHash(head.next?.getFirstHash())
            }

            return HashUtils.sha256(head.getFirstHash() as ByteArray, head.getSecondHash() as ByteArray)
        }

        var element = updateHashes.pop()

        // Step one node forward depending on what was written by the first pop, and process the reached node
        // Here it is correct to consider only the next node, because firstHash is always present and refers to the underlying value
        if(element.second == "right"){
            val firstNode = element.first.next
            // Handle the three cases for the second hash on the BaseList
            val nextNode: AuthSkipListNode<String, ByteArray>? = firstNode!!.next
            if(nextNode == null || nextNode.up != null) {
                firstNode.setSecondHash(null)
            }
            else{
                if(nextNode.getSecondHash() != null){
                    firstNode.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                } else {
                    firstNode.setSecondHash(nextNode.getFirstHash())
                }
            }
        }
        else {
            val firstNode = element.first.down as AuthSkipListNode<String, ByteArray>
            // Handle the three cases for the second hash on the BaseList
            val nextNode = firstNode.next
            if(nextNode == null || nextNode.up != null) {
                firstNode.setSecondHash(null)
            }
            else{
                if(nextNode.getSecondHash() != null){
                    firstNode.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                } else {
                    firstNode.setSecondHash(nextNode.getFirstHash())
                }
            }
        }

        var stillInBaseList = true


        if (element.second == "down") stillInBaseList = false

        if (stillInBaseList) {            // Handle here the nodes on the BaseList, there can be more than one
            // Handle the three cases for the second hash on the BaseList
            // Here too it is correct to consider only the next node, because firstHash is always present and refers to the underlying value
            val nextNode: AuthSkipListNode<String, ByteArray>? = element.first.next
            if (nextNode == null || nextNode.up != null) {
                element.first.setSecondHash(null)
            } else {
                if (nextNode.getSecondHash() != null) {
                    element.first.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                } else {
                    element.first.setSecondHash(nextNode.getFirstHash())
                }
            }
        } else {                           // Handle here all the upper nodes, all in the same way
            prevTower = element.first.down as AuthSkipListNode<String, ByteArray>
            if (element.first.next == null || element.first.next!!.up != null) {        // there is a tower or null on the right
                element.first.setFirstHash(prevTower.getFirstHash())
                element.first.setSecondHash(prevTower.getSecondHash())
            } else {                               // there is a plateau on the right
                val nextNode = element.first.next

                if (prevTower.getSecondHash() != null) {
                    element.first.setFirstHash(HashUtils.sha256(prevTower.getFirstHash() as ByteArray, prevTower.getSecondHash() as ByteArray))
                } else {
                    element.first.setFirstHash(prevTower.getFirstHash())
                }
                if (nextNode!!.getSecondHash() == null) {
                    element.first.setSecondHash(nextNode.getFirstHash())
                } else {
                    element.first.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                }
            }
        }

        while(!updateHashes.isEmpty()){     // there are more elements to process

            element = updateHashes.pop()
            if (element.second == "down") stillInBaseList = false

            if (stillInBaseList) {            // Handle here the nodes on the BaseList, there can be more than one
                // Handle the three cases for the second hash on the BaseList
                val nextNode: AuthSkipListNode<String, ByteArray>? = element.first.next
                if (nextNode == null || nextNode.up != null) {
                    element.first.setSecondHash(null)
                } else {
                    if (nextNode.getSecondHash() != null) {
                        element.first.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                    } else {
                        element.first.setSecondHash(nextNode.getFirstHash())
                    }
                }
            } else {                           // Handle here all the upper nodes, all in the same way
                prevTower = element.first.down as AuthSkipListNode<String, ByteArray>
                if (element.first.next == null || element.first.next!!.up != null) {        // there is a tower or null on the right
                    element.first.setFirstHash(prevTower.getFirstHash())
                    element.first.setSecondHash(prevTower.getSecondHash())
                } else {                               // there is a plateau on the right
                    val nextNode = element.first.next
                    prevTower = element.first.down as AuthSkipListNode<String, ByteArray>

                    if (prevTower.getSecondHash() != null) {
                        element.first.setFirstHash(HashUtils.sha256(prevTower.getFirstHash() as ByteArray, prevTower.getSecondHash() as ByteArray))
                    } else {
                        element.first.setFirstHash(prevTower.getFirstHash())
                    }
                    if (nextNode!!.getSecondHash() == null) {
                        element.first.setSecondHash(nextNode.getFirstHash())
                    } else {
                        element.first.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                    }
                }
            }
        }

        return HashUtils.sha256(element.first.getFirstHash() as ByteArray, element.first.getSecondHash() as ByteArray)
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
    private fun applyDel(k: String): ByteArray {

        // Useful while testing
        stack.clear()

        if (!containsKey(k)) {
            if (getHead().getSecondHash() != null){
                return HashUtils.sha256(getHead().getFirstHash() as ByteArray, getHead().getSecondHash() as ByteArray)
            } else {
                return getHead().getFirstHash() as ByteArray
            }
        }

        // Empty the stack to discard the search path of k
        stack.clear()

        // One element will be deleted
        size--


        val element: AuthSkipListNode<String, ByteArray> = find(k)

        // Delete the node from the BaseList updating the various pointers
        // This operation is always performed, no matter what, because all elements are present in the BaseList

        // Removal from the BaseList + pointer adjustment
        element.prev?.next = element.next
        element.next?.prev = element.prev


        // While loop that walks back the path from the stack and positions itself on the node to delete at the immediately upper level

        var level = 0

        // here we remove the upper nodes

        while (level < maxLevel && element.up != null){

            var found = false
            var currentPair: Pair<AuthSkipListNode<String, ByteArray>, String>

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

        // At this point the only nodes whose hash needs updating are those on the new path recomputed to reach the just-deleted node

        find(k)

        var stillInBaseList = true
        var current: Pair<AuthSkipListNode<String, ByteArray>, String> = Pair(getHead(), "right")

        // Update the head node
        if(head.next != null) {
            if (head.next?.getSecondHash() != null) {
                current.first.setFirstHash(HashUtils.sha256(head.next?.getFirstHash() as ByteArray, head.next?.getSecondHash() as ByteArray))
            } else {
                current.first.setFirstHash(head.next?.getFirstHash())
            }
        } else head.setSecondHash(null)

        while(!stack.isEmpty()){

            current = stack.pop()

            if(current.second == "down")    stillInBaseList = false

            if(stillInBaseList){            // Handle here the nodes on the BaseList, there can be more than one
                val nextNode: AuthSkipListNode<String, ByteArray>? = current.first.next
                if(nextNode == null || nextNode.up != null) {
                    current.first.setSecondHash(null)
                }
                else{
                    if(nextNode.getSecondHash() != null){
                        current.first.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                    } else {
                        current.first.setSecondHash(nextNode.getFirstHash())
                    }
                }
            }
            else{                           // Handle here all the upper nodes, all in the same way
                val belowNode = current.first.down as AuthSkipListNode<String, ByteArray>
                if(current.first.next == null || current.first.next!!.up != null){        // there is a tower or null on the right
                    current.first.setFirstHash(belowNode.getFirstHash())
                    current.first.setSecondHash(belowNode.getSecondHash())
                }
                else {                               // there is a plateau on the right
                    val nextNode = current.first.next
                    if(belowNode.getSecondHash() != null){
                        current.first.setFirstHash(HashUtils.sha256(belowNode.getFirstHash() as ByteArray, belowNode.getSecondHash() as ByteArray))
                    } else {
                        current.first.setFirstHash(belowNode.getFirstHash())
                    }
                    if(nextNode!!.getSecondHash() != null){
                        current.first.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                    } else {
                        current.first.setSecondHash(nextNode.getFirstHash())
                    }
                }
            }
        }

        if(current.first.getSecondHash() != null){
            return HashUtils.sha256(current.first.getFirstHash() as ByteArray, current.first.getSecondHash() as ByteArray)
        } else {
            return current.first.getFirstHash() as ByteArray
        }
    }

    /**
     * Given a key, returns a pair containing the value associated with the key and
     * the proof for that key
     *
     * @see getValue
     * @see find
     *
     * @param k
     *
     * @return Pair<V, proof<H, K, V>> if the key is present, otherwise returns Pair<null, proof<H, K, V>>
     *         where the latter is the proof for the key immediately preceding the given one
     *
     */
    override fun getWithProof(k: String): Pair<Any?, Proof<ByteArray, String, Any>> {

        stack.clear()
        val baseNode = find(k)

        var proof = AuthSkipListNode(baseNode.getKey(), baseNode.getFirstHash(), baseNode.getSecondHash())

        // Check whether the right neighbor is a tower or a plateau and, if so, add it to the proof
        val nextOnBase = baseNode.next
        if(nextOnBase != null && nextOnBase.up == null){        // there is a plateau on the right
            val tmp = AuthSkipListNode(nextOnBase.getKey(), nextOnBase.getFirstHash(), nextOnBase.getSecondHash())
            proof.next = tmp
            tmp.prev = proof
        }

        while(!stack.isEmpty()){
            val nextNode = stack.pop()

            // Copy the just-popped node into tmp
            val tmp = AuthSkipListNode(nextNode.first.getKey(), nextNode.first.getFirstHash(), nextNode.first.getSecondHash())
            tmp.down = nextNode.first.down

            if(nextNode.second == "down"){
                proof.up = tmp
                tmp.down = proof
                val adjacent = nextNode.first.next
                if(adjacent != null && adjacent.up == null){
                    val plateau = AuthSkipListNode(adjacent.getKey(), adjacent.getFirstHash(), adjacent.getSecondHash())
                    tmp.next = plateau
                    plateau.prev = tmp
                }
            } else {
                proof.prev = tmp
                tmp.next = proof
                val candidate = tmp.down
                if(tmp.down is AuthSkipListNode<*, *>) {
                    val adjacent = candidate as AuthSkipListNode<String, ByteArray>
                    val underlying = AuthSkipListNode(adjacent.getKey(), adjacent.getFirstHash(), adjacent.getSecondHash())
                    tmp.down = underlying
                    underlying.up = tmp
                }
            }

            proof = tmp

        }

        stack.clear()

        return Pair(getValue(k), SkipListProof(proof, k))
    }

    /**
     * Given a list of keys, computes and returns a single proof covering all those key-value pairs
     *
     * @param keys
     *
     * @return proof<H, K, V>
     *
     */
    override fun getProof(keys: List<String>): Proof<ByteArray, String, Any> {

        if (!containsKey(keys[0])) throw IllegalArgumentException("Insert only keys that are present")
        var proof = this.getWithProof(keys[0]).second

        for (k in keys){
            if (!containsKey(k)) throw IllegalArgumentException("Insert only keys that are present")
            proof = proof.union(this.getWithProof(k).second)
        }

        return proof
    }

    /**
     *  Copies the structure and returns an identical one, except that deltas are NOT copied
     *
     *  @return ADS<H, K, V> representing the copy
     */
    private fun clone(): ADS<ByteArray, String, Any> {

        // Get the height of the skiplist
        val height = this.getMaxLevel()

        // Build the tower of dummies
        val clone = AuthSkipList()
        var head = clone.getHead()
        head.setFirstHash(this.start.getFirstHash())
        head.setSecondHash(this.start.getSecondHash())
        val start = head

        var verticalDummiesIterator = this.start
        while(verticalDummiesIterator.up != null){
            val next = AuthSkipListNode(verticalDummiesIterator.up!!.getKey(), verticalDummiesIterator.up!!.getFirstHash(), verticalDummiesIterator.up!!.getSecondHash())
            next.down = head
            head.up = next
            head = head.up!!
            verticalDummiesIterator = verticalDummiesIterator.up!!
        }

        // Empty list case
        if(this.size == 0) return clone

        // Create the array that will hold the SortedMaps for the skiplist
        val maps = Array<SortedMap<String, AuthSkipListNode<String, ByteArray>?>>(height + 1, { _ -> sortedMapOf()})

        var verticalIterator = this.start

        // Iterate over the BaseList
        var baseListIterator = verticalIterator.next
        maps[0] = sortedMapOf(Pair(baseListIterator!!.getKey(), baseListIterator))
        while(baseListIterator!!.next != null){
            maps[0][baseListIterator.next!!.getKey()] = baseListIterator.next
            baseListIterator = baseListIterator.next
        }

        if(height > 0) {

            verticalIterator = verticalIterator.up!!

            for (i in 1..height) {

                // Prepare the two parallel iterators
                var horizontalIterator = verticalIterator
                var baseLineIterator = this.start

                // Walk the two maps in parallel
                while(baseLineIterator.next != null){
                    val nextUpper = horizontalIterator.next
                    if(nextUpper == null) break
                    else {
                        while(baseLineIterator.next!!.getKey() < nextUpper.getKey()){
                            maps[i][baseLineIterator.next!!.getKey()] = null
                            baseLineIterator = baseLineIterator.next!!
                        }
                    }
                    maps[i][baseLineIterator.next!!.getKey()] = nextUpper

                    horizontalIterator = horizontalIterator.next!!
                    baseLineIterator = baseLineIterator.next!!
                }

                // Move the upper iterator forward
                if (verticalIterator.up != null) {
                    verticalIterator = verticalIterator.up!!
                }
            }
        }


        // Once the SortedMaps are built, instantiate the new nodes updating the values

        for (map in maps) {
            for((key, node) in map){
                if(node != null){
                    val clonedNode = AuthSkipListNode(node.getKey(), node.getFirstHash(), node.getSecondHash())
                    clonedNode.setUnderlyingValue(node.getUnderlyingValue())
                    map[key] = clonedNode
                }
            }
        }

        // Fix up the horizontal pointers
        var pointersVerticalIterator = start
        for(i in 0..height) {
            var pointersHorizontalIterator = pointersVerticalIterator
            for ((key, node) in maps[i]){
                if (node == null) continue
                pointersHorizontalIterator.next = node
                node.prev = pointersHorizontalIterator
                pointersHorizontalIterator = node
            }
            if(pointersVerticalIterator.up != null){
                pointersVerticalIterator = pointersVerticalIterator.up!!
            }
        }

        // Fix up the vertical pointers

        for(i in height downTo 1){
            for((key, node) in maps[i]){
                if(node == null) continue
                node.down = maps[i-1][key] as AuthSkipListNode<String, ByteArray>
                maps[i-1][key]!!.up = node
            }
        }


        while(head.up != null){
            head = head.up!!
        }
        clone.setHead(head)
        clone.size = this.getSize()
        clone.maxLevel = this.getMaxLevel()

        // Return the SkipList
        return clone
    }

    /**
     *  |--- Functional model design pattern ---|
     *
     * Collapses all deltas relative to the current structure and applies them in order
     *
     * @return A new ADS<H, K, V> with the deltas applied
     *
     */
    override fun applyDeltas(): ADS<ByteArray, String, Any> {

        val returnADS = this.clone() as AuthSkipList

        for (action in deltas.iterator()) {
            if(action.first == "add"){
                returnADS.applyAdd(action.second, action.third)
            }
            else{
                returnADS.applyDel(action.second)
            }
        }
        deltas.clear()
        return returnADS
    }

    override fun add(k: String, v: Any): ByteArray {
        deltas.add(Triple("add", k, v))
        if(getHead().getSecondHash() != null){
            return HashUtils.sha256(getHead().getFirstHash() as ByteArray, getHead().getSecondHash() as ByteArray)
        } else {
            return getHead().getFirstHash() as ByteArray
        }
    }

    override fun del(k: String): ByteArray {
        deltas.add(Triple("del", k, null))
        if(getHead().getSecondHash() != null){
            return HashUtils.sha256(getHead().getFirstHash() as ByteArray, getHead().getSecondHash() as ByteArray)
        } else {
            return getHead().getFirstHash() as ByteArray
        }
    }

    override fun rootHash(): ByteArray {
        if(getHead().getSecondHash() != null){
            return HashUtils.sha256(getHead().getFirstHash() as ByteArray, getHead().getSecondHash() as ByteArray)
        } else {
            return getHead().getFirstHash() as ByteArray
        }
    }

    private fun lessThan(a: String, b: String): Boolean {
        when(a){
            Constants._keyForNegativeInfinity -> return false
            else -> return a < b
        }
    }

    private fun equalTo(a: String, b: String) = a.compareTo(b) == 0

    private fun getHead() = this.head

    fun setHead(head: AuthSkipListNode<String, ByteArray>){
        this.head = head
    }

    fun getSize() = this.size

    private fun getMaxLevel() = this.maxLevel

    override fun toString(): String {
        var s = "SkipList: "
        var iterator = start
        while (iterator.next != null) {
            s += iterator.next.toString() + ", "
            iterator = iterator.next!!
        }
        // Print the result making sure to drop the trailing ", "
        return s.substring(0, s.length - 2)
    }

    override fun printADS(){
        val heights = HashMap<String, Int>()
        // The first String holds the real keys; the pair contains the alias key and the value
        val keys = HashMap<String, Pair<String, Any?>>()
        val hashes = storeHashes()

        var iterator = start
        heights[iterator.getKey()] = getMaxLevel()
        keys[iterator.getKey()] = Pair("K0", iterator.getUnderlyingValue())

        // Compute the heights of all the towers and record every key
        var keyCounter = 1
        while (iterator.next != null) {
            heights[iterator.next!!.getKey()] = computeColumnHeight(iterator.next!!)
            keys[iterator.next!!.getKey()] = Pair("K$keyCounter", iterator.next!!.getUnderlyingValue())
            iterator = iterator.next!!
            keyCounter++
        }

        var currentLevel = getMaxLevel()
        var verticalIterator: Any = getHead()
        var spacesLength = 14

        // Iterate horizontally for each level and print
        while (verticalIterator is AuthSkipListNode<*, *>) {
            var horizontalIterator = verticalIterator as AuthSkipListNode<String, ByteArray>?

            System.out.printf("%-10s", "Level: $currentLevel ")

            while (horizontalIterator != null) {
                val nodeHeight = heights[horizontalIterator.getKey()]

                if (nodeHeight!! >= currentLevel) {
                    val alias = keys[horizontalIterator.getKey()]!!.first+"."+currentLevel
                    val aliasLength = Math.max(5, alias.length)
                    var counter = 5
                    while (counter != aliasLength){
                        counter++
                        spacesLength++
                    }
                    System.out.printf("%s%.8s%s", "[--", " ${keys[horizontalIterator.getKey()]!!.first}.$currentLevel --", "--]")
                } else {
                    System.out.printf("%${spacesLength-1}s", "")
                }

                if(horizontalIterator.next != null) {
                    val emptySpacesCount = computeNumberOfSpaces(horizontalIterator, horizontalIterator.next!!)
                    for (i in 0..emptySpacesCount) {
                        System.out.printf("%${spacesLength}s", "")
                        keyCounter++
                    }
                }
                horizontalIterator = horizontalIterator.next
            }
            println()
            currentLevel--
            verticalIterator = verticalIterator.down
        }

        println()

        val sortedHashes = hashes.toSortedMap()
        for((node, hashes) in sortedHashes){
            System.out.printf("%-7s%-82s%-82s", node, " ---> FirstHash = " + HashUtils.toHex(hashes.first), ", SecondHash = " + HashUtils.toHex(hashes.second))
            println()
        }

        println()

        val sortedKeys = keys.toSortedMap()
        sortedKeys.remove("negativeInfinity")
        for((keys, couples) in sortedKeys){
            System.out.printf("%-4s%-22s%-22s", couples.first, " ---> Real Key = $keys",", Value = " + couples.second)
            println()
            //println(couples.first + " ---> Real Key = " + keys + ", Value = " + couples.second)
        }

        println()
    }

    private fun storeHashes(): HashMap<String, Pair<ByteArray?, ByteArray?>> {

        val hashMap = HashMap<String, Pair<ByteArray?, ByteArray?>>()

        var horizontalIterator: AuthSkipListNode<String, ByteArray>? = start
        var keysCounter = 0
        var levelCounter: Int

        while(horizontalIterator != null){
            levelCounter = 0
            hashMap["K$keysCounter.$levelCounter"] = Pair(horizontalIterator.getFirstHash(), horizontalIterator.getSecondHash())
            var verticalIterator = horizontalIterator
            while(verticalIterator!!.up != null) {
                levelCounter++
                hashMap["K$keysCounter.$levelCounter"] = Pair(verticalIterator.up!!.getFirstHash(), verticalIterator.up!!.getSecondHash())
                verticalIterator = verticalIterator.up
            }
            keysCounter++
            horizontalIterator = horizontalIterator.next
        }

        return hashMap
    }

    private fun computeColumnHeight(node: AuthSkipListNode<String, ByteArray>): Int{
        var height = 0
        var localNode = node
        while(localNode.up != null) {
            height++
            localNode = localNode.up!!
        }
        return height
    }

    private fun computeNumberOfSpaces(node: AuthSkipListNode<String, ByteArray>, next: AuthSkipListNode<String, ByteArray>): Int {

        var localBase = node
        var localNext = next
        var steps = 0

        while (localBase.down is AuthSkipListNode<*, *>){
            localBase = localBase.down as AuthSkipListNode<String, ByteArray>
        }

        while (localNext.down is AuthSkipListNode<*, *>){
            localNext = localNext.down as AuthSkipListNode<String, ByteArray>
        }

        while (localBase.next!!.getKey() != localNext.getKey()) {
            steps++
            localBase = localBase.next!!
        }

        return steps - 1
    }

    /**
     * Reports whether the SkipList is empty (contains no elements) or not
     *
     * return true if empty, false otherwise
     *
     */
    override fun isEmpty(): Boolean {
        if(start.next == null)
            return true
        return false
    }

}
