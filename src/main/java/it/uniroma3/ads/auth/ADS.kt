package it.uniroma3.ads.auth

import it.uniroma3.ads.proof.Proof
import java.io.Serializable

interface ADS<H, K, V> : Serializable {

    /**
     * Restituisce il valore associato alla chiave passata assieme alla proof per quella chiave
     *
     * @param k
     *
     * @return V se presente, null altrimenti
     * @return Proof<H, K, V> relativa alla chiave
     *
     */
    fun getWithProof(k: K): Pair<V?, Proof<H, K, V>>

    /**
     * Data una Lista di chiavi, calcola e restituisce la proof unica per tutti quelle coppie chiave-valore
     *
     * @param keys
     *
     * @return proof<H, K, V>
     *
     */
    fun getProof(keys: List<K>): Proof<H, K, V>

    /**
     * Data una chiave restituisce il valore associato ad essa
     *
     * @param k
     *
     * @return V se presente, null altrimenti
     *
     */
    fun getValue(k: K): V?

    /**
     * Operazioni che creano dei delta rispetto alla struttura corrente, SENZA modificarla
     *
     * @return H, rootHash relativo alla struttura corrente
     *
     */
    fun add(k: K, v: V): H
    fun del(k: K): H

    /**
     *  |--- Design pattern Modello funzionale ---|
     *
     * Compatta tutti i vari delta relativi alla struttura corrente, e li applica in ordine
     *
     * @return Una nuova ADS<H, K, V> con i delta attualizzati
     *
     */
    fun applyDeltas(): ADS<H, K, V>

    /**
     * Calcola il rootHash della struttura corrente
     *
     * @return rootHash H
     *
     */
    fun rootHash(): H

    /**
     * Informa se l'ADS è vuoto, cioè non contiene elementi, o meno
     *
     * return true se vuoto, false altrimenti
     *
     */
    fun isEmpty(): Boolean

    /**
     *  Stampa su schermo la struttura
     *
     */
    fun printADS()

}