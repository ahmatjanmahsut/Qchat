package com.qchat.backend.plugins

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import com.qchat.backend.model.*

/**
 * WebSocket会话管理器
 */
object WebSocketSessionManager {
    private val sessions = mutableMapOf<String, DefaultWebSocketServerSession>()
    private val userSessions = mutableMapOf<String, MutableSet<String>>() // userId -> sessionIds
    private val sessionCounter = AtomicLong(0)

    fun addSession(sessionId: String, userId: String, session: DefaultWebSocketServerSession) {
        sessions[sessionId] = session
        userSessions.getOrPut(userId) { mutableSetOf() }.add(sessionId)
    }

    fun removeSession(sessionId: String, userId: String) {
        sessions.remove(sessionId)
        userSessions[userId]?.remove(sessionId)
        if (userSessions[userId]?.isEmpty() == true) {
            userSessions.remove(userId)
        }
    }

    fun getSession(sessionId: String): DefaultWebSocketServerSession? = sessions[sessionId]

    fun getUserSessions(userId: String): Set<String> = userSessions[userId] ?: emptySet()

    fun isUserOnline(userId: String): Boolean = userSessions.containsKey(userId)

    fun getOnlineUsers(): Set<String> = userSessions.keys.toSet()
}

/**
 * WebSocket消息类型
 */
enum class WSMessageType {
    MESSAGE, ACK, TYPING, READ, ONLINE, OFFLINE, PING, PONG, ERROR, SESSION_REQUEST
}

/**
 * WebSocket消息
 */
data class WSMessage(
    val type: WSMessageType,
    val id: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * WebSocket处理服务
 */
class WebSocketHandler {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun handleMessage(sessionId: String, userId: String, message: Frame.Text) {
        try {
            val wsMessage = json.decodeFromString<WSMessage>(message.readText())
            when (wsMessage.type) {
                WSMessageType.MESSAGE -> handleChatMessage(sessionId, userId, wsMessage)
                WSMessageType.ACK -> handleAck(userId, wsMessage)
                WSMessageType.TYPING -> handleTyping(userId, wsMessage)
                WSMessageType.READ -> handleRead(userId, wsMessage)
                WSMessageType.PING -> handlePing(sessionId)
                WSMessageType.SESSION_REQUEST -> handleSessionRequest(userId, wsMessage)
                else -> {}
            }
        } catch (e: Exception) {
            sendError(sessionId, "Invalid message format")
        }
    }

    private suspend fun handleChatMessage(sessionId: String, senderId: String, message: WSMessage) {
        try {
            val payload = json.decodeFromString<SendMessagePayload>(message.payload)
            val encryptedMessage = EncryptedMessage(
                id = message.id,
                sender = senderId,
                recipient = payload.recipient,
                encryptedContent = payload.encryptedContent,
                encryptionMetadata = payload.encryptionMetadata,
                timestamp = message.timestamp,
                type = payload.type
            )

            // 存储消息
            MessageStore.saveMessage(encryptedMessage)

            // 转发给接收方
            val recipientSessions = WebSocketSessionManager.getUserSessions(payload.recipient)
            recipientSessions.forEach { recipientSessionId ->
                WebSocketSessionManager.getSession(recipientSessionId)?.let { session ->
                    session.send(json.encodeToString(WSMessage(
                        type = WSMessageType.MESSAGE,
                        id = message.id,
                        payload = json.encodeToString(encryptedMessage)
                    )))
                }
            }

            // 发送ACK给发送方
            WebSocketSessionManager.getSession(sessionId)?.send(json.encodeToString(WSMessage(
                type = WSMessageType.ACK,
                id = message.id,
                payload = json.encodeToString(MessageAck(
                    messageId = message.id,
                    status = MessageStatus.SENT
                ))
            )))
        } catch (e: Exception) {
            sendError(sessionId, "Failed to process message")
        }
    }

    private suspend fun handleAck(userId: String, message: WSMessage) {
        try {
            val ack = json.decodeFromString<MessageAck>(message.payload)
            MessageStore.updateMessageStatus(ack.messageId, ack.status)

            // 通知相关用户
            val message = MessageStore.getMessage(ack.messageId)
            message?.let {
                val recipientSessions = WebSocketSessionManager.getUserSessions(it.sender)
                recipientSessions.forEach { sessionId ->
                    WebSocketSessionManager.getSession(sessionId)?.send(json.encodeToString(WSMessage(
                        type = WSMessageType.ACK,
                        id = message.id,
                        payload = json.encodeToString(ack)
                    )))
                }
            }
        } catch (e: Exception) {
            // 处理错误
        }
    }

    private suspend fun handleTyping(userId: String, message: WSMessage) {
        try {
            val payload = json.decodeFromString<TypingPayload>(message.payload)
            val recipientSessions = WebSocketSessionManager.getUserSessions(payload.recipient)

            recipientSessions.forEach { sessionId ->
                WebSocketSessionManager.getSession(sessionId)?.send(json.encodeToString(WSMessage(
                    type = WSMessageType.TYPING,
                    id = message.id,
                    payload = json.encodeToString(TypingIndicator(
                        chatId = payload.chatId,
                        userId = userId,
                        isTyping = payload.isTyping,
                        timestamp = System.currentTimeMillis()
                    ))
                )))
            }
        } catch (e: Exception) {
            // 处理错误
        }
    }

    private suspend fun handleRead(userId: String, message: WSMessage) {
        try {
            val payload = json.decodeFromString<ReadReceiptPayload>(message.payload)

            // 更新消息状态
            payload.messageIds.forEach { messageId ->
                MessageStore.updateMessageStatus(messageId, MessageStatus.READ)
            }

            // 通知发送方
            val firstMessage = MessageStore.getMessage(payload.messageIds.firstOrNull() ?: return)
            firstMessage?.let {
                val senderSessions = WebSocketSessionManager.getUserSessions(it.sender)
                senderSessions.forEach { sessionId ->
                    WebSocketSessionManager.getSession(sessionId)?.send(json.encodeToString(WSMessage(
                        type = WSMessageType.READ,
                        id = message.id,
                        payload = json.encodeToString(payload)
                    )))
                }
            }
        } catch (e: Exception) {
            // 处理错误
        }
    }

    private suspend fun handlePing(sessionId: String) {
        WebSocketSessionManager.getSession(sessionId)?.send(json.encodeToString(WSMessage(
            type = WSMessageType.PONG,
            id = "",
            payload = ""
        )))
    }

    private suspend fun handleSessionRequest(userId: String, message: WSMessage) {
        try {
            val payload = json.decodeFromString<SessionRequestPayload>(message.payload)
            // 返回预密钥包或会话建立信息
            // 实际实现需要从数据库获取用户的预密钥包
        } catch (e: Exception) {
            sendError(sessionId, "Failed to process session request")
        }
    }

    private suspend fun sendError(sessionId: String, error: String) {
        WebSocketSessionManager.getSession(sessionId)?.send(json.encodeToString(WSMessage(
            type = WSMessageType.ERROR,
            id = "",
            payload = """{"code": 500, "message": "$error"}"""
        )))
    }

    fun notifyUserOnline(userId: String) {
        // 通知所有在线用户该用户上线
    }

    fun notifyUserOffline(userId: String) {
        // 通知所有在线用户该用户下线
    }
}

/**
 * 消息存储（内存中，实际应使用数据库）
 */
object MessageStore {
    private val messages = mutableMapOf<String, EncryptedMessage>()

    fun saveMessage(message: EncryptedMessage) {
        messages[message.id] = message
    }

    fun getMessage(messageId: String): EncryptedMessage? = messages[messageId]

    fun getMessagesByChat(chatId: String): List<EncryptedMessage> {
        return messages.values.filter { it.recipient == chatId || it.sender == chatId }
    }

    fun updateMessageStatus(messageId: String, status: MessageStatus) {
        messages[messageId]?.let {
            // 注意：这里需要修改EncryptedMessage以支持状态更新
        }
    }
}

/**
 * 发送消息负载
 */
@kotlinx.serialization.Serializable
data class SendMessagePayload(
    val recipient: String,
    val encryptedContent: String,
    val encryptionMetadata: EncryptionMetadata,
    val type: MessageType,
    val localId: String,
    val replyTo: String? = null
)

/**
 * 输入状态负载
 */
@kotlinx.serialization.Serializable
data class TypingPayload(
    val recipient: String,
    val chatId: String,
    val isTyping: Boolean
)

/**
 * 已读回执负载
 */
@kotlinx.serialization.Serializable
data class ReadReceiptPayload(
    val chatId: String,
    val messageIds: List<String>
)

/**
 * 会话请求负载
 */
@kotlinx.serialization.Serializable
data class SessionRequestPayload(
    val userId: String
)

/**
 * 输入指示器
 */
@kotlinx.serialization.Serializable
data class TypingIndicator(
    val chatId: String,
    val userId: String,
    val isTyping: Boolean,
    val timestamp: Long
)

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(30)
        timeout = Duration.ofSeconds(30)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val webSocketHandler = WebSocketHandler()

    routing {
        webSocket("/ws") {
            val sessionId = "session_${WebSocketSessionManager.sessions.size}"
            val userId = call.request.queryParameters["userId"] ?: "anonymous"

            WebSocketSessionManager.addSession(sessionId, userId, this)

            try {
                // 发送上线通知
                webSocketHandler.notifyUserOnline(userId)

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        webSocketHandler.handleMessage(sessionId, userId, frame)
                    }
                }
            } finally {
                WebSocketSessionManager.removeSession(sessionId, userId)
                webSocketHandler.notifyUserOffline(userId)
            }
        }
    }
}
