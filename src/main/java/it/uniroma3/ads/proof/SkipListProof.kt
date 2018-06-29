package it.uniroma3.ads.proof

import it.uniroma3.ads.auth.AuthSkipListNode
import it.uniroma3.ads.auth.HashUtils
import it.uniroma3.constants.Constants

class SkipListProof(node: AuthSkipListNode<String, ByteArray>, key: String): Proof<ByteArray, String, Any> {

    var proof: AuthSkipListNode<String, ByteArray> = node
        private set

    override var keys: MutableSet<String> = mutableSetOf()

    init {
        keys.add(key)
    }

    /**
     * Returns the rootHash of the Proof
     *
     * @return rootHash H
     *
     */
    override fun rootHash(): ByteArray {
        return HashUtils.sha256(proof.getFirstHash() as ByteArray, proof.getSecondHash() as ByteArray)
    }

    /**
     * If no value is provided, the operation is read-only. The rootHash is computed and returned.
     * If a value is provided, the operation is a write. The structure is updated and the new rootHash is returned.
     *
     * @param key
     * @param value
     *
     * @return rootHash H
     *
     */
    private fun rootHashImperative(key: String?, value: Any?): ByteArray {

        var start: AuthSkipListNode<String, ByteArray>

        if(value == null || key == null) {
            return HashUtils.sha256(proof.getFirstHash() as ByteArray, proof.getSecondHash() as ByteArray)
        } else {
            start = findNode(key, proof)
            start.setFirstHash(HashUtils.sha256(value.toString().toByteArray()))
        }

        // First node, on the BaseList
        if(start.next != null) {
            if(start.next!!.getSecondHash() != null){
                start.setSecondHash(HashUtils.sha256(start.next!!.getFirstHash() as ByteArray, start.next!!.getSecondHash() as ByteArray))
            } else {
                start.setSecondHash(start.next!!.getFirstHash())
            }
        }

        while(start.up != null || start.prev != null){

            val nextNode = start.next
            val underlying = start.down

            if(underlying is AuthSkipListNode<*, *>) {

                if (nextNode == null || nextNode.up != null) {        // there is a tower or null on the right
                    start.setFirstHash(underlying.getFirstHash() as ByteArray)
                    start.setSecondHash(underlying.getSecondHash() as ByteArray?)
                } else {                                                // there is a plateau on the right
                    if (underlying.getSecondHash() != null) {
                        start.setFirstHash(HashUtils.sha256(underlying.getFirstHash() as ByteArray, underlying.getSecondHash() as ByteArray))
                    } else {
                        start.setFirstHash(underlying.getFirstHash())
                    }
                    if(nextNode.getSecondHash() != null){
                        start.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                    } else {
                        start.setSecondHash(nextNode.getFirstHash())
                    }
                }


            } else if(nextNode != null){
                if(nextNode.getSecondHash() != null){
                    start.setSecondHash(HashUtils.sha256(nextNode.getFirstHash() as ByteArray, nextNode.getSecondHash() as ByteArray))
                } else {
                    start.setSecondHash(nextNode.getFirstHash())
                }
            }

            if(start.up == null) {
                start = start.prev!!                // had it been null, we would not be inside the while
            } else {
                start = start.up!!                  // had it been null, we would not be in the else
            }

        }

        // Now handle the HEAD node

        if(start.next != null) {
            if(start.next!!.getSecondHash() != null){
                start.setSecondHash(HashUtils.sha256(start.next!!.getFirstHash() as ByteArray, start.next!!.getSecondHash() as ByteArray))
            } else {
                start.setSecondHash(start.next!!.getFirstHash())
            }
        }

        val underlying = start.down
        if(underlying is AuthSkipListNode<*, *>) {
            if(underlying.getSecondHash() != null){
                start.setFirstHash(HashUtils.sha256(underlying.getFirstHash() as ByteArray, underlying.getSecondHash() as ByteArray))
            } else {
                start.setFirstHash(underlying.getFirstHash())
            }
        }

        if(start.getSecondHash() != null)
            return HashUtils.sha256(start.getFirstHash() as ByteArray, start.getSecondHash() as ByteArray)
        else
            return start.getFirstHash() as ByteArray
    }


    /**
     * Merges two proofs over the same structure at the same point in time. The result
     * does not interfere with the proof the method is invoked on
     *
     * @param proof, to be merged
     *
     * @return New Proof<H, K, V>, the union of the two proofs
     *
     */
    override fun union(proof: Proof<ByteArray, String, Any>): Proof<ByteArray, String, Any> {
        val newProof = this.clone()
        return (newProof as SkipListProof).unionImperative(proof)
    }


    /**
     * Merges two proofs over the same structure at the same point in time
     *
     * @param proof, to be merged
     *
     * @return proof<H, K, V>, the union of the two proofs
     *
     */
    private fun unionImperative(proof: Proof<ByteArray, String, Any>): Proof<ByteArray, String, Any> {

        for (key in proof.keys.minus(keys)){

            // add the key to the Set<K>
            this.keys.add(key)

            // safe cast to SkipListProof
            proof as SkipListProof

            // iterator over the proof to merge
            var iterator2 = proof.getHead()

            // iterator over this
            var iterator1 = this.proof

            // exit condition for the while
            var added = false

            while(!added){

                if(iterator2.next != null){

                    val copied = AuthSkipListNode(iterator2.next!!.getKey(), iterator2.next!!.getFirstHash(), iterator2.next!!.getSecondHash())

                    if(iterator1.next == null){
                        iterator1.next = copied
                        copied.prev = iterator1
                    }
                }

                if(iterator2.down.javaClass == iterator2.javaClass){

                    val down = iterator2.down as AuthSkipListNode<String, ByteArray>
                    val copied = AuthSkipListNode(down.getKey(), down.getFirstHash(), down.getSecondHash())

                    if(iterator1.down.javaClass != iterator1.javaClass){
                        iterator1.down = copied
                        copied.up = iterator1
                    }
                }

                if (iterator2.next != null){
                    if (lessThan(iterator2.next!!.getKey(), key) || equalTo(iterator2.next!!.getKey(), key)) {
                        iterator1 = iterator1.next!!
                        iterator2 = iterator2.next!!
                    } else if (iterator2.down.javaClass == iterator2.javaClass){
                        iterator1 = iterator1.down as AuthSkipListNode<String, ByteArray>
                        iterator2 = iterator2.down as AuthSkipListNode<String, ByteArray>
                    } else {
                        added = true
                    }
                } else {
                    if (iterator2.down.javaClass == iterator2.javaClass) {
                        iterator1 = iterator1.down as AuthSkipListNode<String, ByteArray>
                        iterator2 = iterator2.down as AuthSkipListNode<String, ByteArray>
                    } else {
                        added = true
                    }
                }
            }//end while
        }//end for

        return this
    }

    /**
     * If no values are provided, the operation is read-only. The rootHash is computed and returned.
     * If a non-null V is also present, the operation is a write. A new updated structure is returned together with its new rootHash.
     *
     * @param list
     *
     * @return Updated Proof<*, *, *> and the corresponding new rootHash
     *
     */
    override fun rootHash(list: List<Pair<String, Any?>>): Pair<Proof<ByteArray, String, Any>, ByteArray> {

        var result: ByteArray

        val newProof = this.clone() as SkipListProof

        result = if(newProof.proof.getSecondHash() != null) {
            HashUtils.sha256(newProof.proof.getFirstHash() as ByteArray, newProof.proof.getSecondHash() as ByteArray)
        }
        else
            newProof.proof.getFirstHash() as ByteArray


        for((key, value) in list.sortedByDescending { it.first }){
            if(value == null) continue
            result = newProof.rootHashImperative(key, value)
        }

        return Pair(newProof, result)
    }

    /**
     * Private function that searches for the matching node
     *
     */
    private fun findNode(key: String, start: AuthSkipListNode<String, ByteArray>? = null): AuthSkipListNode<String, ByteArray>{

        var found = false
        var end: AuthSkipListNode<String, ByteArray>

        end = if(start == null){
            this.getHead()
        } else {
            start
        }

        while(!found) {

            val nextNode = end.next
            if (nextNode != null) {     // can move right
                if(nextNode.getKey() <= key)
                    end = nextNode
                else if (end.down is AuthSkipListNode<*, *>) {
                    end = end.down as AuthSkipListNode<String, ByteArray>
                }
                else {
                    if(end.getKey() != key)
                        throw IllegalArgumentException("The given key does not exist!")
                    found = true
                }

            } else {
                if(end.down is AuthSkipListNode<*, *>){
                    end = end.down as AuthSkipListNode<String, ByteArray>
                }
                else {
                    if(!equalTo(end.getKey(), key))
                        throw IllegalArgumentException("The given key does not exist!")
                    found = true
                }
            }
        }

        return end
    }

    private fun clone(): Proof<ByteArray, String, Any> {

        val paths: MutableList<Proof<ByteArray, String, Any>> = mutableListOf()

        for(key in keys){
            var startingNode = findNode(key)
            var clonedPath = AuthSkipListNode(startingNode.getKey(), startingNode.getFirstHash(), startingNode.getSecondHash())

            if(startingNode.next != null){
                val tmp = AuthSkipListNode(startingNode.next!!.getKey(), startingNode.next!!.getFirstHash(), startingNode.next!!.getSecondHash())
                tmp.prev = clonedPath
                clonedPath.next = tmp
            }

            while(startingNode.up != null || startingNode.prev != null){
                if(startingNode.up != null){
                    val tmp = AuthSkipListNode(startingNode.up!!.getKey(), startingNode.up!!.getFirstHash(), startingNode.up!!.getSecondHash())
                    tmp.down = clonedPath
                    clonedPath.up = tmp

                    // Move up in parallel
                    clonedPath = clonedPath.up!!
                    startingNode = startingNode.up!!

                    // Copy the right plateau, if present
                    if(startingNode.next != null){
                        val tmp = AuthSkipListNode(startingNode.next!!.getKey(), startingNode.next!!.getFirstHash(), startingNode.next!!.getSecondHash())
                        tmp.prev = clonedPath
                        clonedPath.next = tmp
                    }
                } else {
                    val tmp = AuthSkipListNode(startingNode.prev!!.getKey(), startingNode.prev!!.getFirstHash(), startingNode.prev!!.getSecondHash())
                    tmp.next = clonedPath
                    clonedPath.prev = tmp

                    // Move left in parallel
                    clonedPath = clonedPath.prev!!
                    startingNode = startingNode.prev!!

                    // Copy the support node below, if present
                    if(startingNode.down is AuthSkipListNode<*, *>){
                        val supportHashing = startingNode.down as AuthSkipListNode<String ,ByteArray>
                        val tmp = AuthSkipListNode(supportHashing.getKey(), supportHashing.getFirstHash(), supportHashing.getSecondHash())
                        tmp.up = clonedPath
                        clonedPath.down = tmp
                    }
                }
            }

            val clonedProof = SkipListProof(clonedPath, key)
            paths.add(clonedProof)
        }

        var mergedProof = paths.first()

        for(path in paths){
            mergedProof = (mergedProof as SkipListProof).unionImperative(path)
        }

        return mergedProof
    }

    private fun getHead() = this.proof

    private fun lessThan(a: String, b: String): Boolean {
        return when(a) {
            Constants._keyForNegativeInfinity -> false
            else -> a < b
        }
    }

    override fun printProof() {

        // Loop to repeat for each path inside the proof
        for (key in keys){
            println("|------- Path for key = $key -------|")
            println()

            var directionMemorizer = "START"
            var stepCounter = 1
            var finished = false

            var node = findNode(key, proof)

            while(!finished) {

                System.out.printf("%.9s", "$stepCounter. $directionMemorizer    ")
                System.out.printf("%s", "[Key = " + node.getKey().substring(0, Math.max(4, node.getKey().length))+ ", FirstHash = ${HashUtils.toHex(node.getFirstHash())}, SecondHash = ${HashUtils.toHex(node.getSecondHash())}]")
                println()

                when (directionMemorizer) {

                    "UP" -> if(node.next != null) {
                        System.out.printf("%9s%s"," ","Right node --> [Key = ${node.next!!.getKey()}, FirstHash = ${HashUtils.toHex(node.next!!.getFirstHash())}, SecondHash = ${HashUtils.toHex(node.next!!.getSecondHash())}]")
                        println()
                    }

                    "LEFT" -> if(node.down is AuthSkipListNode<*, *>) {
                        System.out.printf("%9s%s","","Down node --> [Key = ${(node.down as AuthSkipListNode<*, *>).getKey()}, FirstHash = ${HashUtils.toHex((node.down as AuthSkipListNode<String, ByteArray>).getFirstHash())}, SecondHash = ${HashUtils.toHex((node.down as AuthSkipListNode<String, ByteArray>).getSecondHash())}]")
                        println()
                    }

                    "START" -> if(node.next != null) {
                        System.out.printf("%9s%s","","Right node --> [Key = ${node.next!!.getKey()}, FirstHash = ${HashUtils.toHex(node.next!!.getFirstHash())}, SecondHash = ${HashUtils.toHex(node.next!!.getSecondHash())}]")
                        println()
                    }

                }
                println()

                if(node.up != null){
                    node = node.up!!
                    directionMemorizer = "UP"
                    stepCounter++
                } else if(node.prev != null){
                    node = node.prev!!
                    directionMemorizer = "LEFT"
                    stepCounter++
                } else {
                    finished = true
                }
            }
            println()
        }
    }

    private fun equalTo(a: String, b: String) = a.compareTo(b) == 0
}