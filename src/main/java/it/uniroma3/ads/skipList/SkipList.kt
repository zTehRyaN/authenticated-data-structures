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
        size = 0                                                // Numero di elementi diversi tra loro
        maxLevel = 0                                            // Altezza del nodo più alto (head)
        //head = main.skipList.SkipListNode<E>(null)
        start = SkipListNode(Double.NEGATIVE_INFINITY as K, Double.NEGATIVE_INFINITY as V)     // Previene da garbage collecting
        //head.nextNodes?.add(null)
        head = start
    }

    fun getHead(): SkipListNode<K, V> {
        return this.head
    }

    /**
     * Ritorna il nodo, partendo da un nodo dato e rimanendo sullo stesso livello, con il più alto valore
     * per la chiave minore di e
     *
     * @param e
     * @param current
     *
     * @return SkipListNode<K, V>
     */

    fun findNext(e: K, current: SkipListNode<K, V>): SkipListNode<K, V> {
        var next: SkipListNode<K, V>? = current.next  //puntatore al successivo nodo
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

//    private fun findNext(e: E, current: main.skipList.SkipListNode<E>, level: Int): main.skipList.SkipListNode<E>{
//        var next: main.skipList.SkipListNode<E>? = current.nextNodes!![level]  //puntatore al successivo nodo
//        var localCurrent = current
//
//        while(next != null){
//            var value: E = next.getValue() as E
//            if(lessThan(e,value))
//                break
//            localCurrent = next
//            next = current.nextNodes!![level]
//        }
//        return localCurrent
//
//    }

    /**
     * Estende la ricerca effettuata da findNext a tutti i nodi
     * Ritorna il più basso nodo con chiave minore o uguale ad e
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
            localCurrent = localCurrent.down as SkipListNode<K, V>     // se sono di tipo diverso, non entrerò nel ciclo e non genero eccezioni sul cast
            localCurrent = findNext(e, localCurrent)
        }
        return localCurrent
    }

//    private fun find(e: E, current: main.skipList.SkipListNode<E>, level: Int): main.skipList.SkipListNode<E>{
//        var localCurrent = current
//        var localLevel = level
//        do {
//            localCurrent = findNext(e, localCurrent, level)
//        } while (localLevel-- > 0)
//        return localCurrent
//    }

    /**
     * Generalizza la ricerca di find partendo sempre dal nodo di partenza, posto all'altezza massima
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

//    private fun find(e: E): main.skipList.SkipListNode<E>{
//        return find(e, this.head, this.maxLevel)
//    }

    /**
     * Effettua una ricerca su tutta la skiplist per vedere se c'è o no un elemento in base alla chiave
     *
     * @see find
     * @see findNext
     *
     * @param k
     *
     * @return false se elemento non presente, true altrimenti
     *
     */

    fun containsKey(k: K): Boolean{
        val node: SkipListNode<K, V> = find(k)
        return node != null && node.getKey() != null && equalTo(node.getKey(), k)
    }

    /**
     * Data una chiave effettua una ricerca e restituisce il valore associato ad essa
     *
     * @see find
     * @see findNext
     *
     * @param k
     *
     * @return V se la chiave è presente, null altrimenti
     *
     */

    fun getValue(k: K): V?{
        val node: SkipListNode<K, V> = find(k)
        if (node != null && node.getKey() != null && equalTo(node.getKey(), k))
            return node.getValue()
        return null
    }


    /**
     * Aggiunge un nodo alla skipList sulla base della chiave passata
     *
     * Se la chiave non è presente, viene creato il nuovo nodo da inserire nella skipList
     * Se la chiave è gia presente, e il valore è uguale non fa nulla, altrimenti lo aggiorna
     *
     * @see containsKey
     * @see getValue
     *
     * @param k
     * @param v
     *
     * @return true se l'aggiunta/aggiornamento è riuscito, false altrimenti
     */

    fun add(k: K, v: V): Boolean{

        // Utile in fase di testing
        stack.clear()

        if (containsKey(k) && getValue(k) == v) return false

        if (containsKey(k) && getValue(k) != v) { // devo aggiornare il value associato alla chiave
            var target: SkipListNode<K, V> = find(k)
            target.setValue(v)

            var up = target.up

            while (up != null){
                up.setValue(v)
                up = up.up
            }
            return true
        }

        // Svuoto lo stack per eliminare i path di ricerca di e
        stack.clear()

        // Un elemento sarà aggiunto
        size++

        // Eseguo e conto i Fair Coin Flip
        var level = 0
        while (Math.random() < _probability)
            level++
        // Creo altre sentinelle, se necessario
        while (level > maxLevel) {
            this.maxLevel++
            this.head.up = SkipListNode(Double.NEGATIVE_INFINITY as K, Double.NEGATIVE_INFINITY as V)
            // variabile di appoggio per collegamento per double-linked list
            var tmp = this.head
            // tmp punterà il nodo "vecchio"
            this.head = head.up!!
            // head diventa dunque corrispondende con l'altezza massima della main.skipList.skipList
            this.head.down = tmp
        }

        var previous: SkipListNode<K, V> = find(k)
        // A questo punto devo inserire il nodo con x tra previous e previous.next
        // Questa operazione viene eseguita sempre, a prescindere, perchè nella BaseList sono inseriti tutti gli elementi

        // Questo è il nodo da inserire nella BaseList sopra il quale eventualmente inserire altri nodi (col suo stesso valore)
        var newNode: SkipListNode<K, V> = SkipListNode(k, v)

        // Inserimento nella BaseList + adjust puntatori
        newNode.next = previous.next
        previous.next?.prev = newNode
        previous.next = newNode
        newNode.prev = previous

        // Ciclo while che, per ogni HEAD estratta, ricava dallo stack il path inverso e si posiziona sul nodo previous del livello immediatamente superiore
        // Il numero di livelli necessario si presuppone corretto perchè aggiornato sopra

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

            // Effettuo l'aggiunta
            var block: SkipListNode<K, V> = SkipListNode(k, v)

            block.next = currentPair.first.next
            if (currentPair.first.next != null) {
                currentPair.first.next!!.prev = block
            }
            currentPair.first.next = block
            block.prev = currentPair.first              //if i want double links


            // Aggiorno prevTower.up e block.down
            block.down = prevTower
            prevTower.up = block                        //if i want double links

            // Aggiorno prevTower
            prevTower = block

            // Ultimo passo per uscire dal while
            level--
        }

        stack.clear()
        return true
    }

//    fun add(e: E): Boolean{
//
//        if (containsKey(e)) return false
//
//        getSize++
//
//        var level: Int = 0
//        while (Math.random() < _probability)
//            level++
//        while (level > maxLevel) {
//            this.head.nextNodes?.add(null)
//            this.maxLevel++
//        }
//
//        var newNode: main.skipList.SkipListNode<E> = main.skipList.SkipListNode<E>(e)
//        var current = head
//
//        do {
//            current = findNext(e, current, level)
//            newNode.nextNodes?.add(0, current.nextNodes?.get(level))
//            current.nextNodes?.set(level, newNode)
//        } while (level-- > 0)
//
//        return true
//    }

    /**
     * Rimuove un nodo alla skipList sulla base della chiave passata
     *
     * @see containsKey
     *
     * @param k
     *
     * @return true se la rimozione è avvenuta, false altrimenti
     */

    fun delete(k: K): Boolean{

        // Utile in fase di testing
        stack.clear()

        if (!containsKey(k)) return false

        // Svuoto lo stack per eliminare il path di ricerca di k
        stack.clear()

        // Un elemento sarà cancellato
        size--


        var element: SkipListNode<K, V> = find(k)

        // Devo eliminare il nodo dalla BaseList aggiornando i vari puntatori
        // Questa operazione viene eseguita sempre, a prescindere, perchè nella BaseList sono presenti tutti gli elementi

        // Rimozione dalla BaseList + adjust puntatori
        element.prev?.next = element.next
        element.next?.prev = element.prev


        // Ciclo while che ricava dallo stack il path inverso k si posiziona sul nodo da eliminare del livello immediatamente superiore

        var level = 0

        // qui elimino i nodi superiori

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

            // Eliminando il .down elimino il nodo sottostante (Garbage Collected)
            currentPair.first.down = "Underlying value"

            currentPair.first.prev?.next = currentPair.first.next
            currentPair.first.next?.prev = currentPair.first.prev

            // Ultimo passo per uscire dal while
            level++
        }

        // If per tenere aggiornato il maxLevel della main.skipList.skipList
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
        // Stampo il risultato preoccupandomi di togliere l'ultimo ", "
        return s.substring(0, s.length - 2)
    }
}
