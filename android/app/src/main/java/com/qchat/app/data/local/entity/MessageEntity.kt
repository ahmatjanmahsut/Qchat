package com.qchat.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 消息实体 - 存储聊天消息
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["senderId"]),
        Index(value = ["localTimestamp"]),
        Index(value = ["serverTimestamp"]),
        Index(value = ["syncStatus"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val senderId: String,
    val content: String, // 加密内容
    val contentType: MessageContentType,
    val status: MessageStatus,
    val isOutgoing: Boolean,
    val replyToMessageId: String? = null, // 回复的消息ID
    val editedAt: Long? = null,           // 编辑时间
    val deletedAt: Long? = null,          // 删除时间
    val localTimestamp: Long = System.currentTimeMillis(), // 本地时间戳
    val serverTimestamp: Long?,           // 服务器时间戳（用于排序和冲突解决）
    val version: Int = 1,                  // 消息版本（用于冲突解决）
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val retryCount: Int = 0                // 重试次数
)

/**
 * 消息内容类型
 */
enum class MessageContentType {
    TEXT,           // 文本
    IMAGE,          // 图片
    VIDEO,          // 视频
    AUDIO,          // 音频
    FILE,           // 文件
    LOCATION,       // 位置
    CONTACT,        // 联系人
    STICKER,        // 贴纸
    SYSTEM          // 系统消息
}

/**
 * 消息状态
 */
enum class MessageStatus {
    SENDING,    // 发送中
    SENT,       // 已发送
    DELIVERED,  // 已送达
    READ,       // 已读
    FAILED,     // 发送失败
    DELETED    // 已删除
}
