package com.qchat.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 群组成员实体 - 存储群组成员关系
 */
@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "userId"],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["joinedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GroupMemberEntity(
    val groupId: String,
    val userId: String,
    val role: GroupRole = GroupRole.MEMBER,
    val nickname: String?, // 在群组中的昵称
    val joinedAt: Long = System.currentTimeMillis(),
    val leftAt: Long? = null, // 离开时间，为null表示还在群中
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
