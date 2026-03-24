package com.qchat.app.data.sync

import com.qchat.app.data.local.dao.MessageDao
import com.qchat.app.data.local.dao.SessionDao
import com.qchat.app.data.local.entity.MessageEntity
import com.qchat.app.data.local.entity.MessageStatus
import com.qchat.app.data.local.entity.SyncStatus
import com.qchat.app.data.remote.api.MessageApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息历史同步状态
 */
data class MessageHistorySyncState(
    val sessionId: String,
    val isSyncing: Boolean = false,
    val progress: Float = 0f,
    val syncedCount: Int = 0,
    val totalCount: Int = 0,
    val hasMore: Boolean = true,
    val lastSyncTimestamp: Long? = null,
    val error: String? = null
)

/**
 * 消息历史同步器接口
 */
interface MessageHistorySyncer {
    fun getSyncState(sessionId: String): StateFlow<MessageHistorySyncState?>
    suspend fun syncMessageHistory(sessionId: String, limit: Int = 50): Result<Int>
    suspend fun syncOlderMessages(sessionId: String, limit: Int = 50): Result<Int>
    suspend fun syncNewerMessages(sessionId: String, limit: Int = 50): Result<Int>
    suspend fun fullSync(sessionId: String): Result<Int>
    fun cancelSync(sessionId: String)
}

/**
 * 消息历史同步器实现
 */
@Singleton
class MessageHistorySyncerImpl @Inject constructor(
    private val messageApi: MessageApi,
    private val messageDao: MessageDao,
    private val sessionDao: SessionDao,
    private val conflictResolver: MessageConflictResolver
) : MessageHistorySyncer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncStates = MutableStateFlow<Map<String, MessageHistorySyncState>>(emptyMap())

    override fun getSyncState(sessionId: String): StateFlow<MessageHistorySyncState?> =
        MutableStateFlow(syncStates.value[sessionId])

    override suspend fun syncMessageHistory(sessionId: String, limit: Int): Result<Int> {
        updateSyncState(sessionId) { it.copy(isSyncing = true, error = null) }

        return try {
            // 获取本地最后一条消息的时间戳
            val lastLocalMessage = messageDao.getLastMessage(sessionId)
            val sinceTimestamp = lastLocalMessage?.serverTimestamp

            // 从服务器获取新消息
            val response = messageApi.getMessages(sessionId, sinceTimestamp, limit)

            if (response.isSuccessful) {
                val remoteMessages = response.body() ?: emptyList()

                // 处理冲突
                var syncedCount = 0
                for (remoteMessage in remoteMessages) {
                    val localMessage = messageDao.getMessageById(remoteMessage.id)

                    if (localMessage != null) {
                        // 存在本地消息，需要解决冲突
                        val resolvedMessage = conflictResolver.resolveConflict(localMessage, remoteMessage)
                        messageDao.update(resolvedMessage)
                    } else {
                        // 新消息，直接插入
                        val entity = remoteMessage.toEntity()
                        messageDao.insert(entity)
                    }
                    syncedCount++
                }

                // 更新同步状态
                val lastMessage = remoteMessages.lastOrNull()
                updateSyncState(sessionId) {
                    it.copy(
                        isSyncing = false,
                        progress = 1f,
                        syncedCount = it.syncedCount + syncedCount,
                        lastSyncTimestamp = lastMessage?.serverTimestamp ?: it.lastSyncTimestamp,
                        hasMore = remoteMessages.size >= limit
                    )
                }

                Result.success(syncedCount)
            } else {
                val error = "Failed to sync messages: ${response.code()}"
                updateSyncState(sessionId) { it.copy(isSyncing = false, error = error) }
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            updateSyncState(sessionId) { it.copy(isSyncing = false, error = e.message) }
            Result.failure(e)
        }
    }

    override suspend fun syncOlderMessages(sessionId: String, limit: Int): Result<Int> {
        updateSyncState(sessionId) { it.copy(isSyncing = true, error = null) }

        return try {
            // 获取本地最旧消息的时间戳
            val localMessages = messageDao.getMessagesBySessionPaginated(sessionId, 1, 0)
            val oldestTimestamp = localMessages.firstOrNull()?.serverTimestamp

            if (oldestTimestamp == null) {
                // 没有本地消息，执行完整同步
                return syncMessageHistory(sessionId, limit)
            }

            // 从服务器获取更旧的消息
            val response = messageApi.getMessages(sessionId, null, limit)

            if (response.isSuccessful) {
                val remoteMessages = response.body()?.filter {
                    (it.serverTimestamp ?: 0L) < oldestTimestamp
                } ?: emptyList()

                var syncedCount = 0
                for (remoteMessage in remoteMessages) {
                    val localMessage = messageDao.getMessageById(remoteMessage.id)

                    if (localMessage == null) {
                        val entity = remoteMessage.toEntity()
                        messageDao.insert(entity)
                        syncedCount++
                    }
                }

                updateSyncState(sessionId) {
                    it.copy(
                        isSyncing = false,
                        syncedCount = it.syncedCount + syncedCount,
                        hasMore = remoteMessages.size >= limit
                    )
                }

                Result.success(syncedCount)
            } else {
                val error = "Failed to sync older messages: ${response.code()}"
                updateSyncState(sessionId) { it.copy(isSyncing = false, error = error) }
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            updateSyncState(sessionId) { it.copy(isSyncing = false, error = e.message) }
            Result.failure(e)
        }
    }

    override suspend fun syncNewerMessages(sessionId: String, limit: Int): Result<Int> {
        updateSyncState(sessionId) { it.copy(isSyncing = true, error = null) }

        return try {
            // 获取本地最新消息的时间戳
            val lastLocalMessage = messageDao.getLastMessage(sessionId)
            val sinceTimestamp = lastLocalMessage?.serverTimestamp

            val response = messageApi.getMessages(sessionId, sinceTimestamp, limit)

            if (response.isSuccessful) {
                val remoteMessages = response.body()?.filter {
                    it.serverTimestamp != null && sinceTimestamp != null &&
                            it.serverTimestamp > sinceTimestamp
                } ?: emptyList()

                var syncedCount = 0
                for (remoteMessage in remoteMessages) {
                    val localMessage = messageDao.getMessageById(remoteMessage.id)

                    if (localMessage != null) {
                        val resolvedMessage = conflictResolver.resolveConflict(localMessage, remoteMessage)
                        messageDao.update(resolvedMessage)
                    } else {
                        val entity = remoteMessage.toEntity()
                        messageDao.insert(entity)
                    }
                    syncedCount++
                }

                updateSyncState(sessionId) {
                    it.copy(
                        isSyncing = false,
                        syncedCount = it.syncedCount + syncedCount,
                        lastSyncTimestamp = remoteMessages.lastOrNull()?.serverTimestamp
                            ?: it.lastSyncTimestamp
                    )
                }

                Result.success(syncedCount)
            } else {
                val error = "Failed to sync newer messages: ${response.code()}"
                updateSyncState(sessionId) { it.copy(isSyncing = false, error = error) }
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            updateSyncState(sessionId) { it.copy(isSyncing = false, error = e.message) }
            Result.failure(e)
        }
    }

    override suspend fun fullSync(sessionId: String): Result<Int> {
        updateSyncState(sessionId) { it.copy(isSyncing = true, progress = 0f, error = null) }

        var totalSynced = 0

        try {
            // 分批同步消息历史
            var hasMore = true
            while (hasMore) {
                val result = syncMessageHistory(sessionId, BATCH_SIZE)
                result.fold(
                    onSuccess = { count ->
                        totalSynced += count
                        updateSyncState(sessionId) {
                            it.copy(progress = (totalSynced.toFloat() / 1000f).coerceAtMost(1f))
                        }
                        hasMore = count >= BATCH_SIZE
                    },
                    onFailure = { e ->
                        return Result.failure(e)
                    }
                )
            }

            // 同步更新的消息
            syncNewerMessages(sessionId, BATCH_SIZE)

            updateSyncState(sessionId) {
                it.copy(
                    isSyncing = false,
                    progress = 1f,
                    syncedCount = totalSynced,
                    hasMore = false
                )
            }

            Result.success(totalSynced)
        } catch (e: Exception) {
            updateSyncState(sessionId) { it.copy(isSyncing = false, error = e.message) }
            Result.failure(e)
        }
    }

    override fun cancelSync(sessionId: String) {
        updateSyncState(sessionId) { it.copy(isSyncing = false) }
    }

    private fun updateSyncState(sessionId: String, update: (MessageHistorySyncState) -> MessageHistorySyncState) {
        val currentState = syncStates.value[sessionId] ?: MessageHistorySyncState(sessionId = sessionId)
        syncStates.value = syncStates.value + (sessionId to update(currentState))
    }

    private fun com.qchat.app.data.remote.dto.MessageDto.toEntity(): MessageEntity = MessageEntity(
        id = id,
        sessionId = sessionId,
        senderId = senderId,
        content = content,
        contentType = com.qchat.app.data.local.entity.MessageContentType.valueOf(contentType),
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

    companion object {
        private const val BATCH_SIZE = 50
    }
}
