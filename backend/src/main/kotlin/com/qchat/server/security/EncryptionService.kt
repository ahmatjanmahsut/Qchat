package com.qchat.server.security

import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Base64

/**
 * Encryption service for generating encryption keys and handling encryption operations
 */
object EncryptionService {
    private val logger = LoggerFactory.getLogger("EncryptionService")

    private val secureRandom = SecureRandom()

    /**
     * Generate a random encryption key
     */
    fun generateKey(length: Int = 32): String {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * Generate a random session ID
     */
    fun generateSessionId(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Generate a random salt
     */
    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}
