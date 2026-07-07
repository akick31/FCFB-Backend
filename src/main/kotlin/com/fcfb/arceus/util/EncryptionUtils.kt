package com.fcfb.arceus.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class EncryptionUtils(
    @Value("\${encryption.algorithm}")
    private val algorithm: String,
    @Value("\${encryption.key}")
    private val encryptionKey: String,
) {
    @Throws(Exception::class)
    fun encrypt(value: String): String {
        // Fresh IV per call so identical plaintexts don't produce identical ciphertext
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        val secretKey = SecretKeySpec(encryptionKey!!.toByteArray(), "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedBytes = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))

        val ivAndEncrypted = iv + encryptedBytes
        return Base64.getEncoder().encodeToString(ivAndEncrypted)
    }

    @Throws(Exception::class)
    fun decrypt(encryptedValue: String): String {
        val decodedBytes = Base64.getDecoder().decode(encryptedValue)
        val iv = decodedBytes.copyOfRange(0, 16)
        val encryptedBytes = decodedBytes.copyOfRange(16, decodedBytes.size)
        val ivSpec = IvParameterSpec(iv)
        val secretKey = SecretKeySpec(encryptionKey!!.toByteArray(), "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    fun hash(value: String): String {
        val bytes = value.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
