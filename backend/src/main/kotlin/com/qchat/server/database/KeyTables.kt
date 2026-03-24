package com.qchat.server.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * User keys table - stores encryption keys for E2E encryption
 */
object UserKeys : Table("user_keys") {
    val id = varchar("id", 36).primaryKey()
    val userId = varchar("user_id", 36).references(Users.id)
    val keyType = enumeration("key_type", KeyType::class)
    val publicKey = text("public_key")
    val privateKeyEncrypted = text("private_key_encrypted").nullable() // Encrypted with user's password
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val isActive = bool("is_active").default(true)
}

/**
 * User status table - stores online/offline status
 */
object UserStatus : Table("user_status") {
    val id = varchar("id", 36).primaryKey()
    val userId = varchar("user_id", 36).references(Users.id).uniqueIndex()
    val status = enumeration("status", UserStatusType::class).default(UserStatusType.OFFLINE)
    val lastSeenAt = timestamp("last_seen_at")
    val isOnline = bool("is_online").default(false)
}

enum class KeyType {
    IDENTITY, SESSION, MESSAGE
}

enum class UserStatusType {
    ONLINE, AWAY, BUSY, OFFLINE
}
