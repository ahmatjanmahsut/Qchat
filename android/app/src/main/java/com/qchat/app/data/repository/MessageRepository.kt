package com.qchat.app.data.repository

import com.qchat.app.data.local.dao.MessageDao
import com.qchat.app.data.local.dao.SessionDao
import com.qchat.app.data.local.entity.MessageContentType
import com.qchat.app.data.local.entity.MessageEntity
import com.qchat.app.data.local.entity.MessageStatus
import com.qchat.app.data.local.entity.SessionEntity
import com.qchat.app.data.local.entity.SessionType
import com.qchat.app.data.local.entity.SyncStatus
import com.qchat.app.data.remote.api.MessageApi
import com.qchat.app.data.remote.dto.MessageDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息仓库接口
 */
interface MessageRepository {
    fun getMessagesBySessionFlow(sessionId: String): Flow<List<MessageEntity>>
    suspend fun getMessagesBySession(sessionId: String, limit: Int, offset: Int): List<MessageEntity>
    suspend fun getMessageById(messageId: String): MessageEntity?
    fun getMessageByIdFlow(messageId: String): Flow<MessageEntity?>
    suspend fun getLastMessage(sessionId: String): MessageEntity?
    suspend fun sendMessage(message: MessageEntity): Result<MessageEntity>
    suspend fun updateMessage(message: MessageEntity)
    suspend fun deleteMessage(messageId: String)
    suspend fun markAsRead(sessionId: String)
    suspend fun markMessageAsRead(messageId: String)
    suspend fun syncMessages(sessionId: String, sinceTimestamp: Long?): Result<List<MessageEntity>>
    suspend fun getPendingSyncMessages(): List<MessageEntity>
    suspend fun retryFailedMessages()
    fun getUnreadCountFlow(sessionId: String): Flow<Int>
}

/**
 * 消息仓库实现
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val sessionDao: SessionDao,
    private val messageApi: MessageApi
) : MessageRepository {

    override fun getMessagesBySessionFlow(sessionId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesBySessionFlow(sessionId)

    override suspend fun getMessagesBySession(sessionId: String, limit: Int, offset: Int): List<MessageEntity> =
        messageDao.getMessagesBySessionPaginated(sessionId, limit, offset)

    override suspend fun getMessageById(messageId: String): MessageEntity? =
        messageDao.getMessageById(messageId)

    override fun getMessageByIdFlow(messageId: String): Flow<MessageEntity?> =
        messageDao.getMessageByIdFlow(messageId)

    override suspend fun getLastMessage(sessionId: String): MessageEntity? =
        messageDao.getLastMessage(sessionId)

    override suspend fun sendMessage(message: MessageEntity): Result<MessageEntity> {
        return try {
            // 保存到本地数据库，状态为发送中
            messageDao.insert(message.copy(status = MessageStatus.SENDING))

            // 更新会话的最后消息
            sessionDao.updateLastMessage(
                sessionId = message.sessionId,
                messageId = message.id,
                content = message.content,
                timestamp = message.localTimestamp
            )

            // 发送到服务器
            val response = messageApi.sendMessage(message.toDto())
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val sentMessage = dto.toEntity().copy(
                        status = MessageStatus.SENT,
                        syncStatus = SyncStatus.SYNCED
                    )
                    messageDao.update(sentMessage)
                    Result.success(sentMessage)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                // 更新状态为失败
                messageDao.updateStatus(messageId, MessageStatus.FAILED)
                Result.failure(Exception("Send failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            messageDao.updateStatus(message.id, MessageStatus.FAILED)
            Result.failure(e)
        }
    }

    override suspend fun updateMessage(message: MessageEntity) {
        messageDao.update(message)
    }

    override suspend fun deleteMessage(messageId: String) {
        messageDao.markAsDeleted(messageId)
    }

    override suspend fun markAsRead(sessionId: String) {
        val messages = messageDao.getMessagesBySessionPaginated(sessionId, 100, 0)
        messages.filter { !it.isOutgoing && it.status != MessageStatus.READ }
            .forEach { message ->
                messageDao.updateStatus(message.id, MessageStatus.READ)
            }
        sessionDao.updateUnreadCount(sessionId, 0)
    }

    override suspend fun markMessageAsRead(messageId: String) {
        messageDao.updateStatus(messageId, MessageStatus.READ)
    }

    override suspend fun syncMessages(sessionId: String, sinceTimestamp: Long?): Result<List<MessageEntity>> {
        return try {
            val response = messageApi.getMessages(sessionId, sinceTimestamp)
            if (response.isSuccessful) {
                response.body()?.let { messages ->
                    val entities = messages.map { it.toEntity() }
                    messageDao.insertAll(entities)
                    Result.success(entities)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Sync failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingSyncMessages(): List<MessageEntity> =
        messageDao.getMessagesBySyncStatus(SyncStatus.PENDING)

    override suspend fun retryFailedMessages() {
        val failedMessages = messageDao.getMessagesByStatus(MessageStatus.FAILED)
        failedMessages.forEach { message ->
            if (message.retryCount < 3) {
                messageDao.update(
                    message.copy(
                        status = MessageStatus.SENDING,
                        retryCount = message.retryCount + 1
                    )
                )
                // 重新发送
                sendMessage(message)
            }
        }
    }

    override fun getUnreadCountFlow(sessionId: String): Flow<Int> =
        kotlinx.coroutines.flow.flow {
            emit(messageDao.getUnreadCount(sessionId))
        }
}

/**
 * 消息实体DTO扩展函数
 */
fun MessageEntity.toDto(): com.qchat.app.data.remote.dto.MessageDto = com.qchat.app.data.remote.dto.MessageDto(
    id = id,
    sessionId = sessionId,
    senderId = senderId,
    content = content,
    contentType = contentType.name,
    status = status.name,
    isOutgoing = isOutgoing,
    replyToMessageId = replyToMessageId,
    editedAt = editedAt,
    deletedAt = deletedAt,
    localTimestamp = localTimestamp,
    serverTimestamp = serverTimestamp,
    version = version
)

fun MessageDto.toEntity(): MessageEntity = MessageEntity(
    id = id,
    sessionId = sessionId,
    senderId = senderId,
    content = content,
    contentType = MessageContentType.valueOf(contentType),
    status = MessageStatus.valueOf(status),
    isOutgoing = isOutgoing,
    replyToMessageId = replyToMessageId,
    editedAt = editedAt,
    deletedAt = deletedAt,
    localTimestamp = localTimestamp,
    serverTimestamp = serverTimestamp,
    version = version,
    syncStatus = SyncStatus.SYNCED
)
