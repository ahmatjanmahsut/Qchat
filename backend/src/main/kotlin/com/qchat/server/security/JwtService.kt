package com.qchat.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.qchat.server.config.AppConfig
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.UUID

/**
 * JWT Service for token generation and validation
 */
object JwtService {
    private val logger = LoggerFactory.getLogger("JwtService")

    private val algorithm = Algorithm.HMAC256(AppConfig.jwtSecret)

    /**
     * Generate a JWT token for a user
     */
    fun generateToken(userId: String, username: String): String {
        val issuedAt = Date()
        val expiresAt = Date(System.currentTimeMillis() + AppConfig.jwtExpiration)

        return JWT.create()
            .withIssuer(AppConfig.jwtIssuer)
            .withIssuedAt(issuedAt)
            .withExpiresAt(expiresAt)
            .withSubject(userId)
            .withClaim("username", username)
            .withJWTId(UUID.randomUUID().toString())
            .sign(algorithm)
    }

    /**
     * Generate a refresh token
     */
    fun generateRefreshToken(userId: String): String {
        val issuedAt = Date()
        val expiresAt = Date(System.currentTimeMillis() + AppConfig.jwtExpiration * 7) // 7 days

        return JWT.create()
            .withIssuer(AppConfig.jwtIssuer)
            .withIssuedAt(issuedAt)
            .withExpiresAt(expiresAt)
            .withSubject(userId)
            .withClaim("type", "refresh")
            .withJWTId(UUID.randomUUID().toString())
            .sign(algorithm)
    }

    /**
     * Create a JWT verifier
     */
    fun makeVerifier(): JWTVerifier {
        return JWT.require(algorithm)
            .withIssuer(AppConfig.jwtIssuer)
            .build()
    }

    /**
     * Validate a JWT token
     */
    fun validateToken(jwt: DecodedJWT): JwtUser? {
        return try {
            val userId = jwt.subject
            val username = jwt.getClaim("username").asString()
            val type = jwt.getClaim("type").asString()

            if (userId != null && username != null && type != "refresh") {
                JwtUser(userId, username)
            } else {
                null
            }
        } catch (e: JWTVerificationException) {
            logger.warn("JWT validation failed: ${e.message}")
            null
        }
    }

    /**
     * Decode token without verification (for debugging)
     */
    fun decodeToken(token: String): DecodedJWT? {
        return try {
            JWT.decode(token)
        } catch (e: Exception) {
            logger.error("Failed to decode token", e)
            null
        }
    }
}

data class JwtUser(
    val userId: String,
    val username: String
)
