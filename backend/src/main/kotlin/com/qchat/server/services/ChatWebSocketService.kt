package com.qchat.server.services

import com.qchat.server.models.*
import com.qchat.server.security.JwtService
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket service for real-time chat
 */
object ChatWebSocketService {
    private val logger = LoggerFactory.getLogger("ChatWebSocketService")
    private val json = Json { ignoreUnknownKeys = true }

    // Active connections by user ID
    private val connections = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

    /**
     * Handle WebSocket connection
     */
    suspend fun handleWebSocket(session: DefaultWebSocketServerSession) {
        val userId = extractUserIdFromSession(session)

        if (userId == null) {
            logger.warn("WebSocket connection rejected: invalid token")
            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid authentication"))
            return
        }

        logger.info("WebSocket connection established for user: $userId")

        // Store connection
        connections[userId] = session

        // Update user online status
        UserService().updateUserOnlineStatus(userId, true)

        try {
            // Handle incoming messages
            session.incoming.consumeEach { frame ->
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        handleMessage(userId, text, session)
                    }
                    is Frame.Close -> {
                        logger.info("WebSocket connection closed by user: $userId")
                    }
                    else -> {
                        // Ignore other frames
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket error for user: $userId", e)
        } finally {
            // Remove connection
            connections.remove(userId)

            // Update user offline status
            UserService().updateUserOnlineStatus(userId, false)

            logger.info("WebSocket connection removed for user: $userId")
        }
    }

    /**
     * Extract user ID from WebSocket session
     */
    private fun extractUserIdFromSession(session: DefaultWebSocketServerSession): String? {
        // Get token from query parameters or headers
        val token = session.call.parameters["token"]
            ?: session.call.request.headers["Authorization"]?.replace("Bearer ", "")

        return token?.let {
            try {
                val decoded = JwtService.decodeToken(it)
                decoded?.subject
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Handle incoming WebSocket message
     */
    private suspend fun handleMessage(userId: String, text: String, session: DefaultWebSocketServerSession) {
        try {
            val wsMessage = json.decodeFromString<WebSocketMessage>(text)

            when (wsMessage.type) {
                "MESSAGE" -> handleChatMessage(userId, wsMessage.payload)
                "TYPING" -> handleTypingIndicator(userId, wsMessage.payload)
                "READ_RECEIPT" -> handleReadReceipt(userId, wsMessage.payload)
                "ONLINE_STATUS" -> handleOnlineStatusRequest(userId, wsMessage.payload)
                else -> logger.warn("Unknown WebSocket message type: ${wsMessage.type}")
            }
        } catch (e: Exception) {
            logger.error("Error handling WebSocket message", e)
        }
    }

    /**
     * Handle chat message
     */
    private suspend fun handleChatMessage(senderId: String, payload: String) {
        try {
            val request = json.decodeFromString<SendMessageRequest>(payload)
            val result = MessageService().sendMessage(senderId, request)

            if (result.isSuccess) {
                val message = result.getOrNull()!!

                // Broadcast message to recipients
                broadcastToConversation(
                    conversationId = message.conversationId,
                    excludeUserId = senderId,
                    message = WebSocketMessage(
                        type = "MESSAGE",
                        payload = json.encodeToString(MessageResponse.serializer(), message)
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Error handling chat message", e)
        }
    }

    /**
     * Handle typing indicator
     */
    private suspend fun handleTypingIndicator(userId: String, payload: String) {
        try {
            val indicator = json.decodeFromString<TypingIndicator>(payload)

            // Broadcast typing indicator to conversation participants
            broadcastToConversation(
                conversationId = indicator.conversationId,
                excludeUserId = userId,
                message = WebSocketMessage(
                    type = "TYPING",
                    payload = json.encodeToString(TypingIndicator.serializer(), indicator)
                )
            )
        } catch (e: Exception) {
            logger.error("Error handling typing indicator", e)
        }
    }

    /**
     * Handle read receipt
     */
    private suspend fun handleReadReceipt(userId: String, payload: String) {
        try {
            val request = json.decodeFromString<MarkReadRequest>(payload)
            MessageService().markAsRead(request.conversationId, userId, request.messageId)

            // Broadcast read receipt
            broadcastToConversation(
                conversationId = request.conversationId,
                excludeUserId = userId,
                message = WebSocketMessage(
                    type = "READ_RECEIPT",
                    payload = payload
                )
            )
        } catch (e: Exception) {
            logger.error("Error handling read receipt", e)
        }
    }

    /**
     * Handle online status request
     */
    private suspend fun handleOnlineStatusRequest(userId: String, payload: String) {
        try {
            val targetUserId = payload
            val isOnline = CacheService().isUserOnline(targetUserId)

            connections[userId]?.send(
                Frame.Text(
                    json.encodeToString(
                        WebSocketMessage.serializer(),
                        WebSocketMessage(
                            type = "ONLINE_STATUS",
                            payload = json.encodeToString(
                                OnlineStatusUpdate.serializer(),
                                OnlineStatusUpdate(targetUserId, isOnline)
                            )
                        )
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Error handling online status request", e)
        }
    }

    /**
     * Broadcast message to all users in a conversation
     */
    private suspend fun broadcastToConversation(
        conversationId: String,
        excludeUserId: String,
        message: WebSocketMessage
    ) {
        val participantIds = getConversationParticipants(conversationId)

        participantIds.forEach { participantId ->
            if (participantId != excludeUserId) {
                connections[participantId]?.let { session ->
                    try {
                        session.send(
                            Frame.Text(json.encodeToString(WebSocketMessage.serializer(), message))
                        )
                    } catch (e: Exception) {
                        logger.error("Error sending message to user: $participantId", e)
                    }
                }
            }
        }
    }

    /**
     * Get conversation participants
     */
    private fun getConversationParticipants(conversationId: String): List<String> {
        return try {
            val service = ConversationService()
            val conversation = service.getConversationById(conversationId, "")
            conversation?.participants?.map { it.id } ?: emptyList()
        } catch (e: Exception) {
            logger.error("Error getting conversation participants", e)
            emptyList()
        }
    }

    /**
     * Send message to specific user
     */
    fun sendToUser(userId: String, message: WebSocketMessage) {
        connections[userId]?.let { session ->
            try {
                kotlinx.coroutines.runBlocking {
                    session.send(Frame.Text(json.encodeToString(WebSocketMessage.serializer(), message)))
                }
            } catch (e: Exception) {
                logger.error("Error sending message to user: $userId", e)
            }
        }
    }

    /**
     * Get active connection count
     */
    fun getActiveConnectionCount(): Int = connections.size

    /**
     * Get online users
     */
    fun getOnlineUsers(): Set<String> = connections.keys.toSet()
}
