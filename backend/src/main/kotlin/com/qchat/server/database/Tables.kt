package com.qchat.server.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * User table - stores user account information
 */
object Users : Table("users") {
    val id = varchar("id", 36).primaryKey() // UUID
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val displayName = varchar("display_name", 100)
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val phoneNumber = varchar("phone_number", 20).nullable()
    val bio = text("bio").nullable()
    val publicKey = text("public_key").nullable() // For E2E encryption
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val isActive = bool("is_active").default(true)
    val isVerified = bool("is_verified").default(false)
}
