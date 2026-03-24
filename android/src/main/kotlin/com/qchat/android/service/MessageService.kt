package com.qchat.android.service

import android.util.Base64
import com.qchat.android.crypto.*
import com.qchat.android.data.local.dao.SessionDao
import com.qchat.android.data.local.entity.SessionEntity
import com.qchat.android.data.model.*
import com.qchat.android.data.remote.MessageApiService
import com.qchat.android.data.remote.ChatApiService
import com.qchat.android.data.repository.MessageRepository
import com.qchat.android.data.repository.ChatRepository
import com.qchat.android.data.repository.SessionRepository
import com.qchat.android.data.websocket.WebSocketService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息服务
 *
 * 处理消息的发送、接收、加密和解密
 */
@Singleton
class MessageService @Inject constructor(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
    private val encryptionManager: EncryptionServiceManager,
    private val keyStoreService: KeyStoreService,
    private val webSocketService: WebSocketService,
    private val messageApiService: MessageApiService,
    private val chatApiService: ChatApiService
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentUserId: String? = null
    private var identityKeyPair: IdentityKeyPair? = null

    private val _messageStatusUpdates = MutableSharedFlow<MessageStatusUpdate>()
    val messageStatusUpdates: SharedFlow<MessageStatusUpdate> = _messageStatusUpdates.asSharedFlow()

    /**
     * 初始化服务
     */
    fun initialize(userId: String, identityKey: IdentityKeyPair) {
        currentUserId = userId
        identityKeyPair = identityKey
    }

    /**
     * 发送消息
     */
    suspend fun sendMessage(
        chatId: String,
        content: MessageContent,
        replyTo: String? = null
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val userId = currentUserId ?: return@withContext Result.failure(IllegalStateException("User not initialized"))
            val identityKey = identityKeyPair ?: return@withContext Result.failure(IllegalStateException("Identity key not initialized"))

            // 获取聊天信息
            val chat = chatRepository.getChatById(chatId)
                ?: return@withContext Result.failure(IllegalStateException("Chat not found"))

            // 创建消息
            val localId = generateLocalId()
            val message = Message(
                id = "", // 服务器将分配ID
                localId = localId,
                chatId = chatId,
                senderId = userId,
                senderName = "", // 将从用户资料填充
                type = content.type,
                content = content,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING,
                reply = replyTo?.let { getReplyInfo(it) }
            )

            // 保存本地消息（发送中状态）
            messageRepository.saveMessage(message)

            // 获取会话状态
            val sessionState = getOrCreateSession(chatId, chat)
                ?: return@withContext Result.failure(IllegalStateException("Session not established"))

            // 加密消息
            val sessionStateEntity = sessionRepository.getSession(sessionState.sessionId)
                ?: return@withContext Result.failure(IllegalStateException("Session not found"))

            val ratchetState = encryptionManager.encryptMessage(
                message = message,
                sessionState = sessionStateEntityToSessionState(sessionStateEntity),
                senderIdentityKey = identityKey
            )

            // 通过WebSocket发送
            val result = webSocketService.sendMessage(
                message = message,
                encryptedContent = ratchetState.encryptedMessage.encryptedContent,
                encryptionMetadata = ratchetState.encryptedMessage.encryptionMetadata
            )

            if (result.isSuccess) {
                // 更新本地消息状态
                messageRepository.updateMessageStatusByLocalId(localId, MessageStatus.SENT)

                // 更新会话状态
                updateSessionState(ratchetState.newRatchetState)

                Result.success(message.copy(id = result.getOrNull() ?: localId))
            } else {
                // 更新本地消息状态为失败
                messageRepository.updateMessageStatusByLocalId(localId, MessageStatus.FAILED)
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to send message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 接收消息
     */
    suspend fun receiveMessage(encryptedMessage: EncryptedMessage): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val identityKeyPair = identityKeyPair
                ?: return@withContext Result.failure(IllegalStateException("Identity key not initialized"))

            // 获取会话状态
            val sessionId = encryptedMessage.encryptionMetadata.sessionId
            val sessionEntity = sessionRepository.getSession(sessionId)
                ?: return@withContext Result.failure(IllegalStateException("Session not found"))

            // 获取发送方公钥
            val senderPublicKey = getSenderPublicKey(encryptedMessage.sender)

            // 解密消息
            val decrypted = encryptionManager.decryptMessage(
                encryptedMessage = encryptedMessage,
                sessionState = sessionEntityToSessionState(sessionEntity),
                senderIdentityKey = senderPublicKey
            )

            if (!decrypted.success) {
                return@withContext Result.failure(Exception(decrypted.error))
            }

            // 构建消息对象
            val message = Message(
                id = encryptedMessage.id,
                chatId = encryptedMessage.recipient,
                senderId = encryptedMessage.sender,
                senderName = "", // 从用户资料获取
                type = encryptedMessage.type,
                content = decrypted.content!!,
                timestamp = encryptedMessage.timestamp,
                status = MessageStatus.DELIVERED,
                encryption = encryptedMessage.encryptionMetadata
            )

            // 保存消息
            messageRepository.saveMessage(message)

            // 更新会话状态
            if (decrypted.newRatchetState != null) {
                updateSessionState(decrypted.newRatchetState)
            }

            // 发送送达确认
            webSocketService.sendMessageAck(
                MessageAck(
                    messageId = encryptedMessage.id,
                    status = MessageStatus.DELIVERED,
                    deliveredAt = System.currentTimeMillis()
                )
            )

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 处理消息状态更新
     */
    suspend fun handleMessageAck(ack: MessageAck) {
        withContext(Dispatchers.IO) {
            when (ack.status) {
                MessageStatus.DELIVERED -> {
                    messageRepository.updateMessageStatus(ack.messageId, MessageStatus.DELIVERED)
                    _messageStatusUpdates.emit(MessageStatusUpdate(ack.messageId, MessageStatus.DELIVERED))
                }
                MessageStatus.READ -> {
                    messageRepository.updateMessageStatus(ack.messageId, MessageStatus.READ)
                    _messageStatusUpdates.emit(MessageStatusUpdate(ack.messageId, MessageStatus.READ))
                }
                else -> {}
            }
        }
    }

    /**
     * 获取聊天消息
     */
    fun getMessages(chatId: String): Flow<List<Message>> {
        return messageRepository.getMessages(chatId)
    }

    /**
     * 加载更多消息
     */
    suspend fun loadMoreMessages(chatId: String, beforeTimestamp: Long, limit: Int = 20): List<Message> {
        return messageRepository.getEarlierMessages(chatId, beforeTimestamp, limit)
    }

    /**
     * 标记消息已读
     */
    suspend fun markAsRead(chatId: String, messageIds: List<String>) {
        messageRepository.getMessageById(messageIds.lastOrNull() ?: return)?.let { message ->
            currentUserId?.let { userId ->
                chatRepository.markAsRead(chatId, message.id, userId)
            }
        }
        webSocketService.sendReadReceipt(chatId, messageIds)
    }

    /**
     * 获取或创建会话
     */
    private suspend fun getOrCreateSession(chatId: String, chat: Chat): SessionState? {
        // 检查现有会话
        val existingSessions = sessionRepository.getSessionsByUser(chatId)
        if (existingSessions.isNotEmpty()) {
            return sessionEntityToSessionState(existingSessions.first())
        }

        // 建立新会话
        return establishSession(chatId)
    }

    /**
     * 建立新会话
     */
    private suspend fun establishSession(remoteUserId: String): SessionState? {
        try {
            // 获取对方预密钥包
            val response = kotlinx.coroutines.withContext(Dispatchers.IO) {
                // 这里应该调用API获取预密钥包
                // 目前暂时返回null
                null
            }

            if (response == null) return null

            val identityKey = identityKeyPair ?: return null

            // 执行X3DH建立会话
            val initialMessage = "Session established".toByteArray(Charsets.UTF_8)
            val result = encryptionManager.establishSession(
                myIdentityKeyPair = identityKey,
                theirPreKeyBundle = response,
                initialMessage = initialMessage
            )

            // 保存会话状态
            val sessionEntity = SessionEntity(
                sessionId = result.sessionId,
                remoteUserId = remoteUserId,
                remoteDeviceId = "", // 暂时为空
                rootKey = Base64.encodeToString(result.sessionState.rootKey, Base64.NO_WRAP),
                chainKey = Base64.encodeToString(result.sessionState.sendingChainKey ?: ByteArray(32), Base64.NO_WRAP),
                sendingChainKey = result.sessionState.sendingChainKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
                receivingChainKey = result.sessionState.receivingChainKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
                sendingRatchetKey = result.sessionState.sendingRatchetKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
                receivingRatchetKey = result.sessionState.receivingRatchetKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
                sendingChainIndex = result.sessionState.sendingChainIndex,
                receivingChainIndex = result.sessionState.receivingChainIndex,
                previousChainLength = result.sessionState.previousChainLength,
                messageNumber = result.sessionState.messageNumber,
                ratchetStateJson = encryptionManager.javaClass.getDeclaredMethod(
                    "serializeState",
                    DoubleRatchet.RatchetState::class.java
                ).invoke(encryptionManager, result.sessionState) as String,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            sessionRepository.saveSession(sessionEntity)

            return SessionState(
                sessionId = result.sessionId,
                remoteUserId = remoteUserId,
                remoteDeviceId = "",
                rootKey = Base64.encodeToString(result.sessionState.rootKey, Base64.NO_WRAP),
                chainKey = Base64.encodeToString(result.sessionState.sendingChainKey ?: ByteArray(32), Base64.NO_WRAP),
                sendingChainKey = result.sessionState.sendingChainKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
                receivingChainKey = result.sessionState.receivingChainKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
                sendingRatchetKey = result.sessionState.sendingRatchetKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
                receivingRatchetKey = result.sessionState.receivingRatchetKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
                sendingChainIndex = result.sessionState.sendingChainIndex,
                receivingChainIndex = result.sessionState.receivingChainIndex,
                previousChainLength = result.sessionState.previousChainLength,
                messageNumber = result.sessionState.messageNumber,
                ratchetState = encryptionManager.javaClass.getDeclaredMethod(
                    "serializeState",
                    DoubleRatchet.RatchetState::class.java
                ).invoke(encryptionManager, result.sessionState) as String,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 更新会话状态
     */
    private suspend fun updateSessionState(ratchetState: DoubleRatchet.RatchetState) {
        val sessionEntity = sessionRepository.getSession(ratchetState.sessionId) ?: return

        val updatedEntity = sessionEntity.copy(
            rootKey = Base64.encodeToString(ratchetState.rootKey, Base64.NO_WRAP),
            sendingChainKey = ratchetState.sendingChainKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
            receivingChainKey = ratchetState.receivingChainKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
            sendingRatchetKey = ratchetState.sendingRatchetKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
            receivingRatchetKey = ratchetState.receivingRatchetKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
            sendingChainIndex = ratchetState.sendingChainIndex,
            receivingChainIndex = ratchetState.receivingChainIndex,
            previousChainLength = ratchetState.previousChainLength,
            messageNumber = ratchetState.messageNumber,
            ratchetStateJson = encryptionManager.javaClass.getDeclaredMethod(
                "serializeState",
                DoubleRatchet.RatchetState::class.java
            ).invoke(encryptionManager, ratchetState) as String,
            updatedAt = System.currentTimeMillis()
        )

        sessionRepository.saveSession(updatedEntity)
    }

    /**
     * 获取回复信息
     */
    private suspend fun getReplyInfo(messageId: String): ReplyInfo? {
        val message = messageRepository.getMessageById(messageId) ?: return null
        return ReplyInfo(
            messageId = message.id,
            senderId = message.senderId,
            senderName = message.senderName,
            messagePreview = getMessagePreview(message.content),
            messageType = message.type
        )
    }

    /**
     * 获取消息预览
     */
    private fun getMessagePreview(content: MessageContent): String {
        return when (content) {
            is TextContent -> content.text.take(100)
            is ImageContent -> "[图片]"
            is VideoContent -> "[视频]"
            is FileContent -> "[文件: ${content.fileName}]"
            is VoiceContent -> "[语音]"
            else -> "[消息]"
        }
    }

    /**
     * 获取发送方公钥
     */
    private fun getSenderPublicKey(senderId: String): org.signal.libsignal.protocol.ecc.ECPublicKey {
        // 从会话或缓存获取发送方公钥
        // 这里应该从已存储的数据中获取
        throw NotImplementedError("需要实现获取发送方公钥")
    }

    /**
     * 会话实体转换为会话状态
     */
    private fun sessionEntityToSessionState(entity: SessionEntity): SessionState {
        return SessionState(
            sessionId = entity.sessionId,
            remoteUserId = entity.remoteUserId,
            remoteDeviceId = entity.remoteDeviceId,
            rootKey = entity.rootKey,
            chainKey = entity.chainKey,
            sendingChainKey = entity.sendingChainKey,
            receivingChainKey = entity.receivingChainKey,
            sendingRatchetKey = entity.sendingRatchetKey,
            receivingRatchetKey = entity.receivingRatchetKey,
            sendingChainIndex = entity.sendingChainIndex,
            receivingChainIndex = entity.receivingChainIndex,
            previousChainLength = entity.previousChainLength,
            messageNumber = entity.messageNumber,
            ratchetState = entity.ratchetStateJson,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun generateLocalId(): String = "local_${System.currentTimeMillis()}_${(0..999999).random()}"
}

/**
 * 消息状态更新
 */
data class MessageStatusUpdate(
    val messageId: String,
    val status: MessageStatus
)
