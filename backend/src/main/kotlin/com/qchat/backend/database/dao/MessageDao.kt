package com.qchat.backend.database.dao

import com.qchat.backend.database.entity.Message
import com.qchat.backend.database.entity.MessageContentType
import com.qchat.backend.database.entity.MessageStatus
import com.qchat.backend.database.entity.Messages
import com.qchat.backend.database.entity.Session
import com.qchat.backend.database.entity.Sessions
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.update
import java.time.Instant

/**
 * 消息数据访问对象
 */
class MessageDao {

    fun findById(id: Long): Message? = Message.findById(id)

    fun findByIds(ids: List<Long>): List<Message> =
        Message.find { Messages.id inList ids }.toList()

    fun findBySessionId(sessionId: Long, limit: Int = 50, offset: Int = 0): List<Message> =
        Message.find { Messages.sessionId eq sessionId }
            .orderBy(Messages.serverTimestamp to SortOrder.Desc)
            .limit(limit, offset.toLong())
            .toList()
            .reversed() // 按时间正序返回

    fun findBySessionIdSince(sessionId: Long, sinceTimestamp: Instant, limit: Int = 100): List<Message> =
        Message.find { Messages.sessionId eq sessionId and (Messages.serverTimestamp greater sinceTimestamp) }
            .orderBy(Messages.serverTimestamp to SortOrder.Asc)
            .limit(limit)
            .toList()

    fun findBySenderId(senderId: Long, limit: Int = 50): List<Message> =
        Message.find { Messages.senderId eq senderId }
            .orderBy(Messages.createdAt to SortOrder.Desc)
            .limit(limit)
            .toList()

    fun create(
        sessionId: Long,
        senderId: Long,
        content: String,
        contentType: MessageContentType = MessageContentType.TEXT,
        replyToMessageId: Long? = null,
        deviceId: String? = null
    ): Message = Message.new {
        this.sessionId = sessionId
        this.senderId = senderId
        this.content = content
        this.contentType = contentType
        this.status = MessageStatus.SENT
        this.replyToMessageId = replyToMessageId
        this.serverTimestamp = Instant.now()
        this.version = 1
        this.deviceId = deviceId
    }

    fun updateContent(messageId: Long, newContent: String): Message? {
        val message = findById(messageId) ?: return null
        transaction(message) {
            message.content = newContent
            message.editedAt = Instant.now()
            message.version = message.version + 1
            message.updatedAt = Instant.now()
        }
        return message
    }

    fun updateStatus(messageId: Long, status: MessageStatus): Boolean {
        val updated = Messages.update({ Messages.id eq messageId }) {
            it[Messages.status] = status
        }
        return updated > 0
    }

    fun markAsDeleted(messageId: Long): Boolean {
        val updated = Messages.update({ Messages.id eq messageId }) {
            it[Messages.deletedAt] = Instant.now()
            it[Messages.status] = MessageStatus.DELETED
        }
        return updated > 0
    }

    fun markAsReadBySession(sessionId: Long, readerId: Long) {
        Messages.update({
            Messages.sessionId eq sessionId and
                (Messages.senderId neq readerId) and
                (Messages.status neq MessageStatus.READ)
        }) {
            it[Messages.status] = MessageStatus.READ
        }
    }

    fun delete(messageId: Long): Boolean {
        val message = findById(messageId) ?: return false
        transaction(message) { message.delete() }
        return true
    }

    fun getLastMessage(sessionId: Long): Message? =
        Message.find { Messages.sessionId eq sessionId }
            .orderBy(Messages.serverTimestamp to SortOrder.Desc)
            .firstOrNull()

    fun getUnreadCount(sessionId: Long, userId: Long): Int =
        Message.find {
            Messages.sessionId eq sessionId and
                (Messages.senderId neq userId) and
                (Messages.status neq MessageStatus.READ)
        }.count().toInt()

    fun getMessageCount(sessionId: Long): Int =
        Message.find { Messages.sessionId eq sessionId }.count().toInt()
}
