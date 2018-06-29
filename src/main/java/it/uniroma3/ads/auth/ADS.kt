package it.uniroma3.ads.auth

import it.uniroma3.ads.proof.Proof
import java.io.Serializable

interface ADS<H, K, V> : Serializable {

    /**
     * Returns the value associated with the given key, together with the proof for that key
     *
     * @param k
     *
     * @return V if present, null otherwise
     * @return Proof<H, K, V> for the key
     *
     */
    fun getWithProof(k: K): Pair<V?, Proof<H, K, V>>

    /**
     * Given a list of keys, computes and returns a single proof covering all those key-value pairs
     *
     * @param keys
     *
     * @return proof<H, K, V>
     *
     */
    fun getProof(keys: List<K>): Proof<H, K, V>

    /**
     * Given a key, returns the value associated with it
     *
     * @param k
     *
     * @return V if present, null otherwise
     *
     */
    fun getValue(k: K): V?

    /**
     * Operations that create deltas relative to the current structure, WITHOUT modifying it
     *
     * @return H, rootHash of the current structure
     *
     */
    fun add(k: K, v: V): H
    fun del(k: K): H

    /**
     *  |--- Functional model design pattern ---|
     *
     * Collapses all deltas relative to the current structure and applies them in order
     *
     * @return A new ADS<H, K, V> with the deltas applied
     *
     */
    fun applyDeltas(): ADS<H, K, V>

    /**
     * Computes the rootHash of the current structure
     *
     * @return rootHash H
     *
     */
    fun rootHash(): H

    /**
     * Reports whether the ADS is empty (contains no elements) or not
     *
     * return true if empty, false otherwise
     *
     */
    fun isEmpty(): Boolean

    /**
     *  Prints the structure to screen
     *
     */
    fun printADS()

}