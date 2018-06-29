package it.uniroma3.ads.auth

import java.security.MessageDigest

object HashUtils {

    fun sha512(input: String) = hashString("SHA-512", input)

    fun sha256(input: String) = hashString("SHA-256", input)

    fun sha1(input: String) = hashString("SHA-1", input)

    fun sha512(input1: String, input2: String) = hashPipe("SHA-512", input1, input2)

    fun sha256(input1: String, input2: String) = hashPipe("SHA-256", input1, input2)

    fun sha1(input1: String, input2: String) = hashPipe("SHA-1", input1, input2)

    fun sha512(input: ByteArray) = hashString("SHA-512", input)

    fun sha256(input: ByteArray) = hashString("SHA-256", input)

    fun sha1(input: ByteArray) = hashString("SHA-1", input)

    fun sha512(input1: ByteArray, input2: ByteArray) = hashPipe("SHA-512", input1, input2)

    fun sha256(input1: ByteArray, input2: ByteArray) = hashPipe("SHA-256", input1, input2)

    fun sha1(input1: ByteArray, input2: ByteArray) = hashPipe("SHA-1", input1, input2)

    private fun hashString(type: String, input: String): String {
        val hexChars = "0123456789ABCDEF"
        val bytes = MessageDigest.getInstance(type).digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }

        return result.toString()
    }

    private fun hashString(type: String, input: ByteArray) = MessageDigest.getInstance(type).digest(input)

    private fun hashPipe(type: String, input1: String, input2: String): String {
        val hexChars = "0123456789ABCDEF"
        val bytes = MessageDigest.getInstance(type).digest((input1+input2).toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }

        return result.toString()
    }

    private fun hashPipe(type: String, input1: ByteArray, input2: ByteArray) = MessageDigest.getInstance(type).digest((input1+input2))

    /**
     * Metodo per consentire corretta visualizzazione degli hash in formato byteArray
     * Utile in fase di testing.
     *
     * Per tornare a rendere parametrico AuthSkipListNode basta eliminare metodo e riportare il toString a condizioni originarie
     *
     */
    fun toHex(array: ByteArray?): String {

        if (array == null) return "null"

        array as ByteArray
        val result = StringBuffer()

        for (b in array) {
            val st = String.format("%02X", b)
            result.append(st)
        }

        return result.toString()
    }
}