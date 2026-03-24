package com.qchat.server.models

import kotlinx.serialization.Serializable

/**
 * User registration request
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val displayName: String? = null,
    val phoneNumber: String? = null
)

/**
 * User login request
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val deviceType: String? = null
)

/**
 * Authentication response containing tokens
 */
@Serializable
data class AuthResponse(
    val userId: String,
    val username: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

/**
 * Token refresh request
 */
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

/**
 * User profile update request
 */
@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val phoneNumber: String? = null
)

/**
 * User profile response
 */
@Serializable
data class UserProfileResponse(
    val id: String,
    val username: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val phoneNumber: String? = null,
    val bio: String? = null,
    val isVerified: Boolean,
    val createdAt: String,
    val isOnline: Boolean = false
)

/**
 * Simple user info for lists and searches
 */
@Serializable
data class UserInfo(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false
)

/**
 * Password change request
 */
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
