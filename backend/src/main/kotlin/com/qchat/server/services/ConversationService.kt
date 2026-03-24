package com.qchat.server.services

import com.qchat.server.database.*
import com.qchat.server.models.*
import com.qchat.server.security.EncryptionService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.currentTimestamp
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Conversation service for managing conversations (private and group chats)
 */
class ConversationService {
    private val logger = LoggerFactory.getLogger("ConversationService")
    private val cacheService = CacheService()

    /**
     * Create a new conversation
     */
    fun createConversation(userId: String, request: CreateConversationRequest): Result<ConversationResponse> {
        logger.info("Creating conversation of type: ${request.type} by user: $userId")

        return when (request.type.uppercase()) {
            "PRIVATE" -> createPrivateConversation(userId, request)
            "GROUP" -> createGroupConversation(userId, request)
            else -> Result.failure(Exception("Invalid conversation type"))
        }
    }

    /**
     * Create a private conversation between two users
     */
    private fun createPrivateConversation(userId: String, request: CreateConversationRequest): Result<ConversationResponse> {
        val participantId = request.participantIds?.firstOrNull()
            ?: return Result.failure(Exception("Participant ID required for private conversation"))

        // Check if conversation already exists
        val existingConversation = transaction {
            val conv = Conversations.select {
                (Conversations.type eq ConversationType.PRIVATE) and
                        (Conversations.isActive eq true)
            }.mapNotNull { row ->
                val convId = row[Conversations.id]
                // Check if both users are participants
                val participants = ConversationParticipants.select {
                    ConversationParticipants.conversationId eq convId
                }.map { it[ConversationParticipants.userId] }.toSet()

                if (setOf(userId, participantId) == participants) {
                    convId to row
                } else null
            }.firstOrNull()

            conv?.second
        }

        if (existingConversation != null) {
            return Result.success(conversationToResponse(existingConversation, userId))
        }

        // Create new conversation
        val conversationId = UUID.randomUUID().toString()

        transaction {
            Conversations.insert {
                it[Conversations.id] = conversationId
                it[Conversations.type] = ConversationType.PRIVATE
                it[Conversations.createdAt] = currentTimestamp()
                it[Conversations.updatedAt] = currentTimestamp()
                it[Conversations.isActive] = true
            }

            // Add participants
            ConversationParticipants.insert {
                it[id] = UUID.randomUUID().toString()
                it[conversationId] = conversationId
                it[userId] = userId
                it[joinedAt] = currentTimestamp()
                it[isActive] = true
            }

            ConversationParticipants.insert {
                it[id] = UUID.randomUUID().toString()
                it[conversationId] = conversationId
                it[userId] = participantId
                it[joinedAt] = currentTimestamp()
                it[isActive] = true
            }
        }

        logger.info("Private conversation created: $conversationId")
        return getConversationById(conversationId, userId)?.let { Result.success(it) }
            ?: Result.failure(Exception("Failed to create conversation"))
    }

    /**
     * Create a group conversation
     */
    private fun createGroupConversation(userId: String, request: CreateConversationRequest): Result<ConversationResponse> {
        if (request.name.isNullOrBlank()) {
            return Result.failure(Exception("Group name is required"))
        }

        val conversationId = UUID.randomUUID().toString()
        val encryptionKey = EncryptionService.generateKey()

        transaction {
            Conversations.insert {
                it[Conversations.id] = conversationId
                it[Conversations.type] = ConversationType.GROUP
                it[Conversations.name] = request.name
                it[Conversations.description] = request.description
                it[Conversations.ownerId] = userId
                it[Conversations.encryptionKey] = encryptionKey
                it[Conversations.createdAt] = currentTimestamp()
                it[Conversations.updatedAt] = currentTimestamp()
                it[Conversations.isActive] = true
            }

            // Add creator as owner
            GroupMembers.insert {
                it[id] = UUID.randomUUID().toString()
                it[conversationId] = conversationId
                it[userId] = userId
                it[role] = GroupRole.OWNER
                it[joinedAt] = currentTimestamp()
                it[isActive] = true
            }

            // Add other participants
            request.participantIds?.forEach { participantId ->
                GroupMembers.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[conversationId] = conversationId
                    it[userId] = participantId
                    it[role] = GroupRole.MEMBER
                    it[joinedAt] = currentTimestamp()
                    it[isActive] = true
                }
            }
        }

        logger.info("Group conversation created: $conversationId")
        return getConversationById(conversationId, userId)?.let { Result.success(it) }
            ?: Result.failure(Exception("Failed to create conversation"))
    }

    /**
     * Get conversation by ID
     */
    fun getConversationById(conversationId: String, userId: String): ConversationResponse? {
        return transaction {
            val conv = Conversations.select {
                (Conversations.id eq conversationId) and (Conversations.isActive eq true)
            }.firstOrNull() ?: return@transaction null

            // Verify user is participant
            when (conv[Conversations.type]) {
                ConversationType.PRIVATE -> {
                    val participants = ConversationParticipants.select {
                        ConversationParticipants.conversationId eq conversationId
                    }.map { it[ConversationParticipants.userId] }

                    if (userId !in participants) return@transaction null
                }
                ConversationType.GROUP -> {
                    val member = GroupMembers.select {
                        (GroupMembers.conversationId eq conversationId) and
                                (GroupMembers.userId eq userId) and
                                (GroupMembers.isActive eq true)
                    }.firstOrNull() ?: return@transaction null
                }
            }

            conversationToResponse(conv, userId)
        }
    }

    /**
     * Get user's conversations
     */
    fun getUserConversations(userId: String): List<ConversationResponse> {
        return transaction {
            // Get private conversations
            val privateConvs = ConversationParticipants.select {
                (ConversationParticipants.userId eq userId) and (ConversationParticipants.isActive eq true)
            }.mapNotNull { participantRow ->
                val conversationId = participantRow[ConversationParticipants.conversationId]
                Conversations.select {
                    (Conversations.id eq conversationId) and (Conversations.isActive eq true)
                }.firstOrNull()?.let { convRow ->
                    conversationToResponse(convRow, userId)
                }
            }

            // Get group conversations
            val groupConvs = GroupMembers.select {
                (GroupMembers.userId eq userId) and (GroupMembers.isActive eq true)
            }.mapNotNull { memberRow ->
                val conversationId = memberRow[GroupMembers.conversationId]
                Conversations.select {
                    (Conversations.id eq conversationId) and (Conversations.isActive eq true)
                }.firstOrNull()?.let { convRow ->
                    conversationToResponse(convRow, userId)
                }
            }

            (privateConvs + groupConvs).sortedByDescending { it.updatedAt }
        }
    }

    /**
     * Add participant to conversation
     */
    fun addParticipant(conversationId: String, requesterId: String, userId: String): Result<Boolean> {
        return transaction {
            val conv = Conversations.select { Conversations.id eq conversationId }.firstOrNull()
                ?: return@transaction Result.failure(Exception("Conversation not found"))

            if (conv[Conversations.type] != ConversationType.GROUP) {
                return@transaction Result.failure(Exception("Can only add participants to groups"))
            }

            if (conv[Conversations.ownerId] != requesterId) {
                return@transaction Result.failure(Exception("Only group owner can add participants"))
            }

            GroupMembers.insert {
                it[id] = UUID.randomUUID().toString()
                it[conversationId] = conversationId
                it[userId] = userId
                it[role] = GroupRole.MEMBER
                it[joinedAt] = currentTimestamp()
                it[isActive] = true
            }

            Result.success(true)
        }
    }

    /**
     * Remove participant from conversation
     */
    fun removeParticipant(conversationId: String, requesterId: String, userId: String): Result<Boolean> {
        return transaction {
            val conv = Conversations.select { Conversations.id eq conversationId }.firstOrNull()
                ?: return@transaction Result.failure(Exception("Conversation not found"))

            if (conv[Conversations.type] != ConversationType.GROUP) {
                return@transaction Result.failure(Exception("Can only remove participants from groups"))
            }

            if (conv[Conversations.ownerId] != requesterId && requesterId != userId) {
                return@transaction Result.failure(Exception("Only owner can remove participants or user can leave themselves"))
            }

            GroupMembers.update({
                (GroupMembers.conversationId eq conversationId) and
                        (GroupMembers.userId eq userId)
            }) {
                it[isActive] = false
            }

            Result.success(true)
        }
    }

    /**
     * Convert conversation row to response
     */
    private fun conversationToResponse(conv: org.jetbrains.exposed.sql.Row, userId: String): ConversationResponse {
        val conversationId = conv[Conversations.id]

        // Get participants/members
        val participants = when (conv[Conversations.type]) {
            ConversationType.PRIVATE -> {
                ConversationParticipants.select {
                    ConversationParticipants.conversationId eq conversationId
                }.map { participantRow ->
                    val participantUserId = participantRow[ConversationParticipants.userId]
                    UserInfo(
                        id = participantUserId,
                        username = "", // Will be filled by caller if needed
                        displayName = "",
                        avatarUrl = null,
                        isOnline = cacheService.isUserOnline(participantUserId)
                    )
                }
            }
            ConversationType.GROUP -> {
                GroupMembers.select {
                    (GroupMembers.conversationId eq conversationId) and (GroupMembers.isActive eq true)
                }.map { memberRow ->
                    UserInfo(
                        id = memberRow[GroupMembers.userId],
                        username = "",
                        displayName = "",
                        avatarUrl = null,
                        isOnline = cacheService.isUserOnline(memberRow[GroupMembers.userId])
                    )
                }
            }
        }

        // Get last message
        val lastMessage = getLastMessage(conversationId, userId)

        // Get unread count
        val unreadCount = cacheService.getUnreadCount(conversationId, userId).toInt()

        return ConversationResponse(
            id = conversationId,
            type = conv[Conversations.type].name,
            name = conv[Conversations.name],
            description = conv[Conversations.description],
            avatarUrl = conv[Conversations.avatarUrl],
            ownerId = conv[Conversations.ownerId],
            participants = participants,
            lastMessage = lastMessage,
            unreadCount = unreadCount,
            createdAt = conv[Conversations.createdAt].toString(),
            updatedAt = conv[Conversations.updatedAt].toString()
        )
    }

    private fun getLastMessage(conversationId: String, userId: String): MessageResponse? {
        return transaction {
            Messages.select {
                (Messages.conversationId eq conversationId) and (Messages.deletedAt.isNull())
            }.orderBy(Messages.createdAt, SortOrder.DESC)
                .limit(1)
                .firstOrNull()?.let { row ->
                    MessageResponse(
                        id = row[Messages.id],
                        conversationId = row[Messages.conversationId],
                        senderId = row[Messages.senderId],
                        senderName = "",
                        type = row[Messages.type].name,
                        content = row[Messages.content],
                        mediaUrl = row[Messages.mediaUrl],
                        mediaType = row[Messages.mediaType],
                        replyToId = row[Messages.replyToId],
                        createdAt = row[Messages.createdAt].toString(),
                        updatedAt = row[Messages.updatedAt].toString(),
                        status = row[Messages.status].name,
                        isEncrypted = row[Messages.isEncrypted]
                    )
                }
        }
    }
}
