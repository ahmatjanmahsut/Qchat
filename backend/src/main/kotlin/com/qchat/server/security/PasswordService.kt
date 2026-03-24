package com.qchat.server.security

import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory

/**
 * Password hashing service using BCrypt
 */
object PasswordService {
    private val logger = LoggerFactory.getLogger("PasswordService")

    private const val BCRYPT_ROUNDS = 12

    /**
     * Hash a password using BCrypt
     */
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS))
    }

    /**
     * Verify a password against a hash
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return try {
            BCrypt.checkpw(password, hash)
        } catch (e: Exception) {
            logger.error("Error verifying password", e)
            false
        }
    }

    /**
     * Validate password strength
     */
    fun isPasswordStrong(password: String): Boolean {
        // Password must be at least 8 characters
        // and contain at least one uppercase, one lowercase, and one digit
        if (password.length < 8) return false

        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }

        return hasUppercase && hasLowercase && hasDigit
    }
}
