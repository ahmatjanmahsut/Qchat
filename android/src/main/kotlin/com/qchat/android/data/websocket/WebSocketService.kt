package com.qchat.android.data.websocket

import com.qchat.android.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket消息类型
 */
@kotlinx.serialization.Serializable
enum class WebSocketMessageType {
    MESSAGE,
    ACK,
    TYPING,
    READ,
    ONLINE,
    OFFLINE,
    PING,
    PONG,
    ERROR,
    SESSION_REQUEST
}

/**
 * WebSocket消息
 */
@kotlinx.serialization.Serializable
data class WebSocketMessage(
    @kotlinx.serialization.SerialName("type")
    val type: WebSocketMessageType,
    @kotlinx.serialization.SerialName("id")
    val id: String,
    @kotlinx.serialization.SerialName("payload")
    val payload: String,
    @kotlinx.serialization.SerialName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * WebSocket连接状态
 */
sealed class WebSocketState {
    object Disconnected : WebSocketState()
    object Connecting : WebSocketState()
    object Connected : WebSocketState()
    data class Error(val message: String) : WebSocketState()
    object Reconnecting : WebSocketState()
}

/**
 * WebSocket服务
 *
 * 处理实时消息传输，包括：
 * - 消息发送和接收
 * - 消息状态更新（送达、已读）
 * - 用户在线状态
 * - 输入状态指示
 * - 心跳检测
 */
@Singleton
class WebSocketService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    private var webSocket: WebSocket? = null
    private var connectionJob: Job? = null
    private var pingJob: Job? = null

    private val _connectionState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<WebSocketMessage>()
    val incomingMessages: SharedFlow<WebSocketMessage> = _incomingMessages.asSharedFlow()

    private val _messageAcks = MutableSharedFlow<MessageAck>()
    val messageAcks: SharedFlow<MessageAck> = _messageAcks.asSharedFlow()

    private val _typingIndicators = MutableSharedFlow<TypingIndicator>()
    val typingIndicators: SharedFlow<TypingIndicator> = _typingIndicators.asSharedFlow()

    private val _onlineStatus = MutableSharedFlow<OnlineStatus>()
    val onlineStatus: SharedFlow<OnlineStatus> = _onlineStatus.asSharedFlow()

    private var authToken: String? = null
    private var serverUrl: String = "wss://api.qchat.com/ws"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val PING_INTERVAL = 30L // 秒
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_DELAY = 5000L // 毫秒
    }

    /**
     * 连接到WebSocket服务器
     */
    fun connect(token: String, url: String? = null) {
        if (_connectionState.value == WebSocketState.Connected ||
            _connectionState.value == WebSocketState.Connecting) {
            return
        }

        authToken = token
        serverUrl = url ?: serverUrl

        _connectionState.value = WebSocketState.Connecting

        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                val request = Request.Builder()
                    .url("$serverUrl?token=$token")
                    .build()

                webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())

                // 启动心跳
                startPing()
            } catch (e: Exception) {
                _connectionState.value = WebSocketState.Error(e.message ?: "Connection failed")
            }
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        connectionJob?.cancel()
        pingJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = WebSocketState.Disconnected
    }

    /**
     * 发送消息
     */
    suspend fun sendMessage(message: Message, encryptedContent: String, encryptionMetadata: EncryptionMetadata): Result<String> {
        if (_connectionState.value != WebSocketState.Connected) {
            return Result.failure(IllegalStateException("WebSocket not connected"))
        }

        val payload = SendMessagePayload(
            recipient = message.chatId,
            encryptedContent = encryptedContent,
            encryptionMetadata = encryptionMetadata,
            type = message.type,
            localId = message.localId ?: message.id,
            replyTo = message.reply?.messageId
        )

        val wsMessage = WebSocketMessage(
            type = WebSocketMessageType.MESSAGE,
            id = message.localId ?: message.id,
            payload = json.encodeToString(payload)
        )

        val messageJson = json.encodeToString(wsMessage)
        val sent = webSocket?.send(messageJson) ?: false

        return if (sent) {
            Result.success(message.localId ?: message.id)
        } else {
            Result.failure(Exception("Failed to send message"))
        }
    }

    /**
     * 发送消息确认
     */
    fun sendMessageAck(ack: MessageAck) {
        val payload = json.encodeToString(ack)
        val wsMessage = WebSocketMessage(
            type = WebSocketMessageType.ACK,
            id = ack.messageId,
            payload = payload
        )
        webSocket?.send(json.encodeToString(wsMessage))
    }

    /**
     * 发送已读回执
     */
    fun sendReadReceipt(chatId: String, messageIds: List<String>) {
        val payload = json.encodeToString(ReadReceiptPayload(chatId, messageIds))
        val wsMessage = WebSocketMessage(
            type = WebSocketMessageType.READ,
            id = generateId(),
            payload = payload
        )
        webSocket?.send(json.encodeToString(wsMessage))
    }

    /**
     * 发送输入状态
     */
    fun sendTypingIndicator(chatId: String, isTyping: Boolean) {
        val payload = json.encodeToString(TypingIndicatorPayload(chatId, isTyping))
        val wsMessage = WebSocketMessage(
            type = WebSocketMessageType.TYPING,
            id = generateId(),
            payload = payload
        )
        webSocket?.send(json.encodeToString(wsMessage))
    }

    /**
     * 请求建立加密会话
     */
    fun requestSession(userId: String) {
        val payload = json.encodeToString(SessionRequestPayload(userId))
        val wsMessage = WebSocketMessage(
            type = WebSocketMessageType.SESSION_REQUEST,
            id = generateId(),
            payload = payload
        )
        webSocket?.send(json.encodeToString(wsMessage))
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connectionState.value = WebSocketState.Connected
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch {
                try {
                    val wsMessage = json.decodeFromString<WebSocketMessage>(text)
                    handleIncomingMessage(wsMessage)
                } catch (e: Exception) {
                    // 忽略无法解析的消息
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.value = WebSocketState.Disconnected
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connectionState.value = WebSocketState.Error(t.message ?: "Connection failed")
            attemptReconnect()
        }
    }

    private suspend fun handleIncomingMessage(wsMessage: WebSocketMessage) {
        when (wsMessage.type) {
            WebSocketMessageType.MESSAGE -> {
                _incomingMessages.emit(wsMessage)
            }
            WebSocketMessageType.ACK -> {
                val ack = json.decodeFromString<MessageAck>(wsMessage.payload)
                _messageAcks.emit(ack)
            }
            WebSocketMessageType.TYPING -> {
                val indicator = json.decodeFromString<TypingIndicator>(wsMessage.payload)
                _typingIndicators.emit(indicator)
            }
            WebSocketMessageType.ONLINE -> {
                val status = json.decodeFromString<OnlineStatus>(wsMessage.payload)
                _onlineStatus.emit(status)
            }
            WebSocketMessageType.OFFLINE -> {
                val status = json.decodeFromString<OnlineStatus>(wsMessage.payload)
                _onlineStatus.emit(status)
            }
            WebSocketMessageType.PONG -> {
                // 心跳响应
            }
            WebSocketMessageType.ERROR -> {
                val error = json.decodeFromString<WebSocketError>(wsMessage.payload)
                // 处理错误
            }
            else -> {}
        }
    }

    private fun startPing() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                delay(PING_INTERVAL * 1000)
                if (_connectionState.value == WebSocketState.Connected) {
                    val pingMessage = WebSocketMessage(
                        type = WebSocketMessageType.PING,
                        id = generateId(),
                        payload = ""
                    )
                    webSocket?.send(json.encodeToString(pingMessage))
                }
            }
        }
    }

    private fun attemptReconnect() {
        if (connectionJob?.isActive == true) return

        connectionJob = scope.launch {
            _connectionState.value = WebSocketState.Reconnecting
            var attempts = 0

            while (attempts < MAX_RECONNECT_ATTEMPTS && isActive) {
                delay(RECONNECT_DELAY * (attempts + 1))
                attempts++

                authToken?.let { token ->
                    val request = Request.Builder()
                        .url("$serverUrl?token=$token")
                        .build()

                    webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
                }
            }

            if (_connectionState.value != WebSocketState.Connected) {
                _connectionState.value = WebSocketState.Error("Max reconnection attempts reached")
            }
        }
    }

    private fun generateId(): String = java.util.UUID.randomUUID().toString()
}

/**
 * 发送消息负载
 */
@kotlinx.serialization.Serializable
data class SendMessagePayload(
    @kotlinx.serialization.SerialName("recipient")
    val recipient: String,
    @kotlinx.serialization.SerialName("encrypted_content")
    val encryptedContent: String,
    @kotlinx.serialization.SerialName("encryption_metadata")
    val encryptionMetadata: EncryptionMetadata,
    @kotlinx.serialization.SerialName("type")
    val type: MessageType,
    @kotlinx.serialization.SerialName("local_id")
    val localId: String,
    @kotlinx.serialization.SerialName("reply_to")
    val replyTo: String? = null
)

/**
 * 已读回执负载
 */
@kotlinx.serialization.Serializable
data class ReadReceiptPayload(
    @kotlinx.serialization.SerialName("chat_id")
    val chatId: String,
    @kotlinx.serialization.SerialName("message_ids")
    val messageIds: List<String>
)

/**
 * 输入状态负载
 */
@kotlinx.serialization.Serializable
data class TypingIndicatorPayload(
    @kotlinx.serialization.SerialName("chat_id")
    val chatId: String,
    @kotlinx.serialization.SerialName("is_typing")
    val isTyping: Boolean
)

/**
 * 会话请求负载
 */
@kotlinx.serialization.Serializable
data class SessionRequestPayload(
    @kotlinx.serialization.SerialName("user_id")
    val userId: String
)

/**
 * 输入指示器
 */
@kotlinx.serialization.Serializable
data class TypingIndicator(
    @kotlinx.serialization.SerialName("chat_id")
    val chatId: String,
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("is_typing")
    val isTyping: Boolean,
    @kotlinx.serialization.SerialName("timestamp")
    val timestamp: Long
)

/**
 * 在线状态
 */
@kotlinx.serialization.Serializable
data class OnlineStatus(
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("is_online")
    val isOnline: Boolean,
    @kotlinx.serialization.SerialName("last_seen")
    val lastSeen: Long? = null
)

/**
 * WebSocket错误
 */
@kotlinx.serialization.Serializable
data class WebSocketError(
    @kotlinx.serialization.SerialName("code")
    val code: Int,
    @kotlinx.serialization.SerialName("message")
    val message: String
)
