package com.qchat.server.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Message table - stores all messages
 */
object Messages : Table("messages") {
    val id = varchar("id", 36).primaryKey() // UUID
    val conversationId = varchar("conversation_id", 36).references(Conversations.id)
    val senderId = varchar("sender_id", 36).references(Users.id)
    val type = enumeration("type", MessageType::class).default(MessageType.TEXT)
    val content = text("content") // Encrypted content for E2E
    val mediaUrl = varchar("media_url", 500).nullable()
    val mediaType = varchar("media_type", 50).nullable()
    val replyToId = varchar("reply_to_id", 36).nullable()
    val forwardedFromId = varchar("forwarded_from_id", 36).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    val isEncrypted = bool("is_encrypted").default(true)
    val status = enumeration("status", MessageStatus::class).default(MessageStatus.SENT)
}

enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, FILE, LOCATION, CONTACT, STICKER
}

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}
