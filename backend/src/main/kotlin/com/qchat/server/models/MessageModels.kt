package com.qchat.server.models

import kotlinx.serialization.Serializable

/**
 * Message send request
 */
@Serializable
data class SendMessageRequest(
    val conversationId: String,
    val type: String = "TEXT",
    val content: String,
    val replyToId: String? = null,
    val forwardedFromId: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null
)

/**
 * Message response
 */
@Serializable
data class MessageResponse(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String? = null,
    val type: String,
    val content: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val forwardedFromId: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val status: String,
    val isEncrypted: Boolean
)

/**
 * Message list response with pagination
 */
@Serializable
data class MessageListResponse(
    val messages: List<MessageResponse>,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalCount: Long
)

/**
 * Mark messages as read request
 */
@Serializable
data class MarkReadRequest(
    val conversationId: String,
    val messageId: String? = null // If null, mark all as read
)

/**
 * WebSocket message for real-time chat
 */
@Serializable
data class WebSocketMessage(
    val type: String, // "MESSAGE", "TYPING", "ONLINE_STATUS", "READ_RECEIPT"
    val payload: String, // JSON payload
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Typing indicator
 */
@Serializable
data class TypingIndicator(
    val conversationId: String,
    val userId: String,
    val isTyping: Boolean
)

/**
 * Online status update
 */
@Serializable
data class OnlineStatusUpdate(
    val userId: String,
    val isOnline: Boolean
)
