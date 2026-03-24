package com.qchat.backend

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 消息数据模型
 */
@kotlinx.serialization.Serializable
enum class MessageType {
    TEXT, IMAGE, VIDEO, FILE, VOICE, STICKER, SYSTEM
}

@kotlinx.serialization.Serializable
enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}

@kotlinx.serialization.Serializable
data class EncryptionMetadata(
    val sessionId: String,
    val messageKeyId: String,
    val ratchetState: String,
    val chainIndex: Int,
    val previousChainLength: Int,
    val hmac: String,
    val signature: String? = null
)

@kotlinx.serialization.Serializable
data class Message(
    val id: String = "",
    val localId: String? = null,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val type: MessageType,
    val content: String, // JSON serialized content
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val replyTo: String? = null,
    val encryption: EncryptionMetadata? = null
)

@kotlinx.serialization.Serializable
data class EncryptedMessage(
    val id: String,
    val sender: String,
    val recipient: String,
    val encryptedContent: String,
    val encryptionMetadata: EncryptionMetadata,
    val timestamp: Long,
    val type: MessageType
)

@kotlinx.serialization.Serializable
data class SendMessageRequest(
    val recipient: String,
    val encryptedContent: String,
    val encryptionMetadata: EncryptionMetadata,
    val type: MessageType,
    val replyTo: String? = null
)

@kotlinx.serialization.Serializable
data class MessageAck(
    val messageId: String,
    val status: MessageStatus,
    val deliveredAt: Long? = null,
    val readAt: Long? = null
)

/**
 * 用户数据模型
 */
@kotlinx.serialization.Serializable
data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val publicKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@kotlinx.serialization.Serializable
data class PreKeyBundle(
    val userId: String,
    val deviceId: String,
    val registrationId: Int,
    val preKeyId: Int,
    val preKeyPublic: String,
    val signedPreKeyId: Int,
    val signedPreKeyPublic: String,
    val signedPreKeySignature: String,
    val identityKey: String
)

/**
 * 聊天数据模型
 */
@kotlinx.serialization.Serializable
enum class ChatType {
    PRIVATE, GROUP, CHANNEL
}

@kotlinx.serialization.Serializable
data class Chat(
    val id: String,
    val type: ChatType,
    val name: String,
    val avatarUrl: String? = null,
    val participants: List<String> = emptyList(),
    val lastMessage: Message? = null,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 认证数据模型
 */
@kotlinx.serialization.Serializable
data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val expiresIn: Long
)

@kotlinx.serialization.Serializable
data class RegisterRequest(
    val username: String,
    val displayName: String,
    val password: String
)

@kotlinx.serialization.Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * API响应
 */
@kotlinx.serialization.Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

@kotlinx.serialization.Serializable
data class MessagesResponse(
    val messages: List<Message>,
    val hasMore: Boolean = false
)

@kotlinx.serialization.Serializable
data class ChatsResponse(
    val chats: List<Chat>
)

@kotlinx.serialization.Serializable
data class UserResponse(
    val user: User
)

@kotlinx.serialization.Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
    val expiresIn: Long
)
