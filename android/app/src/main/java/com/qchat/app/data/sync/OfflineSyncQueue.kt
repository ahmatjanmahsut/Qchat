package com.qchat.app.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 离线消息队列项
 */
data class OfflineQueueItem(
    val id: String,
    val entityType: EntityType,
    val entityId: String,
    val operation: SyncOperation,
    val payload: String,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRetryAt: Long? = null
)

enum class EntityType {
    USER,
    SESSION,
    MESSAGE,
    GROUP,
    GROUP_MEMBER,
    CONTACT
}

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * 离线同步队列接口
 */
interface OfflineSyncQueue {
    val queueSize: StateFlow<Int>
    suspend fun enqueue(item: OfflineQueueItem)
    suspend fun enqueueAll(items: List<OfflineQueueItem>)
    suspend fun dequeue(): OfflineQueueItem?
    suspend fun markAsCompleted(itemId: String)
    suspend fun markAsFailed(itemId: String)
    suspend fun getPendingItems(): List<OfflineQueueItem>
    suspend fun clear()
}

/**
 * 离线同步队列实现
 */
@Singleton
class OfflineSyncQueueImpl @Inject constructor() : OfflineSyncQueue {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = MutableStateFlow<List<OfflineQueueItem>>(emptyList())

    override val queueSize: StateFlow<Int> = MutableStateFlow(0)

    private val maxRetries = 3
    private val retryDelayMs = listOf(1000L, 5000L, 15000L) // 重试延迟

    init {
        // 启动队列处理
        scope.launch {
            processQueue()
        }
    }

    override suspend fun enqueue(item: OfflineQueueItem) {
        queue.value = queue.value + item
        queueSize.value = queue.value.size
    }

    override suspend fun enqueueAll(items: List<OfflineQueueItem>) {
        queue.value = queue.value + items
        queueSize.value = queue.value.size
    }

    override suspend fun dequeue(): OfflineQueueItem? {
        val currentQueue = queue.value
        if (currentQueue.isEmpty()) return null

        // 返回最早加入的项目
        val item = currentQueue.minByOrNull { it.createdAt }
        if (item != null) {
            queue.value = currentQueue - item
            queueSize.value = queue.value.size
        }
        return item
    }

    override suspend fun markAsCompleted(itemId: String) {
        queue.value = queue.value.filter { it.id != itemId }
        queueSize.value = queue.value.size
    }

    override suspend fun markAsFailed(itemId: String) {
        val currentQueue = queue.value
        val item = currentQueue.find { it.id == itemId } ?: return

        if (item.retryCount >= maxRetries) {
            // 超过最大重试次数，移出队列
            queue.value = currentQueue.filter { it.id != itemId }
        } else {
            // 更新重试信息
            queue.value = currentQueue.map {
                if (it.id == itemId) {
                    it.copy(
                        retryCount = it.retryCount + 1,
                        lastRetryAt = System.currentTimeMillis()
                    )
                } else it
            }
        }
        queueSize.value = queue.value.size
    }

    override suspend fun getPendingItems(): List<OfflineQueueItem> = queue.value

    override suspend fun clear() {
        queue.value = emptyList()
        queueSize.value = 0
    }

    private suspend fun processQueue() {
        while (true) {
            val item = dequeue() ?: run {
                delay(1000)
                return@launch
            }

            try {
                // 根据实体类型和操作类型执行同步
                val success = executeSync(item)
                if (success) {
                    markAsCompleted(item.id)
                } else {
                    markAsFailed(item.id)
                }
            } catch (e: Exception) {
                markAsFailed(item.id)
            }

            // 重试延迟
            if (item.retryCount < maxRetries) {
                delay(retryDelayMs.getOrElse(item.retryCount) { 15000L })
            }
        }
    }

    private suspend fun executeSync(item: OfflineQueueItem): Boolean {
        // 这里应该调用具体的API进行同步
        // 返回true表示成功，false表示失败
        return true
    }
}
