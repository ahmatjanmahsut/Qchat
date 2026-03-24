package com.qchat.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 联系人实体 - 存储用户联系人
 */
@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["phoneNumber"]),
        Index(value = ["isBlocked"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val userId: String, // 关联的用户ID
    val phoneNumber: String,
    val displayName: String,
    val profilePicture: String?,
    val isBlocked: Boolean = false,
    val isMuted: Boolean = false,
    val isStarred: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
