package com.qchat.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 会话实体 - 存储会话信息
 */
@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["participantId"]),
        Index(value = ["lastMessageTimestamp"]),
        Index(value = ["updatedAt"])
    ]
)
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val type: SessionType,
    val participantId: String?, // 私聊时为用户ID，群聊时为null
    val groupId: String?,      // 群聊时为群组ID，私聊时为null
    val name: String,           // 会话名称（群名或用户显示名）
    val avatar: String?,        // 头像URL
    val lastMessageId: String?,
    val lastMessageContent: String?, // 摘要
    val lastMessageTimestamp: Long?,  // 最新消息时间戳
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val draftMessage: String? = null, // 草稿消息
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

/**
 * 会话类型
 */
enum class SessionType {
    PRIVATE, // 私聊
    GROUP    // 群聊
}
