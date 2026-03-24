package com.qchat.app.data.sync

import com.qchat.app.data.local.entity.MessageEntity
import com.qchat.app.data.local.entity.SyncStatus
import com.qchat.app.data.remote.api.MessageApi
import com.qchat.app.data.remote.api.SessionApi
import com.qchat.app.data.remote.api.UserApi
import com.qchat.app.data.remote.api.GroupApi
import com.qchat.app.data.remote.api.ContactApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步状态
 */
data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val currentOperation: String? = null,
    val errors: List<SyncError> = emptyList()
)

data class SyncError(
    val entityType: String,
    val entityId: String,
    val error: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 同步结果
 */
sealed class SyncResult {
    data class Success(val syncedCount: Int) : SyncResult()
    data class PartialSuccess(val syncedCount: Int, val failedCount: Int) : SyncResult()
    data class Failure(val error: String) : SyncResult()
}

/**
 * 数据同步服务接口
 */
interface SyncService {
    val syncState: StateFlow<SyncState>
    suspend fun syncAll(): SyncResult
    suspend fun syncMessages(sessionId: String? = null): SyncResult
    suspend fun syncUsers(): SyncResult
    suspend fun syncSessions(): SyncResult
    suspend fun syncGroups(): SyncResult
    suspend fun syncContacts(): SyncResult
    suspend fun syncPendingChanges(): SyncResult
    suspend fun retryFailedSync()
    fun startPeriodicSync(intervalMs: Long)
    fun stopPeriodicSync()
}

/**
 * 数据同步服务实现
 */
@Singleton
class SyncServiceImpl @Inject constructor(
    private val messageApi: MessageApi,
    private val sessionApi: SessionApi,
    private val userApi: UserApi,
    private val groupApi: GroupApi,
    private val contactApi: ContactApi
) : SyncService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow(SyncState())
    override val syncState: StateFlow<SyncState> = _syncState

    private var periodicSyncJob: kotlinx.coroutines.Job? = null

    override suspend fun syncAll(): SyncResult {
        if (_syncState.value.isSyncing) {
            return SyncResult.Failure("Sync already in progress")
        }

        updateState { it.copy(isSyncing = true, currentOperation = "Syncing all data") }

        var successCount = 0
        var failCount = 0

        try {
            // 按顺序同步各类型数据
            syncUsers().let { result ->
                when (result) {
                    is SyncResult.Success -> successCount++
                    is SyncResult.PartialSuccess -> { successCount++; failCount++ }
                    is SyncResult.Failure -> failCount++
                }
            }

            syncSessions().let { result ->
                when (result) {
                    is SyncResult.Success -> successCount++
                    is SyncResult.PartialSuccess -> { successCount++; failCount++ }
                    is SyncResult.Failure -> failCount++
                }
            }

            syncMessages().let { result ->
                when (result) {
                    is SyncResult.Success -> successCount++
                    is SyncResult.PartialSuccess -> { successCount++; failCount++ }
                    is SyncResult.Failure -> failCount++
                }
            }

            syncGroups().let { result ->
                when (result) {
                    is SyncResult.Success -> successCount++
                    is SyncResult.PartialSuccess -> { successCount++; failCount++ }
                    is SyncResult.Failure -> failCount++
                }
            }

            syncContacts().let { result ->
                when (result) {
                    is SyncResult.Success -> successCount++
                    is SyncResult.PartialSuccess -> { successCount++; failCount++ }
                    is SyncResult.Failure -> failCount++
                }
            }

            syncPendingChanges().let { result ->
                when (result) {
                    is SyncResult.Success -> successCount++
                    is SyncResult.PartialSuccess -> { successCount++; failCount++ }
                    is SyncResult.Failure -> failCount++
                }
            }

            updateState {
                it.copy(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis(),
                    currentOperation = null
                )
            }

            return if (failCount == 0) {
                SyncResult.Success(successCount)
            } else {
                SyncResult.PartialSuccess(successCount, failCount)
            }
        } catch (e: Exception) {
            updateState {
                it.copy(
                    isSyncing = false,
                    currentOperation = null,
                    errors = it.errors + SyncError("SYNC", "ALL", e.message ?: "Unknown error")
                )
            }
            return SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    override suspend fun syncMessages(sessionId: String?): SyncResult {
        updateState { it.copy(currentOperation = "Syncing messages") }

        return try {
            val response = messageApi.getMessages(sessionId ?: "", null)
            if (response.isSuccessful) {
                val count = response.body()?.size ?: 0
                updateState { it.copy(currentOperation = null, pendingCount = 0) }
                SyncResult.Success(count)
            } else {
                SyncResult.Failure("Failed to sync messages: ${response.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "Failed to sync messages")
        }
    }

    override suspend fun syncUsers(): SyncResult {
        updateState { it.copy(currentOperation = "Syncing users") }

        return try {
            val response = userApi.getContacts()
            if (response.isSuccessful) {
                val count = response.body()?.size ?: 0
                updateState { it.copy(currentOperation = null) }
                SyncResult.Success(count)
            } else {
                SyncResult.Failure("Failed to sync users: ${response.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "Failed to sync users")
        }
    }

    override suspend fun syncSessions(): SyncResult {
        updateState { it.copy(currentOperation = "Syncing sessions") }

        return try {
            val response = sessionApi.getSessions()
            if (response.isSuccessful) {
                val count = response.body()?.size ?: 0
                updateState { it.copy(currentOperation = null) }
                SyncResult.Success(count)
            } else {
                SyncResult.Failure("Failed to sync sessions: ${response.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "Failed to sync sessions")
        }
    }

    override suspend fun syncGroups(): SyncResult {
        updateState { it.copy(currentOperation = "Syncing groups") }

        return try {
            val response = groupApi.getGroups()
            if (response.isSuccessful) {
                val count = response.body()?.size ?: 0
                updateState { it.copy(currentOperation = null) }
                SyncResult.Success(count)
            } else {
                SyncResult.Failure("Failed to sync groups: ${response.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "Failed to sync groups")
        }
    }

    override suspend fun syncContacts(): SyncResult {
        updateState { it.copy(currentOperation = "Syncing contacts") }

        return try {
            val response = contactApi.getContacts()
            if (response.isSuccessful) {
                val count = response.body()?.size ?: 0
                updateState { it.copy(currentOperation = null) }
                SyncResult.Success(count)
            } else {
                SyncResult.Failure("Failed to sync contacts: ${response.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "Failed to sync contacts")
        }
    }

    override suspend fun syncPendingChanges(): SyncResult {
        updateState { it.copy(currentOperation = "Syncing pending changes") }

        // 实现待同步变更的上传
        var syncedCount = 0
        var failedCount = 0

        updateState { it.copy(currentOperation = null) }
        return SyncResult.Success(syncedCount)
    }

    override suspend fun retryFailedSync() {
        updateState { it.copy(errors = emptyList(), failedCount = 0) }
        syncAll()
    }

    override fun startPeriodicSync(intervalMs: Long) {
        periodicSyncJob?.cancel()
        periodicSyncJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(intervalMs)
                syncAll()
            }
        }
    }

    override fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
    }

    private fun updateState(update: (SyncState) -> SyncState) {
        _syncState.value = update(_syncState.value)
    }
}
