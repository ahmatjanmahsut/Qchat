package com.qchat.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 群组实体 - 存储群组信息
 */
@Entity(
    tableName = "groups",
    indices = [
        Index(value = ["createdBy"]),
        Index(value = ["createdAt"]),
        Index(value = ["syncStatus"])
    ]
)
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val avatar: String?,
    val createdBy: String,
    val memberCount: Int = 0,
    val maxMembers: Int = 500,
    val isJoined: Boolean = true,
    val role: GroupRole = GroupRole.MEMBER,
    val lastMessageId: String?,
    val lastMessageTimestamp: Long?,
    val unreadCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

/**
 * 群组角色
 */
enum class GroupRole {
    OWNER,      // 创建者
    ADMIN,      // 管理员
    MEMBER      // 普通成员
}
