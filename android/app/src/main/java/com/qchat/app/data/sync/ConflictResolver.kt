package com.qchat.app.data.sync

import com.qchat.app.data.local.entity.MessageEntity
import com.qchat.app.data.local.entity.MessageStatus
import com.qchat.app.data.local.entity.SyncStatus
import com.qchat.app.data.remote.dto.MessageDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 冲突类型
 */
enum class ConflictType {
    NONE,           // 无冲突
    CONTENT,        // 内容冲突
    STATUS,         // 状态冲突
    DELETED,        // 删除冲突（本地已删除，远程有更新）
    VERSION         // 版本冲突
}

/**
 * 冲突信息
 */
data class ConflictInfo(
    val localEntity: Any,
    val remoteEntity: Any,
    val conflictType: ConflictType,
    val localTimestamp: Long,
    val remoteTimestamp: Long
)

/**
 * 冲突解决结果
 */
sealed class ConflictResolution {
    data class KeepLocal(val entity: Any) : ConflictResolution()
    data class KeepRemote(val entity: Any) : ConflictResolution()
    data class Merge(val entity: Any) : ConflictResolution()
    data class DiscardBoth(val entityId: String) : ConflictResolution()
}

/**
 * 冲突解决策略接口
 */
interface ConflictResolver {
    suspend fun resolveMessageConflict(local: MessageEntity, remote: MessageDto): ConflictResolution
    suspend fun detectConflict(local: MessageEntity, remote: MessageDto): ConflictInfo?
}

/**
 * 最后写入获胜（Last Write Wins）冲突解决策略
 */
@Singleton
class LastWriteWinsConflictResolver @Inject constructor() : ConflictResolver {

    override suspend fun resolveMessageConflict(
        local: MessageEntity,
        remote: MessageDto
    ): ConflictResolution {
        val conflictInfo = detectConflict(local, remote) ?: return ConflictResolution.KeepRemote(remote)

        return when (conflictInfo.conflictType) {
            ConflictType.NONE -> ConflictResolution.KeepRemote(remote)
            ConflictType.CONTENT -> {
                // 内容冲突 - 使用服务器时间戳决定
                if (conflictInfo.remoteTimestamp > conflictInfo.localTimestamp) {
                    ConflictResolution.KeepRemote(remote)
                } else {
                    ConflictResolution.KeepLocal(local)
                }
            }
            ConflictType.STATUS -> {
                // 状态冲突 - 优先保留更高的状态（已读 > 已送达 > 已发送）
                val localStatus = MessageStatus.valueOf(local.status.name)
                val remoteStatus = MessageStatus.valueOf(remote.status)
                if (remoteStatus.ordinal > localStatus.ordinal) {
                    ConflictResolution.KeepRemote(remote)
                } else {
                    ConflictResolution.KeepLocal(local)
                }
            }
            ConflictType.DELETED -> {
                // 删除冲突 - 以删除为准
                ConflictResolution.DiscardBoth(local.id)
            }
            ConflictType.VERSION -> {
                // 版本冲突 - 使用更高版本
                if (remote.version > local.version) {
                    ConflictResolution.KeepRemote(remote)
                } else {
                    ConflictResolution.KeepLocal(local)
                }
            }
        }
    }

    override suspend fun detectConflict(
        local: MessageEntity,
        remote: MessageDto
    ): ConflictInfo? {
        // 如果本地消息已删除
        if (local.deletedAt != null && remote.deletedAt == null) {
            return ConflictInfo(
                localEntity = local,
                remoteEntity = remote,
                conflictType = ConflictType.DELETED,
                localTimestamp = local.deletedAt,
                remoteTimestamp = remote.serverTimestamp ?: 0L
            )
        }

        // 如果远程消息已删除
        if (local.deletedAt == null && remote.deletedAt != null) {
            return ConflictInfo(
                localEntity = local,
                remoteEntity = remote,
                conflictType = ConflictType.DELETED,
                localTimestamp = local.localTimestamp,
                remoteTimestamp = remote.deletedAt
            )
        }

        // 检查内容是否冲突
        if (local.content != remote.content) {
            // 如果本地是待发送状态（还在发送中），以远程为准
            if (local.status == MessageStatus.SENDING) {
                return ConflictInfo(
                    localEntity = local,
                    remoteEntity = remote,
                    conflictType = ConflictType.CONTENT,
                    localTimestamp = local.localTimestamp,
                    remoteTimestamp = remote.serverTimestamp ?: 0L
                )
            }

            // 如果服务器时间更新，内容可能已被其他设备修改
            val localTime = local.serverTimestamp ?: local.localTimestamp
            val remoteTime = remote.serverTimestamp ?: 0L

            if (kotlin.math.abs(localTime - remoteTime) > CONFLICT_THRESHOLD_MS) {
                return ConflictInfo(
                    localEntity = local,
                    remoteEntity = remote,
                    conflictType = ConflictType.CONTENT,
                    localTimestamp = localTime,
                    remoteTimestamp = remoteTime
                )
            }
        }

        // 检查版本冲突
        if (remote.version > local.version) {
            return ConflictInfo(
                localEntity = local,
                remoteEntity = remote,
                conflictType = ConflictType.VERSION,
                localTimestamp = local.localTimestamp,
                remoteTimestamp = remote.serverTimestamp ?: 0L
            )
        }

        // 检查状态冲突
        if (local.status != MessageStatus.valueOf(remote.status)) {
            return ConflictInfo(
                localEntity = local,
                remoteEntity = remote,
                conflictType = ConflictType.STATUS,
                localTimestamp = local.localTimestamp,
                remoteTimestamp = remote.serverTimestamp ?: 0L
            )
        }

        return null
    }

    companion object {
        // 5秒内的时间差视为同步，不算冲突
        private const val CONFLICT_THRESHOLD_MS = 5000L
    }
}

/**
 * 消息冲突解决器
 */
@Singleton
class MessageConflictResolver @Inject constructor(
    private val conflictResolver: ConflictResolver
) {

    suspend fun resolveConflict(local: MessageEntity, remote: MessageDto): MessageEntity {
        val resolution = conflictResolver.resolveMessageConflict(local, remote)

        return when (resolution) {
            is ConflictResolution.KeepLocal -> local.copy(syncStatus = SyncStatus.SYNCED)
            is ConflictResolution.KeepRemote -> remote.toEntity().copy(syncStatus = SyncStatus.SYNCED)
            is ConflictResolution.Merge -> {
                // 合并策略：保留本地内容，更新状态和版本
                local.copy(
                    status = MessageStatus.valueOf(remote.status),
                    version = remote.version,
                    serverTimestamp = remote.serverTimestamp,
                    syncStatus = SyncStatus.SYNCED
                )
            }
            is ConflictResolution.DiscardBoth -> {
                // 标记为已删除
                local.copy(
                    deletedAt = System.currentTimeMillis(),
                    status = MessageStatus.DELETED,
                    syncStatus = SyncStatus.SYNCED
                )
            }
        }
    }

    private fun MessageDto.toEntity(): MessageEntity = MessageEntity(
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
        syncStatus = SyncStatus.SYNCED,
        retryCount = 0
    )
}
