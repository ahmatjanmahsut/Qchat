package com.qchat.server.services

import com.qchat.server.database.*
import com.qchat.server.models.*
import com.qchat.server.security.EncryptionService
import com.qchat.server.security.JwtService
import com.qchat.server.security.PasswordService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.currentTimestamp
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * User service for managing user operations
 */
class UserService {
    private val logger = LoggerFactory.getLogger("UserService")
    private val cacheService = CacheService()

    /**
     * Register a new user
     */
    fun registerUser(request: RegisterRequest): Result<User> {
        logger.info("Registering user: ${request.username}")

        // Validate password strength
        if (!PasswordService.isPasswordStrong(request.password)) {
            return Result.failure(Exception("Password must be at least 8 characters with uppercase, lowercase, and digits"))
        }

        // Check if username or email already exists
        transaction {
            val existingUser = Users.select {
                (Users.username eq request.username) or (Users.email eq request.email)
            }.firstOrNull()

            if (existingUser != null) {
                val existingUsername = existingUser[Users.username]
                val existingEmail = existingUser[Users.email]
                when {
                    existingUsername == request.username -> return@transaction Result.failure(Exception("Username already taken"))
                    existingEmail == request.email -> return@transaction Result.failure(Exception("Email already registered"))
                }
            }
        }

        // Create user
        val userId = UUID.randomUUID().toString()
        val passwordHash = PasswordService.hashPassword(request.password)
        val displayName = request.displayName ?: request.username

        transaction {
            Users.insert {
                it[Users.id] = userId
                it[Users.username] = request.username
                it[Users.email] = request.email
                it[Users.passwordHash] = passwordHash
                it[Users.displayName] = displayName
                it[Users.phoneNumber] = request.phoneNumber
                it[Users.createdAt] = currentTimestamp()
                it[Users.updatedAt] = currentTimestamp()
                it[Users.isActive] = true
                it[Users.isVerified] = false
            }
        }

        logger.info("User registered successfully: $userId")
        return getUserById(userId) ?: Result.failure(Exception("Failed to retrieve created user"))
    }

    /**
     * Authenticate user with username and password
     */
    fun authenticateUser(request: LoginRequest): Result<AuthResponse> {
        logger.info("Authenticating user: ${request.username}")

        val user = transaction {
            Users.select { Users.username eq request.username }
                .firstOrNull()
        } ?: return Result.failure(Exception("Invalid username or password"))

        val userId = user[Users.id]
        val passwordHash = user[Users.passwordHash]

        if (!PasswordService.verifyPassword(request.password, passwordHash)) {
            logger.warn("Invalid password for user: ${request.username}")
            return Result.failure(Exception("Invalid username or password"))
        }

        // Check if user is active
        if (!user[Users.isActive]) {
            return Result.failure(Exception("Account is deactivated"))
        }

        // Generate tokens
        val accessToken = JwtService.generateToken(userId, request.username)
        val refreshToken = JwtService.generateRefreshToken(userId)

        // Create session
        createSession(userId, request, accessToken)

        // Update user status
        updateUserOnlineStatus(userId, true)

        logger.info("User authenticated successfully: $userId")

        return Result.success(
            AuthResponse(
                userId = userId,
                username = request.username,
                displayName = user[Users.displayName],
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = 86400 // 24 hours
            )
        )
    }

    /**
     * Get user by ID
     */
    fun getUserById(userId: String): User? {
        // Check cache first
        cacheService.getCachedUser(userId)?.let { cached ->
            return User(
                id = cached.id,
                username = cached.username,
                email = "",
                displayName = cached.displayName,
                avatarUrl = cached.avatarUrl,
                isOnline = cacheService.isUserOnline(cached.id)
            )
        }

        return transaction {
            Users.select { Users.id eq userId }
                .firstOrNull()?.let { row ->
                    val user = User(
                        id = row[Users.id],
                        username = row[Users.username],
                        email = row[Users.email],
                        displayName = row[Users.displayName],
                        avatarUrl = row[Users.avatarUrl],
                        phoneNumber = row[Users.phoneNumber],
                        bio = row[Users.bio],
                        isActive = row[Users.isActive],
                        isVerified = row[Users.isVerified],
                        createdAt = row[Users.createdAt].toString()
                    )

                    // Cache user
                    cacheService.cacheUser(
                        userId,
                        UserCacheData(
                            id = user.id,
                            username = user.username,
                            displayName = user.displayName,
                            avatarUrl = user.avatarUrl
                        )
                    )

                    user
                }
        }
    }

    /**
     * Get user profile by ID (public info)
     */
    fun getUserProfile(userId: String): UserProfileResponse? {
        return getUserById(userId)?.let { user ->
            UserProfileResponse(
                id = user.id,
                username = user.username,
                email = user.email,
                displayName = user.displayName,
                avatarUrl = user.avatarUrl,
                phoneNumber = user.phoneNumber,
                bio = user.bio,
                isVerified = user.isVerified,
                createdAt = user.createdAt,
                isOnline = cacheService.isUserOnline(userId)
            )
        }
    }

    /**
     * Update user profile
     */
    fun updateUserProfile(userId: String, request: UpdateProfileRequest): Result<User> {
        logger.info("Updating profile for user: $userId")

        transaction {
            Users.update({ Users.id eq userId }) {
                request.displayName?.let { it[Users.displayName] = request.displayName }
                request.bio?.let { it[Users.bio] = request.bio }
                request.avatarUrl?.let { it[Users.avatarUrl] = request.avatarUrl }
                request.phoneNumber?.let { it[Users.phoneNumber] = request.phoneNumber }
                it[Users.updatedAt] = currentTimestamp()
            }
        }

        // Invalidate cache
        cacheService.invalidateUser(userId)

        return getUserById(userId)?.let { Result.success(it) }
            ?: Result.failure(Exception("User not found"))
    }

    /**
     * Search users by username or display name
     */
    fun searchUsers(query: String, limit: Int = 20): List<UserInfo> {
        return transaction {
            Users.select {
                (Users.username like "%$query%") or (Users.displayName like "%$query%")
            }
                .limit(limit)
                .map { row ->
                    UserInfo(
                        id = row[Users.id],
                        username = row[Users.username],
                        displayName = row[Users.displayName],
                        avatarUrl = row[Users.avatarUrl],
                        isOnline = cacheService.isUserOnline(row[Users.id])
                    )
                }
        }
    }

    /**
     * Change user password
     */
    fun changePassword(userId: String, currentPassword: String, newPassword: String): Result<Boolean> {
        logger.info("Changing password for user: $userId")

        // Verify current password
        val user = transaction {
            Users.select { Users.id eq userId }.firstOrNull()
        } ?: return Result.failure(Exception("User not found"))

        if (!PasswordService.verifyPassword(currentPassword, user[Users.passwordHash])) {
            return Result.failure(Exception("Current password is incorrect"))
        }

        // Validate new password
        if (!PasswordService.isPasswordStrong(newPassword)) {
            return Result.failure(Exception("New password must be at least 8 characters with uppercase, lowercase, and digits"))
        }

        // Update password
        val newHash = PasswordService.hashPassword(newPassword)
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.passwordHash] = newHash
                it[Users.updatedAt] = currentTimestamp()
            }
        }

        logger.info("Password changed successfully for user: $userId")
        return Result.success(true)
    }

    /**
     * Create a new session
     */
    private fun createSession(userId: String, request: LoginRequest, token: String) {
        val sessionId = UUID.randomUUID().toString()

        transaction {
            Sessions.insert {
                it[Sessions.id] = sessionId
                it[Sessions.userId] = userId
                it[Sessions.token] = token
                it[Sessions.deviceId] = request.deviceId
                it[Sessions.deviceName] = request.deviceName
                it[Sessions.deviceType] = request.deviceType
                it[Sessions.createdAt] = currentTimestamp()
                it[Sessions.expiresAt] = java.time.Instant.now().plusSeconds(86400) // 24 hours
                it[Sessions.lastAccessedAt] = currentTimestamp()
                it[Sessions.isActive] = true
            }
        }

        // Cache session
        cacheService.cacheSession(
            sessionId,
            SessionCacheData(
                sessionId = sessionId,
                userId = userId,
                deviceId = request.deviceId
            )
        )
    }

    /**
     * Update user online status
     */
    fun updateUserOnlineStatus(userId: String, isOnline: Boolean) {
        if (isOnline) {
            cacheService.setUserOnline(userId)
        } else {
            cacheService.setUserOffline(userId)
        }

        transaction {
            UserStatus.insert {
                it[UserStatus.userId] = userId
                it[UserStatus.status] = if (isOnline) UserStatusType.ONLINE else UserStatusType.OFFLINE
                it[UserStatus.lastSeenAt] = currentTimestamp()
                it[UserStatus.isOnline] = isOnline
            }
        }
    }
}

data class User(
    val id: String,
    val username: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val phoneNumber: String? = null,
    val bio: String? = null,
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val createdAt: String = ""
)
