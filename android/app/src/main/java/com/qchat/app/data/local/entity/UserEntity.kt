package com.qchat.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户实体 - 本地存储用户信息
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["phoneNumber"], unique = true),
        Index(value = ["username"])
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val phoneNumber: String,
    val username: String?,
    val displayName: String,
    val profilePicture: String?,
    val publicKey: String,
    val lastSeen: Long?, // 时间戳，毫秒
    val isCurrentUser: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

/**
 * 同步状态枚举
 */
enum class SyncStatus {
    SYNCED,      // 已同步
    PENDING,     // 待同步
    FAILED,      // 同步失败
    CONFLICT     // 冲突
}
