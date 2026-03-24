package com.qchat.server.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Session table - stores active user sessions
 */
object Sessions : Table("sessions") {
    val id = varchar("id", 36).primaryKey() // UUID
    val userId = varchar("user_id", 36).references(Users.id)
    val token = varchar("token", 512).uniqueIndex()
    val deviceId = varchar("device_id", 255).nullable()
    val deviceName = varchar("device_name", 100).nullable()
    val deviceType = varchar("device_type", 50).nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = varchar("user_agent", 500).nullable()
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")
    val lastAccessedAt = timestamp("last_accessed_at")
    val isActive = bool("is_active").default(true)
}
