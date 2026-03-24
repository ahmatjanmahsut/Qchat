package com.qchat.server.services

import com.qchat.server.database.*
import com.qchat.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.currentTimestamp
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Message service for managing messages
 */
class MessageService {
    private val logger = LoggerFactory.getLogger("MessageService")
    private val cacheService = CacheService()

    /**
     * Send a message
     */
    fun sendMessage(senderId: String, request: SendMessageRequest): Result<MessageResponse> {
        logger.info("Sending message from user: $senderId to conversation: ${request.conversationId}")

        // Verify sender is participant in conversation
        if (!isUserInConversation(request.conversationId, senderId)) {
            return Result.failure(Exception("User is not a participant in this conversation"))
        }

        val messageId = UUID.randomUUID().toString()

        transaction {
            Messages.insert {
                it[Messages.id] = messageId
                it[Messages.conversationId] = request.conversationId
                it[Messages.senderId] = senderId
                it[Messages.type] = MessageType.valueOf(request.type)
                it[Messages.content] = request.content
                it[Messages.mediaUrl] = request.mediaUrl
                it[Messages.mediaType] = request.mediaType
                it[Messages.replyToId] = request.replyToId
                it[Messages.forwardedFromId] = request.forwardedFromId
                it[Messages.createdAt] = currentTimestamp()
                it[Messages.updatedAt] = currentTimestamp()
                it[Messages.isEncrypted] = true
                it[Messages.status] = MessageStatus.SENT
            }

            // Update conversation's last message time
            Conversations.update({ Conversations.id eq request.conversationId }) {
                it[Conversations.lastMessageAt] = currentTimestamp()
                it[Conversations.updatedAt] = currentTimestamp()
            }
        }

        // Increment unread count for other participants
        incrementUnreadForParticipants(request.conversationId, senderId)

        logger.info("Message sent: $messageId")
        return getMessageById(messageId)?.let { Result.success(it) }
            ?: Result.failure(Exception("Failed to retrieve sent message"))
    }

    /**
     * Get message by ID
     */
    fun getMessageById(messageId: String): MessageResponse? {
        return transaction {
            Messages.select { Messages.id eq messageId }
                .firstOrNull()?.let { row ->
                    val sender = Users.select { Users.id eq row[Messages.senderId] }.firstOrNull()
                    messageToResponse(row, sender)
                }
        }
    }

    /**
     * Get messages for a conversation with pagination
     */
    fun getMessages(conversationId: String, userId: String, page: Int = 1, pageSize: Int = 20): MessageListResponse {
        logger.info("Getting messages for conversation: $conversationId, page: $page")

        // Verify user is participant
        if (!isUserInConversation(conversationId, userId)) {
            return MessageListResponse(
                messages = emptyList(),
                page = page,
                pageSize = pageSize,
                totalPages = 0,
                totalCount = 0
            )
        }

        return transaction {
            val totalCount = Messages.select {
                (Messages.conversationId eq conversationId) and (Messages.deletedAt.isNull())
            }.count()

            val totalPages = (totalCount + pageSize - 1) / pageSize
            val offset = (page - 1) * pageSize

            val messages = Messages.select {
                (Messages.conversationId eq conversationId) and (Messages.deletedAt.isNull())
            }
                .orderBy(Messages.createdAt, SortOrder.DESC)
                .limit(pageSize, offset)
                .map { row ->
                    val sender = Users.select { Users.id eq row[Messages.senderId] }.firstOrNull()
                    messageToResponse(row, sender)
                }

            MessageListResponse(
                messages = messages,
                page = page,
                pageSize = pageSize,
                totalPages = totalPages.toInt(),
                totalCount = totalCount
            )
        }
    }

    /**
     * Mark messages as read
     */
    fun markAsRead(conversationId: String, userId: String, messageId: String? = null): Result<Boolean> {
        logger.info("Marking messages as read in conversation: $conversationId")

        // Verify user is participant
        if (!isUserInConversation(conversationId, userId)) {
            return Result.failure(Exception("User is not a participant in this conversation"))
        }

        transaction {
            if (messageId != null) {
                // Mark specific message as read
                Messages.update({ Messages.id eq messageId }) {
                    it[Messages.status] = MessageStatus.READ
                }
            } else {
                // Mark all messages as read
                Messages.update({
                    (Messages.conversationId eq conversationId) and
                            (Messages.senderId neq userId) and
                            (Messages.status neq MessageStatus.READ)
                }) {
                    it[Messages.status] = MessageStatus.READ
                }
            }

            // Update last read time for participant
            when {
                ConversationParticipants.select {
                    (ConversationParticipants.conversationId eq conversationId) and
                            (ConversationParticipants.userId eq userId)
                }.firstOrNull() != null -> {
                    ConversationParticipants.update({
                        (ConversationParticipants.conversationId eq conversationId) and
                                (ConversationParticipants.userId eq userId)
                    }) {
                        it[lastReadAt] = currentTimestamp()
                    }
                }
                GroupMembers.select {
                    (GroupMembers.conversationId eq conversationId) and
                            (GroupMembers.userId eq userId)
                }.firstOrNull() != null -> {
                    // Group members don't have lastReadAt, so we just clear the cache
                }
            }
        }

        // Clear unread count cache
        cacheService.clearUnreadCount(conversationId, userId)

        return Result.success(true)
    }

    /**
     * Delete a message (soft delete)
     */
    fun deleteMessage(messageId: String, userId: String): Result<Boolean> {
        logger.info("Deleting message: $messageId by user: $userId")

        val message = transaction {
            Messages.select { Messages.id eq messageId }.firstOrNull()
        } ?: return Result.failure(Exception("Message not found"))

        // Only sender can delete their own message
        if (message[Messages.senderId] != userId) {
            return Result.failure(Exception("Can only delete your own messages"))
        }

        transaction {
            Messages.update({ Messages.id eq messageId }) {
                it[deletedAt] = currentTimestamp()
            }
        }

        logger.info("Message deleted: $messageId")
        return Result.success(true)
    }

    /**
     * Check if user is in conversation
     */
    private fun isUserInConversation(conversationId: String, userId: String): Boolean {
        return transaction {
            val conv = Conversations.select { Conversations.id eq conversationId }.firstOrNull()
                ?: return@transaction false

            when (conv[Conversations.type]) {
                ConversationType.PRIVATE -> {
                    ConversationParticipants.select {
                        (ConversationParticipants.conversationId eq conversationId) and
                                (ConversationParticipants.userId eq userId) and
                                (ConversationParticipants.isActive eq true)
                    }.firstOrNull() != null
                }
                ConversationType.GROUP -> {
                    GroupMembers.select {
                        (GroupMembers.conversationId eq conversationId) and
                                (GroupMembers.userId eq userId) and
                                (GroupMembers.isActive eq true)
                    }.firstOrNull() != null
                }
            }
        }
    }

    /**
     * Increment unread count for participants
     */
    private fun incrementUnreadForParticipants(conversationId: String, excludeUserId: String) {
        transaction {
            // Get all participants except sender
            val participants = when {
                ConversationParticipants.select {
                    ConversationParticipants.conversationId eq conversationId
                }.firstOrNull() != null -> {
                    ConversationParticipants.select {
                        (ConversationParticipants.conversationId eq conversationId) and
                                (ConversationParticipants.userId neq excludeUserId) and
                                (ConversationParticipants.isActive eq true)
                    }.map { it[ConversationParticipants.userId] }
                }
                else -> {
                    GroupMembers.select {
                        (GroupMembers.conversationId eq conversationId) and
                                (GroupMembers.userId neq excludeUserId) and
                                (GroupMembers.isActive eq true)
                    }.map { it[GroupMembers.userId] }
                }
            }

            participants.forEach { participantId ->
                cacheService.incrementUnreadCount(conversationId, participantId)
            }
        }
    }

    /**
     * Convert message row to response
     */
    private fun messageToResponse(row: org.jetbrains.exposed.sql.Row, sender: org.jetbrains.exposed.sql.Row?): MessageResponse {
        val replyToContent = row[Messages.replyToId]?.let { replyId ->
            transaction {
                Messages.select { Messages.id eq replyId }
                    .firstOrNull()?.get(Messages.content)
            }
        }

        return MessageResponse(
            id = row[Messages.id],
            conversationId = row[Messages.conversationId],
            senderId = row[Messages.senderId],
            senderName = sender?.get(Users.displayName) ?: "",
            senderAvatar = sender?.get(Users.avatarUrl),
            type = row[Messages.type].name,
            content = row[Messages.content],
            mediaUrl = row[Messages.mediaUrl],
            mediaType = row[Messages.mediaType],
            replyToId = row[Messages.replyToId],
            replyToContent = replyToContent,
            forwardedFromId = row[Messages.forwardedFromId],
            createdAt = row[Messages.createdAt].toString(),
            updatedAt = row[Messages.updatedAt].toString(),
            status = row[Messages.status].name,
            isEncrypted = row[Messages.isEncrypted]
        )
    }
}
