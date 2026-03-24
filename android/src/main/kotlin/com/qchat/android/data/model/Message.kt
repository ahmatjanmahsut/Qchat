package com.qchat.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 消息类型枚举
 */
@Serializable
enum class MessageType {
    TEXT,           // 文本消息
    IMAGE,          // 图片消息
    VIDEO,          // 视频消息
    FILE,           // 文件消息
    VOICE,          // 语音消息
    STICKER,        // 贴纸消息
    SYSTEM          // 系统消息
}

/**
 * 消息状态枚举
 */
@Serializable
enum class MessageStatus {
    SENDING,        // 发送中
    SENT,           // 已发送
    DELIVERED,      // 已送达
    READ,           // 已读
    FAILED          // 发送失败
}

/**
 * 加密元数据
 */
@Serializable
data class EncryptionMetadata(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("message_key_id")
    val messageKeyId: String,
    @SerialName("ratchet_state")
    val ratchetState: String,
    @SerialName("chain_index")
    val chainIndex: Int,
    @SerialName("previous_chain_length")
    val previousChainLength: Int,
    @SerialName("hmac")
    val hmac: String,
    @SerialName("signature")
    val signature: String? = null
)

/**
 * 消息内容基类
 */
@Serializable
sealed class MessageContent {
    abstract val type: MessageType
}

/**
 * 文本消息内容
 */
@Serializable
@SerialName("text")
data class TextContent(
    override val type: MessageType = MessageType.TEXT,
    val text: String
) : MessageContent()

/**
 * 图片消息内容
 */
@Serializable
@SerialName("image")
data class ImageContent(
    override val type: MessageType = MessageType.IMAGE,
    val fileId: String,
    val thumbnailId: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val size: Long,
    val mimeType: String,
    val caption: String? = null
) : MessageContent()

/**
 * 视频消息内容
 */
@Serializable
@SerialName("video")
data class VideoContent(
    override val type: MessageType = MessageType.VIDEO,
    val fileId: String,
    val thumbnailId: String? = null,
    val duration: Int, // 秒
    val width: Int? = null,
    val height: Int? = null,
    val size: Long,
    val mimeType: String,
    val caption: String? = null
) : MessageContent()

/**
 * 文件消息内容
 */
@Serializable
@SerialName("file")
data class FileContent(
    override val type: MessageType = MessageType.FILE,
    val fileId: String,
    val fileName: String,
    val size: Long,
    val mimeType: String,
    val caption: String? = null
) : MessageContent()

/**
 * 语音消息内容
 */
@Serializable
@SerialName("voice")
data class VoiceContent(
    override val type: MessageType = MessageType.VOICE,
    val fileId: String,
    val duration: Int, // 秒
    val size: Long,
    val mimeType: String,
    val isWaveform: Boolean = true,
    val waveform: List<Int>? = null // 波形数据
) : MessageContent()

/**
 * 回复信息
 */
@Serializable
data class ReplyInfo(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("sender_name")
    val senderName: String,
    @SerialName("message_preview")
    val messagePreview: String,
    @SerialName("message_type")
    val messageType: MessageType
)

/**
 * 转发信息
 */
@Serializable
data class ForwardInfo(
    @SerialName("from_chat_id")
    val fromChatId: String,
    @SerialName("original_sender_id")
    val originalSenderId: String,
    @SerialName("original_timestamp")
    val originalTimestamp: Long
)

/**
 * 消息模型
 */
@Serializable
data class Message(
    @SerialName("id")
    val id: String,

    @SerialName("local_id")
    val localId: String? = null, // 本地生成的临时ID，用于发送时引用

    @SerialName("chat_id")
    val chatId: String,

    @SerialName("sender_id")
    val senderId: String,

    @SerialName("sender_name")
    val senderName: String,

    @SerialName("type")
    val type: MessageType,

    @SerialName("content")
    val content: MessageContent,

    @SerialName("timestamp")
    val timestamp: Long,

    @SerialName("status")
    val status: MessageStatus = MessageStatus.SENDING,

    @SerialName("reply")
    val reply: ReplyInfo? = null,

    @SerialName("forward")
    val forward: ForwardInfo? = null,

    @SerialName("encryption")
    val encryption: EncryptionMetadata? = null,

    @SerialName("is_deleted")
    val isDeleted: Boolean = false,

    @SerialName("expire_at")
    val expireAt: Long? = null, // 消失消息过期时间

    @SerialName("reactions")
    val reactions: List<MessageReaction> = emptyList(),

    @SerialName("mentions")
    val mentions: List<String> = emptyList(),

    @SerialName("edited_at")
    val editedAt: Long? = null,

    @SerialName("is_editing")
    val isEditing: Boolean = false
) {
    companion object {
        fun createTextMessage(
            chatId: String,
            senderId: String,
            senderName: String,
            text: String,
            reply: ReplyInfo? = null,
            forward: ForwardInfo? = null
        ): Message {
            return Message(
                id = "",
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                type = MessageType.TEXT,
                content = TextContent(text = text),
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING,
                reply = reply,
                forward = forward
            )
        }
    }
}

/**
 * 消息反应
 */
@Serializable
data class MessageReaction(
    @SerialName("user_id")
    val userId: String,
    @SerialName("emoji")
    val emoji: String,
    @SerialName("timestamp")
    val timestamp: Long
)

/**
 * 加密消息（用于网络传输）
 */
@Serializable
data class EncryptedMessage(
    @SerialName("id")
    val id: String,
    @SerialName("sender")
    val sender: String,
    @SerialName("recipient")
    val recipient: String,
    @SerialName("encrypted_content")
    val encryptedContent: String, // Base64编码的加密内容
    @SerialName("encryption_metadata")
    val encryptionMetadata: EncryptionMetadata,
    @SerialName("timestamp")
    val timestamp: Long,
    @SerialName("type")
    val type: MessageType
)

/**
 * 消息发送请求
 */
@Serializable
data class SendMessageRequest(
    @SerialName("recipient")
    val recipient: String,
    @SerialName("encrypted_content")
    val encryptedContent: String,
    @SerialName("encryption_metadata")
    val encryptionMetadata: EncryptionMetadata,
    @SerialName("type")
    val type: MessageType,
    @SerialName("reply_to")
    val replyTo: String? = null
)

/**
 * 消息接收确认
 */
@Serializable
data class MessageAck(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("status")
    val status: MessageStatus,
    @SerialName("delivered_at")
    val deliveredAt: Long? = null,
    @SerialName("read_at")
    val readAt: Long? = null
)

/**
 * 批量消息状态更新
 */
@Serializable
data class BatchMessageStatusUpdate(
    @SerialName("message_ids")
    val messageIds: List<String>,
    @SerialName("status")
    val status: MessageStatus
)
