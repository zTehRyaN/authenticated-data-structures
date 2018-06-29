package it.uniroma3.pipelineIntegrity.utils

import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

/**
 * KeyUtils contiene funzioni utili per la gestione delle chiavi private e publiche
 */
object KeyUtils {

    /**
     * Restituisce la chiave privata letta dal file fileName
     *
     * @param fileName
     */
    fun privateKey(fileName : String) : PrivateKey {

        var privateKeyContent = String(Files.readAllBytes(Paths.get(fileName)))
        privateKeyContent = privateKeyContent.replace("\\n".toRegex(), "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")

        val kf = KeyFactory.getInstance("RSA")

        val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent))
        return kf.generatePrivate(keySpecPKCS8)

    }

    /**
     * Restituisce la chiave pubblica letta dal file fileName
     *
     * @param fileName
     */
    fun publicKey(fileName : String) : PublicKey {

        var publicKeyContent = String(Files.readAllBytes(Paths.get(fileName)))
        publicKeyContent = publicKeyContent.replace("\\n".toRegex(), "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")

        val kf = KeyFactory.getInstance("RSA")

        val keySpecX509 = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent))
        return kf.generatePublic(keySpecX509)
    }

}