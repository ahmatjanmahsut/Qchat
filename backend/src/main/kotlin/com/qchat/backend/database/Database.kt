package com.qchat.backend.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.postgresql.util.PGobject
import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.*

/**
 * 数据库连接配置
 */
object DatabaseFactory {
    fun init() {
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/qchat",
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "password"
        ) {
            connection {
                // 连接配置
                transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
            }
        }

        transaction {
            // 创建表
            createTables()
        }
    }

    private fun createTables() {
        // 用户表
        create(Users)

        // 设备表
        create(Devices)

        // 预密钥表
        create(PreKeys)

        // 签名预密钥表
        create(SignedPreKeys)

        // 聊天表
        create(Chats)

        // 聊天参与者表
        create(ChatParticipants)

        // 消息表
        create(Messages)

        // 会话表
        create(Sessions)
    }
}

/**
 * 用户表
 */
object Users : Table("users") {
    val id = varchar("id", 64).primaryKey()
    val username = varchar("username", 64).uniqueIndex()
    val displayName = varchar("display_name", 128)
    val passwordHash = varchar("password_hash", 256)
    val avatarUrl = varchar("avatar_url", 512).nullable()
    val phone = varchar("phone", 32).nullable()
    val bio = text("bio").nullable()
    val publicKey = text("public_key").nullable()
    val isVerified = bool("is_verified").default(false)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
}

/**
 * 设备表
 */
object Devices : Table("devices") {
    val id = varchar("id", 64).primaryKey()
    val userId = varchar("user_id", 64).references(Users.id)
    val deviceName = varchar("device_name", 128)
    val platform = varchar("platform", 32)
    val appVersion = varchar("app_version", 32)
    val isPrimary = bool("is_primary").default(false)
    val lastSeen = long("last_seen").nullable()
    val createdAt = long("created_at")
}

/**
 * 预密钥表
 */
object PreKeys : Table("pre_keys") {
    val id = integer("id")
    val userId = varchar("user_id", 64).references(Users.id)
    val deviceId = varchar("device_id", 64).references(Devices.id)
    val publicKey = text("public_key")
    val privateKey = text("private_key")
    val createdAt = long("created_at")

    primaryKey(id, userId, deviceId)
}

/**
 * 签名预密钥表
 */
object SignedPreKeys : Table("signed_pre_keys") {
    val id = integer("id")
    val userId = varchar("user_id", 64).references(Users.id)
    val deviceId = varchar("device_id", 64).references(Devices.id)
    val publicKey = text("public_key")
    val privateKey = text("private_key")
    val signature = text("signature")
    val createdAt = long("created_at")

    primaryKey(id, userId, deviceId)
}

/**
 * 聊天表
 */
object Chats : Table("chats") {
    val id = varchar("id", 64).primaryKey()
    val type = varchar("type", 32)
    val name = varchar("name", 128)
    val avatarUrl = varchar("avatar_url", 512).nullable()
    val description = text("description").nullable()
    val lastMessageId = varchar("last_message_id", 64).nullable()
    val lastMessageTime = long("last_message_time").nullable()
    val createdBy = varchar("created_by", 64).references(Users.id)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
}

/**
 * 聊天参与者表
 */
object ChatParticipants : Table("chat_participants") {
    val chatId = varchar("chat_id", 64).references(Chats.id)
    val userId = varchar("user_id", 64).references(Users.id)
    val role = varchar("role", 32).default("MEMBER")
    val joinedAt = long("joined_at")
    val lastReadMessageId = varchar("last_read_message_id", 64).nullable()

    primaryKey(chatId, userId)
}

/**
 * 消息表
 */
object Messages : Table("messages") {
    val id = varchar("id", 64).primaryKey()
    val localId = varchar("local_id", 64).nullable()
    val chatId = varchar("chat_id", 64).references(Chats.id)
    val senderId = varchar("sender_id", 64).references(Users.id)
    val type = varchar("type", 32)
    val content = text("content")
    val timestamp = long("timestamp")
    val status = varchar("status", 32).default("SENDING")
    val replyTo = varchar("reply_to", 64).nullable()
    val encryptionSessionId = varchar("encryption_session_id", 64).nullable()
    val isDeleted = bool("is_deleted").default(false)
    val expireAt = long("expire_at").nullable()
    val editedAt = long("edited_at").nullable()
    val reactions = text("reactions").default("[]")
}

/**
 * 会话表
 */
object Sessions : Table("sessions") {
    val id = varchar("id", 64).primaryKey()
    val userId = varchar("user_id", 64).references(Users.id)
    val remoteUserId = varchar("remote_user_id", 64).references(Users.id)
    val deviceId = varchar("device_id", 64).nullable()
    val rootKey = text("root_key")
    val chainKey = text("chain_key")
    val ratchetState = text("ratchet_state")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
}
