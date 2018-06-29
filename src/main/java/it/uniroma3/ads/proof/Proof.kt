package it.uniroma3.ads.proof

import java.io.Serializable

interface Proof<H, K, V> : Serializable {

    /**
     * Interface property che tiene traccia delle chiavi a cui è associata un proof.
     */
    var keys: MutableSet<K>

    /**
     * Restituisce il rootHash della Proof
     *
     * @return rootHash H
     *
     */
    fun rootHash(): H

    /**
     * Effettua il merge di due proof relative alla stessa struttura nello stesso istante temporale
     *
     * @param proof, da unire
     *
     * @return proof<H, K, V>, unione delle due proof
     *
     */
    fun union(proof: Proof<H, K, V>): Proof<H, K, V>

    /**
     * Se non sono forniti i valori, l'operazione è di sola lettura, Quindi viene calcolato e restituito il rootHash.
     * Se è presente anche un V != null allora l'operazione è di scrittura. Viene restituita un'altra struttura aggiornata, assieme al suo nuovo rootHash.
     *
     * @param list
     *
     * @return rootHash
     *
     */
    fun rootHash(list: List<Pair<K, V?>>): Pair<Proof<H, K, V>, H>

    /**
     *  Stampa su schermo i path per ogni chiave di cui è composta la proof
     */
    fun printProof()

}