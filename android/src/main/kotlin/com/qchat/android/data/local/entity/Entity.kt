package com.qchat.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.qchat.android.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 消息实体
 */
@Entity(tableName = "messages")
@TypeConverters(Converters::class)
data class MessageEntity(
    @PrimaryKey
    val id: String,

    val localId: String? = null,

    val chatId: String,

    val senderId: String,

    val senderName: String,

    val type: MessageType,

    val contentJson: String,

    val timestamp: Long,

    val status: MessageStatus = MessageStatus.SENDING,

    val replyId: String? = null,

    val replySenderId: String? = null,

    val replySenderName: String? = null,

    val replyPreview: String? = null,

    val replyType: MessageType? = null,

    val forwardFromChatId: String? = null,

    val forwardOriginalSenderId: String? = null,

    val forwardOriginalTimestamp: Long? = null,

    val encryptionSessionId: String? = null,

    val encryptionMessageKeyId: String? = null,

    val encryptionHmac: String? = null,

    val encryptionSignature: String? = null,

    val isDeleted: Boolean = false,

    val expireAt: Long? = null,

    val editedAt: Long? = null,

    val reactionsJson: String = "[]",

    val mentionsJson: String = "[]"
) {
    fun toMessage(): Message {
        val json = Json { ignoreUnknownKeys = true }
        val content = json.decodeFromString<MessageContent>(contentJson)

        return Message(
            id = id,
            localId = localId,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            type = type,
            content = content,
            timestamp = timestamp,
            status = status,
            reply = if (replyId != null) {
                ReplyInfo(
                    messageId = replyId!!,
                    senderId = replySenderId ?: "",
                    senderName = replySenderName ?: "",
                    messagePreview = replyPreview ?: "",
                    messageType = replyType ?: MessageType.TEXT
                )
            } else null,
            forward = if (forwardFromChatId != null) {
                ForwardInfo(
                    fromChatId = forwardFromChatId!!,
                    originalSenderId = forwardOriginalSenderId ?: "",
                    originalTimestamp = forwardOriginalTimestamp ?: 0L
                )
            } else null,
            encryption = if (encryptionSessionId != null) {
                EncryptionMetadata(
                    sessionId = encryptionSessionId!!,
                    messageKeyId = encryptionMessageKeyId ?: "",
                    ratchetState = "",
                    chainIndex = 0,
                    previousChainLength = 0,
                    hmac = encryptionHmac ?: "",
                    signature = encryptionSignature
                )
            } else null,
            isDeleted = isDeleted,
            expireAt = expireAt,
            editedAt = editedAt,
            reactions = json.decodeFromString(reactionsJson),
            mentions = json.decodeFromString(mentionsJson)
        )
    }

    companion object {
        fun fromMessage(message: Message): MessageEntity {
            val json = Json { ignoreUnknownKeys = true }

            return MessageEntity(
                id = message.id,
                localId = message.localId,
                chatId = message.chatId,
                senderId = message.senderId,
                senderName = message.senderName,
                type = message.type,
                contentJson = json.encodeToString(message.content),
                timestamp = message.timestamp,
                status = message.status,
                replyId = message.reply?.messageId,
                replySenderId = message.reply?.senderId,
                replySenderName = message.reply?.senderName,
                replyPreview = message.reply?.messagePreview,
                replyType = message.reply?.messageType,
                forwardFromChatId = message.forward?.fromChatId,
                forwardOriginalSenderId = message.forward?.originalSenderId,
                forwardOriginalTimestamp = message.forward?.originalTimestamp,
                encryptionSessionId = message.encryption?.sessionId,
                encryptionMessageKeyId = message.encryption?.messageKeyId,
                encryptionHmac = message.encryption?.hmac,
                encryptionSignature = message.encryption?.signature,
                isDeleted = message.isDeleted,
                expireAt = message.expireAt,
                editedAt = message.editedAt,
                reactionsJson = json.encodeToString(message.reactions),
                mentionsJson = json.encodeToString(message.mentions)
            )
        }
    }
}

/**
 * 聊天实体
 */
@Entity(tableName = "chats")
@TypeConverters(Converters::class)
data class ChatEntity(
    @PrimaryKey
    val id: String,

    val type: ChatType,

    val name: String,

    val avatarUrl: String? = null,

    val lastMessageJson: String? = null,

    val lastMessageTime: Long,

    val unreadCount: Int = 0,

    val isMuted: Boolean = false,

    val isPinned: Boolean = false,

    val isArchived: Boolean = false,

    val createdAt: Long,

    val updatedAt: Long,

    val encryptionSessionId: String? = null
) {
    fun toChat(participants: List<ChatParticipant>): Chat {
        val json = Json { ignoreUnknownKeys = true }
        return Chat(
            id = id,
            type = type,
            name = name,
            avatarUrl = avatarUrl,
            participants = participants,
            lastMessage = lastMessageJson?.let { json.decodeFromString(it) },
            lastMessageTime = lastMessageTime,
            unreadCount = unreadCount,
            isMuted = isMuted,
            isPinned = isPinned,
            isArchived = isArchived,
            createdAt = createdAt,
            updatedAt = updatedAt,
            encryptionSessionId = encryptionSessionId
        )
    }

    companion object {
        fun fromChat(chat: Chat): ChatEntity {
            val json = Json { ignoreUnknownKeys = true }
            return ChatEntity(
                id = chat.id,
                type = chat.type,
                name = chat.name,
                avatarUrl = chat.avatarUrl,
                lastMessageJson = chat.lastMessage?.let { json.encodeToString(it) },
                lastMessageTime = chat.lastMessageTime,
                unreadCount = chat.unreadCount,
                isMuted = chat.isMuted,
                isPinned = chat.isPinned,
                isArchived = chat.isArchived,
                createdAt = chat.createdAt,
                updatedAt = chat.updatedAt,
                encryptionSessionId = chat.encryptionSessionId
            )
        }
    }
}

/**
 * 聊天参与者实体
 */
@Entity(
    tableName = "chat_participants",
    primaryKeys = ["chatId", "userId"]
)
data class ChatParticipantEntity(
    val chatId: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val role: ParticipantRole = ParticipantRole.MEMBER,
    val joinedAt: Long,
    val lastReadMessageId: String? = null
) {
    fun toChatParticipant(): ChatParticipant {
        return ChatParticipant(
            userId = userId,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            role = role,
            joinedAt = joinedAt,
            lastReadMessageId = lastReadMessageId
        )
    }

    companion object {
        fun fromChatParticipant(chatId: String, participant: ChatParticipant): ChatParticipantEntity {
            return ChatParticipantEntity(
                chatId = chatId,
                userId = participant.userId,
                username = participant.username,
                displayName = participant.displayName,
                avatarUrl = participant.avatarUrl,
                role = participant.role,
                joinedAt = participant.joinedAt,
                lastReadMessageId = participant.lastReadMessageId
            )
        }
    }
}

/**
 * 会话状态实体
 */
@Entity(tableName = "sessions")
@TypeConverters(Converters::class)
data class SessionEntity(
    @PrimaryKey
    val sessionId: String,

    val remoteUserId: String,

    val remoteDeviceId: String,

    val rootKey: String,

    val chainKey: String,

    val sendingChainKey: String? = null,

    val receivingChainKey: String? = null,

    val sendingRatchetKey: String? = null,

    val receivingRatchetKey: String? = null,

    val sendingChainIndex: Int = 0,

    val receivingChainIndex: Int = 0,

    val previousChainLength: Int = 0,

    val messageNumber: Int = 0,

    val ratchetStateJson: String,

    val createdAt: Long,

    val updatedAt: Long
)

/**
 * 预密钥实体
 */
@Entity(tableName = "pre_keys")
data class PreKeyEntity(
    @PrimaryKey
    val id: Int,

    val publicKey: String,

    val privateKey: String
)

/**
 * 签名预密钥实体
 */
@Entity(tableName = "signed_pre_keys")
data class SignedPreKeyEntity(
    @PrimaryKey
    val id: Int,

    val publicKey: String,

    val privateKey: String,

    val signature: String
)

/**
 * 类型转换器
 */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = MessageType.valueOf(value)

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus): String = value.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)

    @TypeConverter
    fun fromChatType(value: ChatType): String = value.name

    @TypeConverter
    fun toChatType(value: String): ChatType = ChatType.valueOf(value)

    @TypeConverter
    fun fromParticipantRole(value: ParticipantRole): String = value.name

    @TypeConverter
    fun toParticipantRole(value: String): ParticipantRole = ParticipantRole.valueOf(value)
}
