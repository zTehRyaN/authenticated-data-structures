package it.uniroma3.ads.auth

import it.uniroma3.ads.proof.Proof
import it.uniroma3.ads.proof.SkipListProof
import it.uniroma3.constants.Constants
import java.util.*
import kotlin.collections.HashMap

class AuthSkipList: ADS<ByteArray, String, Any> {

    private var start: AuthSkipListNode<String, ByteArray>
    private var head: AuthSkipListNode<String, ByteArray>
    private var maxLevel: Int = 0   // Altezza del nodo più alto (head)
    private var size: Int = 0       // Numero di elementi diversi tra loro

    private var deltas: MutableList<Triple<String, String, Any?>> = mutableListOf()

    private var stack = Stack<Pair<AuthSkipListNode<String, ByteArray>, String>>()

    init {
        start = AuthSkipListNode(Constants._keyForNegativeInfinity, HashUtils.sha256(Constants._valueForNegativeInfinity.toByteArray()))     // Previene da garbage collecting
        head = start
    }

    /**
     * Restituisce il nodo, partendo da un nodo dato e rimanendo sullo stesso livello, con il più alto valore
     * per la chiave minore di e
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
    private fun find(e: String, current: AuthSkipListNode<String, ByteArray>): AuthSkipListNode<String, ByteArray> {

        var localCurrent = current

        localCurrent = findNext(e, localCurrent)

        while (localCurrent.down.javaClass.kotlin == localCurrent.javaClass.kotlin) {
            stack.add(Pair(localCurrent, "down"))
            localCurrent = localCurrent.down as AuthSkipListNode<String, ByteArray>     // se sono di tipo diverso, non entrerò nel ciclo e non genero eccezioni sul cast
            localCurrent = findNext(e, localCurrent)
        }
        return localCurrent
    }

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
    private fun find(e: String) = find(e, this.head)

    /**
     * Effettua una ricerca su tutta la skiplist autenticata per vedere se c'è o no un elemento in base alla chiave
     *
     * @see find
     * @see findNext
     *
     * @param k
     *
     * @return false se elemento non presente, true altrimenti
     *
     */
    private fun containsKey(k: String): Boolean{
        val node: AuthSkipListNode<String, ByteArray> = find(k)
        return equalTo(node.getKey(), k)
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
    override fun getValue(k: String): Any? {
        val node: AuthSkipListNode<String, ByteArray> = find(k)
        if (equalTo(node.getKey(), k))
            return node.getUnderlyingValue()
        return null
    }

    /**
     * Aggiunge un nodo alla skipList autenticata sulla base della chiave passata
     *
     * Se la chiave non è presente, viene creato il nuovo nodo da inserire nella skipList
     * Se la chiave è gia presente, e il valore è uguale non fa nulla, altrimenti lo aggiorna
     *
     * @see containsKey
     * @see getValue
     *
     * @param key
     * @param value
     *
     * @return rootHash aggiornato
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

        if (containsKey(key) && getValue(key) != value) {                    // devo aggiornare il value associato alla chiave, e tutti gli hash

            stack.clear()

            val target: AuthSkipListNode<String, ByteArray> = find(key)

            // Aggiorno il valore sottostante
            target.setUnderlyingValue(value as Any)

            // Aggiorno l'hash relativo al nuovo valore
            target.setFirstHash(HashUtils.sha256(value.toString().toByteArray()))

            var stillInBaseList = true

            // current inizializzato come nodo HEAD perchè se non entro nel while vuoldire che ho solo un elemento
            var current: Pair<AuthSkipListNode<String, ByteArray>, String> = Pair(getHead(), "right")

            while(!stack.isEmpty()){
                current = stack.pop()

                if(current.second == "down")    stillInBaseList = false

                if(stillInBaseList){            // Tratto qui i nodi sulla BaseList, che possono essere più d'uno
                    val successivo: AuthSkipListNode<String, ByteArray>? = current.first.next
                    if(successivo == null || successivo.up != null) {
                        current.first.setSecondHash(null)
                    }
                    else{
                        if(successivo.getSecondHash() != null){
                            current.first.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
                        } else {
                            current.first.setSecondHash(successivo.getFirstHash())
                        }
                    }
                }
                else{                           // Tratto qui tutti i nodi superiori, tutti allo stesso modo
                    val sottostante = current.first.down as AuthSkipListNode<String, ByteArray>
                    if(current.first.next == null || current.first.next!!.up != null){        //Allora a destra ho un tower oppure null
                        current.first.setFirstHash(sottostante.getFirstHash())
                        current.first.setSecondHash(sottostante.getSecondHash())
                    }
                    else {                               //Allora a destra ho un plateau
                        val successivo = current.first.next
                        if(sottostante.getSecondHash() != null){
                            current.first.setFirstHash(HashUtils.sha256(sottostante.getFirstHash() as ByteArray, sottostante.getSecondHash() as ByteArray))
                        } else {
                            current.first.setFirstHash(sottostante.getFirstHash())
                        }
                        if(successivo!!.getSecondHash() != null){
                            current.first.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
                        } else {
                            current.first.setSecondHash(successivo.getFirstHash())
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

        // Svuoto lo stack per eliminare i path di ricerca di e
        stack.clear()

        // Un elemento sarà aggiunto
        size++

        // Eseguo e conto i Fair Coin Flip
        var level = 0
        while (Math.random() < Constants.probability)
            level++
        // Creo altre sentinelle, se necessario
        while (level > maxLevel) {
            this.maxLevel++
            this.head.up = AuthSkipListNode(Constants._keyForNegativeInfinity)
            // momentaneamente i nuovi nodi sentinella sono copie del sottostante di partenza
            this.head.up!!.setFirstHash(this.head.getFirstHash())
            this.head.up!!.setSecondHash(this.head.getSecondHash())
            // variabile di appoggio per collegamento per double-linked list
            val tmp = this.head
            // tmp punterà il nodo "vecchio"
            this.head = head.up!!
            // head diventa dunque corrispondende con l'altezza massima della main.skipList.skipList
            this.head.down = tmp
        }


        val previous: AuthSkipListNode<String, ByteArray> = find(key)
        // A questo punto devo inserire il nodo con x tra previous e previous.next
        // Questa operazione viene eseguita sempre, a prescindere, perchè nella BaseList sono inseriti tutti gli elementi

        // Questo è il nodo da inserire nella BaseList sopra il quale eventualmente inserire altri nodi (col suo stesso valore)
        val newNode: AuthSkipListNode<String, ByteArray> = AuthSkipListNode(key)

        // Inserimento nella BaseList + adjust puntatori
        newNode.next = previous.next
        previous.next?.prev = newNode
        previous.next = newNode
        newNode.prev = previous

        newNode.setUnderlyingValue(value as Any)
        newNode.setFirstHash(HashUtils.sha256(value.toString().toByteArray()))

//        // Analizzo i tre casi per il secondo hash sulla BaseList
//        val successivo: AuthSkipListNode<K, H>? = newNode.next
//        if(successivo == null) {
//            newNode.setSecondHash(HashUtils.sha256(_valueForPositiveInfinity) as H)
//        }
//        else if(successivo.up != null){
//            newNode.setSecondHash(successivo.getFirstHash())
//        }
//        else{
//            newNode.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as String, successivo.getSecondHash() as String) as H)
//        }

        // Eliminata condizione di Tamassia per nodi sulla BaseList, analizzo i due casi rimanenti

        val successivo: AuthSkipListNode<String, ByteArray>? = newNode.next
        if(successivo == null || successivo.up != null){
            newNode.setSecondHash(null)
        }
        else {
            if(successivo.getSecondHash() != null){
                newNode.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
            } else {
                newNode.setSecondHash(successivo.getFirstHash())
            }
        }

        // Ciclo while che, per ogni HEAD estratta, ricava dallo stack il path inverso e si posiziona sul nodo previous del livello immediatamente superiore
        // Il numero di livelli necessario si presuppone corretto perchè aggiornato sopra

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

            // Effettuo l'aggiunta
            val block: AuthSkipListNode<String, ByteArray> = AuthSkipListNode(key)

            block.next = currentPair.first.next
            if (currentPair.first.next != null) {
                currentPair.first.next!!.prev = block
            }
            currentPair.first.next = block
            block.prev = currentPair.first              //if i want double links


            // Aggiorno prevTower.up e block.down
            block.down = prevTower
            prevTower.up = block                        //if i want double links


            // Ora che sono sistemati i puntatori, aggiorno gli hash del nodo block
            if(block.next == null || block.next!!.up != null){        //Allora a destra ho un tower oppure null
                block.setFirstHash(prevTower.getFirstHash())
                block.setSecondHash(prevTower.getSecondHash())
            }
            else {                               //Allora a destra ho un plateau
                val successivo = block.next
                if(prevTower.getSecondHash() != null){
                    block.setFirstHash(HashUtils.sha256(prevTower.getFirstHash() as ByteArray, prevTower.getSecondHash() as ByteArray))
                } else {
                    block.setFirstHash(prevTower.getFirstHash())
                }
                if(successivo!!.getSecondHash() != null){
                    block.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
                } else {
                    block.setSecondHash(successivo.getFirstHash())
                }
            }

            // Aggiorno prevTower
            prevTower = block

            // Ultimo passo per uscire dal while
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

        // Devo procedere un passo in avanti a seconda di cosa è scritto nel primo pop ed elaborare questo nodo raggiunto
        // Qui è corretto pensare solo al nodo next, perchè il firstHash è sempre presente e relativo all'Underlying value
        if(element.second == "right"){
            val firstNode = element.first.next
            // Analizzo i tre casi per il secondo hash sulla BaseList
            val successivo: AuthSkipListNode<String, ByteArray>? = firstNode!!.next
            if(successivo == null || successivo.up != null) {
                firstNode.setSecondHash(null)
            }
            else{
                if(successivo.getSecondHash() != null){
                    firstNode.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
                } else {
                    firstNode.setSecondHash(successivo.getFirstHash())
                }
            }
        }
        else {
            val firstNode = element.first.down as AuthSkipListNode<String, ByteArray>
            // Analizzo i tre casi per il secondo hash sulla BaseList
            val successivo = firstNode.next
            if(successivo == null || successivo.up != null) {
                firstNode.setSecondHash(null)
            }
            else{
                if(successivo.getSecondHash() != null){
                    firstNode.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
                } else {
                    firstNode.setSecondHash(successivo.getFirstHash())
                }
            }
        }

        var stillInBaseList = true


        if (element.second == "down") stillInBaseList = false

        if (stillInBaseList) {            // Tratto qui i nodi sulla BaseList, che possono essere più d'uno
            // Analizzo i tre casi per il secondo hash sulla BaseList
            // Anche qui è corretto pensare solo al nodo next, perchè il firstHash è sempre presente e relativo all'underlying value
            val successivo: AuthSkipListNode<String, ByteArray>? = element.first.next
            if (successivo == null || successivo.up != null) {
                element.first.setSecondHash(null)
            } else {
                if (successivo.getSecondHash() != null) {
                    element.first.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
                } else {
                    element.first.setSecondHash(successivo.getFirstHash())
                }
            }
        } else {                           // Tratto qui tutti i nodi superiori, tutti allo stesso modo
            prevTower = element.first.down as AuthSkipListNode<String, ByteArray>
            if (element.first.next == null || element.first.next!!.up != null) {        //Allora a destra ho un tower oppure null
                element.first.setFirstHash(prevTower.getFirstHash())
                element.first.setSecondHash(prevTower.getSecondHash())
            } else {                               //Allora a destra ho un plateau
                val successivo = element.first.next

                if (prevTower.getSecondHash() != null) {
                    element.first.setFirstHash(HashUtils.sha256(prevTower.getFirstHash() as ByteArray, prevTower.getSecondHash() as ByteArray))
                } else {
                    element.first.setFirstHash(prevTower.getFirstHash())
                }
                if (successivo!!.getSecondHash() == null) {
                    element.first.setSecondHash(successivo.getFirstHash())
                } else {
                    element.first.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
                }
            }
        }

        while(!updateHashes.isEmpty()){     // ho altri elementi da elaborare

            element = updateHashes.pop()
            if (element.second == "down") stillInBaseList = false

            if (stillInBaseList) {            // Tratto qui i nodi sulla BaseList, che possono essere più d'uno
                // Analizzo i tre casi per il secondo hash sulla BaseList
                val successivo: AuthSkipListNode<String, ByteArray>? = element.first.next
                if (successivo == null || successivo.up != null) {
                    element.first.setSecondHash(null)
                } else {
                    if (successivo.getSecondHash() != null) {
                        element.first.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
                    } else {
                        element.first.setSecondHash(successivo.getFirstHash())
                    }
                }
            } else {                           // Tratto qui tutti i nodi superiori, tutti allo stesso modo
                prevTower = element.first.down as AuthSkipListNode<String, ByteArray>
                if (element.first.next == null || element.first.next!!.up != null) {        //Allora a destra ho un tower oppure null
                    element.first.setFirstHash(prevTower.getFirstHash())
                    element.first.setSecondHash(prevTower.getSecondHash())
                } else {                               //Allora a destra ho un plateau
                    val successivo = element.first.next
                    prevTower = element.first.down as AuthSkipListNode<String, ByteArray>

                    if (prevTower.getSecondHash() != null) {
                        element.first.setFirstHash(HashUtils.sha256(prevTower.getFirstHash() as ByteArray, prevTower.getSecondHash() as ByteArray))
                    } else {
                        element.first.setFirstHash(prevTower.getFirstHash())
                    }
                    if (successivo!!.getSecondHash() == null) {
                        element.first.setSecondHash(successivo.getFirstHash())
                    } else {
                        element.first.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
                    }
                }
            }
        }

        return HashUtils.sha256(element.first.getFirstHash() as ByteArray, element.first.getSecondHash() as ByteArray)
    }

    /**
     * Rimuove un nodo alla skipList sulla base della chiave passata
     *
     * @see containsKey
     *
     * @param k
     *
     * @return true se la rimozione è avvenuta, false altrimenti
     */
    private fun applyDel(k: String): ByteArray {

        // Utile in fase di testing
        stack.clear()

        if (!containsKey(k)) {
            if (getHead().getSecondHash() != null){
                return HashUtils.sha256(getHead().getFirstHash() as ByteArray, getHead().getSecondHash() as ByteArray)
            } else {
                return getHead().getFirstHash() as ByteArray
            }
        }

        // Svuoto lo stack per eliminare il path di ricerca di k
        stack.clear()

        // Un elemento sarà cancellato
        size--


        val element: AuthSkipListNode<String, ByteArray> = find(k)

        // Devo eliminare il nodo dalla BaseList aggiornando i vari puntatori
        // Questa operazione viene eseguita sempre, a prescindere, perchè nella BaseList sono presenti tutti gli elementi

        // Rimozione dalla BaseList + adjust puntatori
        element.prev?.next = element.next
        element.next?.prev = element.prev


        // Ciclo while che ricava dallo stack il path inverso e si posiziona sul nodo da eliminare del livello immediatamente superiore

        var level = 0

        // qui elimino i nodi superiori

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

        // A questo punto gli unici nodi il cui hash va aggiornato sono quelli presenti nel nuovo path ricalcolato per trovare il nodo appena cancellato

        find(k)

        var stillInBaseList = true
        var current: Pair<AuthSkipListNode<String, ByteArray>, String> = Pair(getHead(), "right")

        // Aggiorno il nodo head
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

            if(stillInBaseList){            // Tratto qui i nodi sulla BaseList, che possono essere più d'uno
                val successivo: AuthSkipListNode<String, ByteArray>? = current.first.next
                if(successivo == null || successivo.up != null) {
                    current.first.setSecondHash(null)
                }
                else{
                    if(successivo.getSecondHash() != null){
                        current.first.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
                    } else {
                        current.first.setSecondHash(successivo.getFirstHash())
                    }
                }
            }
            else{                           // Tratto qui tutti i nodi superiori, tutti allo stesso modo
                val sottostante = current.first.down as AuthSkipListNode<String, ByteArray>
                if(current.first.next == null || current.first.next!!.up != null){        //Allora a destra ho un tower oppure null
                    current.first.setFirstHash(sottostante.getFirstHash())
                    current.first.setSecondHash(sottostante.getSecondHash())
                }
                else {                               //Allora a destra ho un plateau
                    val successivo = current.first.next
                    if(sottostante.getSecondHash() != null){
                        current.first.setFirstHash(HashUtils.sha256(sottostante.getFirstHash() as ByteArray, sottostante.getSecondHash() as ByteArray))
                    } else {
                        current.first.setFirstHash(sottostante.getFirstHash())
                    }
                    if(successivo!!.getSecondHash() != null){
                        current.first.setSecondHash(HashUtils.sha256(successivo.getFirstHash() as ByteArray, successivo.getSecondHash() as ByteArray))
                    } else {
                        current.first.setSecondHash(successivo.getFirstHash())
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
     * Data una chiave, ritorna una coppia contenente il valore associato alla chiave e
     * la proof relativa a quell chiave
     *
     * @see getValue
     * @see find
     *
     * @param k
     *
     * @return Pair<V, proof<H, K, V>> se la chiave è presente, altrimenti ritorna Pair<null, proof<H, K, V>> dove
     *         quest'ultima è la proof per la chiave immediatamente precedente a quella data
     *
     */
    override fun getWithProof(k: String): Pair<Any?, Proof<ByteArray, String, Any>> {

        stack.clear()
        val baseNode = find(k)

        var proof = AuthSkipListNode(baseNode.getKey(), baseNode.getFirstHash(), baseNode.getSecondHash())

        // Controllo se a il nodo adiacente a destra è un tower o plateau e nel caso lo aggiungo alla proof
        val nextOnBase = baseNode.next
        if(nextOnBase != null && nextOnBase.up == null){        //a destra ho un plateau
            val tmp = AuthSkipListNode(nextOnBase.getKey(), nextOnBase.getFirstHash(), nextOnBase.getSecondHash())
            proof.next = tmp
            tmp.prev = proof
        }

        while(!stack.isEmpty()){
            val successivo = stack.pop()

            // Copio il nodo appena ritirato in tmp
            val tmp = AuthSkipListNode(successivo.first.getKey(), successivo.first.getFirstHash(), successivo.first.getSecondHash())
            tmp.down = successivo.first.down

            if(successivo.second == "down"){
                proof.up = tmp
                tmp.down = proof
                val adjacent = successivo.first.next
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
     * Data una Lista di chiavi, calcola e restituisce la proof unica per tutti quelle coppie chiave-valore
     *
     * @param keys
     *
     * @return proof<H, K, V>
     *
     */
    override fun getProof(keys: List<String>): Proof<ByteArray, String, Any> {

        if (!containsKey(keys[0])) throw IllegalArgumentException("Inserisci solo chiavi che sono presenti")
        var proof = this.getWithProof(keys[0]).second

        for (k in keys){
            if (!containsKey(k)) throw IllegalArgumentException("Inserisci solo chiavi che sono presenti")
            proof = proof.union(this.getWithProof(k).second)
        }

        return proof
    }

    /**
     *  Copia la struttura e ne restituisce una identica, tranne per i delta che NON vengono copiati
     *
     *  @return ADS<H, K, V> che rappresenta la copia
     */
    private fun clone(): ADS<ByteArray, String, Any> {

        // Ottengo l'altezza della skiplist
        val height = this.getMaxLevel()

        // Creo la torre dei dummies
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

        // Caso di lista vuota
        if(this.size == 0) return clone

        // Creo array che conterrà le SortedMap relative alla skiplist
        val maps = Array<SortedMap<String, AuthSkipListNode<String, ByteArray>?>>(height + 1, { _ -> sortedMapOf()})

        var verticalIterator = this.start

        // Itero sulla BaseList
        var baseListIterator = verticalIterator.next
        maps[0] = sortedMapOf(Pair(baseListIterator!!.getKey(), baseListIterator))
        while(baseListIterator!!.next != null){
            maps[0][baseListIterator.next!!.getKey()] = baseListIterator.next
            baseListIterator = baseListIterator.next
        }

        if(height > 0) {

            verticalIterator = verticalIterator.up!!

            for (i in 1..height) {

                // Preparo i due iteratori paralleli
                var horizontalIterator = verticalIterator
                var baseLineIterator = this.start

                // Scorro in parallelo le due mappe
                while(baseLineIterator.next != null){
                    val successivoSuperiore = horizontalIterator.next
                    if(successivoSuperiore == null) break
                    else {
                        while(baseLineIterator.next!!.getKey() < successivoSuperiore.getKey()){
                            maps[i][baseLineIterator.next!!.getKey()] = null
                            baseLineIterator = baseLineIterator.next!!
                        }
                    }
                    maps[i][baseLineIterator.next!!.getKey()] = successivoSuperiore

                    horizontalIterator = horizontalIterator.next!!
                    baseLineIterator = baseLineIterator.next!!
                }

                // Aggiorno l'iteratore superiore
                if (verticalIterator.up != null) {
                    verticalIterator = verticalIterator.up!!
                }
            }
        }


        // Finita la costruzione delle SortedMap, ora istanzio i nuovi nodi aggiornando i valori

        for (map in maps) {
            for((key, node) in map){
                if(node != null){
                    val clonedNode = AuthSkipListNode(node.getKey(), node.getFirstHash(), node.getSecondHash())
                    clonedNode.setUnderlyingValue(node.getUnderlyingValue())
                    map[key] = clonedNode
                }
            }
        }

        // Sistemo i puntatori orizzontali
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

        // Sistemo i puntatori verticali

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

        // Restituisco la SkipList
        return clone
    }

    /**
     *  |--- Design pattern Modello funzionale ---|
     *
     * Compatta tutti i vari delta relativi alla struttura corrente, e li applica in ordine
     *
     * @return Una nuova ADS<H, K, V> con i delta attualizzati
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
        // Stampo il risultato preoccupandomi di togliere l'ultimo ", "
        return s.substring(0, s.length - 2)
    }

    override fun printADS(){
        val heights = HashMap<String, Int>()
        // Il primo String sono le chiavi reali, nella coppia ho la chiave alias e il valore
        val keys = HashMap<String, Pair<String, Any?>>()
        val hashes = storeHashes()

        var iterator = start
        heights[iterator.getKey()] = getMaxLevel()
        keys[iterator.getKey()] = Pair("K0", iterator.getUnderlyingValue())

        // Calcolo le altezze di tutte le torri e registro tutte le chiavi
        var keyCounter = 1
        while (iterator.next != null) {
            heights[iterator.next!!.getKey()] = calcolaAltezzaColonna(iterator.next!!)
            keys[iterator.next!!.getKey()] = Pair("K$keyCounter", iterator.next!!.getUnderlyingValue())
            iterator = iterator.next!!
            keyCounter++
        }

        var currentLevel = getMaxLevel()
        var verticalIterator: Any = getHead()
        var lunghezzaSpazi = 14

        // Itero orizzontalmente per ogni livello e stampo
        while (verticalIterator is AuthSkipListNode<*, *>) {
            var horizontalIterator = verticalIterator as AuthSkipListNode<String, ByteArray>?

            System.out.printf("%-10s", "Level: $currentLevel ")

            while (horizontalIterator != null) {
                val nodeHeight = heights[horizontalIterator.getKey()]

                if (nodeHeight!! >= currentLevel) {
                    val alias = keys[horizontalIterator.getKey()]!!.first+"."+currentLevel
                    val lunghezzaAlias = Math.max(5, alias.length)
                    var counter = 5
                    while (counter != lunghezzaAlias){
                        counter++
                        lunghezzaSpazi++
                    }
                    System.out.printf("%s%.8s%s", "[--", " ${keys[horizontalIterator.getKey()]!!.first}.$currentLevel --", "--]")
                } else {
                    System.out.printf("%${lunghezzaSpazi-1}s", "")
                }

                if(horizontalIterator.next != null) {
                    val numeroSpaziVuoti = calcolaNumeroSpazi(horizontalIterator, horizontalIterator.next!!)
                    for (i in 0..numeroSpaziVuoti) {
                        System.out.printf("%${lunghezzaSpazi}s", "")
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

    private fun calcolaAltezzaColonna(node: AuthSkipListNode<String, ByteArray>): Int{
        var altezza = 0
        var localNode = node
        while(localNode.up != null) {
            altezza++
            localNode = localNode.up!!
        }
        return altezza
    }

    private fun calcolaNumeroSpazi(node: AuthSkipListNode<String, ByteArray>, next: AuthSkipListNode<String, ByteArray>): Int {

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
     * Informa se la SkipList è vuota, cioè non contiene elementi, o meno
     *
     * return true se vuota, false altrimenti
     *
     */
    override fun isEmpty(): Boolean {
        if(start.next == null)
            return true
        return false
    }

}
