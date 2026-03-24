package com.qchat.backend.database.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.InstantColumnType

/**
 * 消息表
 */
object Messages : LongIdTable("messages") {
    val sessionId = long("session_id").index()
    val senderId = long("sender_id").index()
    val content = text("content")
    val contentType = enumeration<MessageContentType>("content_type")
    val status = enumeration<MessageStatus>("status")
    val replyToMessageId = long("reply_to_message_id").nullable()
    val editedAt = registerColumn("edited_at", InstantColumnType()).nullable()
    val deletedAt = registerColumn("deleted_at", InstantColumnType()).nullable()
    val serverTimestamp = registerColumn("server_timestamp", InstantColumnType()).index()
    val version = int("version").default(1)
    val deviceId = varchar("device_id", 100).nullable()
    val createdAt = registerColumn("created_at", InstantColumnType()).clientDefault { java.time.Instant.now() }
    val updatedAt = registerColumn("updated_at", InstantColumnType()).clientDefault { java.time.Instant.now() }
}

class Message(id: EntityID<Long>) : Entity<Long>(id) {
    companion object : EntityClass<Long, Message>(Messages)

    var sessionId by Messages.sessionId
    var senderId by Messages.senderId
    var content by Messages.content
    var contentType by Messages.contentType
    var status by Messages.status
    var replyToMessageId by Messages.replyToMessageId
    var editedAt by Messages.editedAt
    var deletedAt by Messages.deletedAt
    var serverTimestamp by Messages.serverTimestamp
    var version by Messages.version
    var deviceId by Messages.deviceId
    var createdAt by Messages.createdAt
    var updatedAt by Messages.updatedAt
}

enum class MessageContentType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    LOCATION,
    CONTACT,
    STICKER,
    SYSTEM
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED,
    DELETED
}
