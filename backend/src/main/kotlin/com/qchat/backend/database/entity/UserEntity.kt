package com.qchat.backend.database.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.`java-time`.JavaInstantColumnType
import org.jetbrains.exposed.sql.kotlin.datetime.InstantColumnType
import java.time.Instant

/**
 * 用户表
 */
object Users : LongIdTable("users") {
    val phoneNumber = varchar("phone_number", 20).uniqueIndex()
    val username = varchar("username", 50).uniqueIndex().nullable()
    val displayName = varchar("display_name", 100)
    val passwordHash = varchar("password_hash", 255)
    val profilePicture = varchar("profile_picture", 500).nullable()
    val publicKey = text("public_key")
    val privateKeyEncrypted = text("private_key_encrypted").nullable()
    val lastSeen = registerColumn("last_seen", InstantColumnType()).nullable()
    val isOnline = bool("is_online").default(false)
    val createdAt = registerColumn("created_at", InstantColumnType()).clientDefault { Instant.now() }
    val updatedAt = registerColumn("updated_at", InstantColumnType()).clientDefault { Instant.now() }
}

class User(id: EntityID<Long>) : Entity<Long>(id) {
    companion object : EntityClass<Long, User>(Users)

    var phoneNumber by Users.phoneNumber
    var username by Users.username
    var displayName by Users.displayName
    var passwordHash by Users.passwordHash
    var profilePicture by Users.profilePicture
    var publicKey by Users.publicKey
    var privateKeyEncrypted by Users.privateKeyEncrypted
    var lastSeen by Users.lastSeen
    var isOnline by Users.isOnline
    var createdAt by Users.createdAt
    var updatedAt by Users.updatedAt
}
