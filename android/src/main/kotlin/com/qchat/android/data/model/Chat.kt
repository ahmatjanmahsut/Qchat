package com.qchat.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 聊天类型
 */
@Serializable
enum class ChatType {
    PRIVATE,     // 私人聊天
    GROUP,       // 群组聊天
    CHANNEL      // 频道
}

/**
 * 聊天模型
 */
@Serializable
data class Chat(
    @SerialName("id")
    val id: String,

    @SerialName("type")
    val type: ChatType,

    @SerialName("name")
    val name: String,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("participants")
    val participants: List<ChatParticipant>,

    @SerialName("last_message")
    val lastMessage: Message? = null,

    @SerialName("last_message_time")
    val lastMessageTime: Long,

    @SerialName("unread_count")
    val unreadCount: Int = 0,

    @SerialName("is_muted")
    val isMuted: Boolean = false,

    @SerialName("is_pinned")
    val isPinned: Boolean = false,

    @SerialName("is_archived")
    val isArchived: Boolean = false,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("updated_at")
    val updatedAt: Long,

    @SerialName("encryption_session_id")
    val encryptionSessionId: String? = null
)

/**
 * 聊天参与者
 */
@Serializable
data class ChatParticipant(
    @SerialName("user_id")
    val userId: String,

    @SerialName("username")
    val username: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("role")
    val role: ParticipantRole = ParticipantRole.MEMBER,

    @SerialName("joined_at")
    val joinedAt: Long,

    @SerialName("last_read_message_id")
    val lastReadMessageId: String? = null
)

/**
 * 参与者角色
 */
@Serializable
enum class ParticipantRole {
    OWNER,        // 创建者
    ADMIN,        // 管理员
    MEMBER        // 普通成员
}

/**
 * 私聊信息
 */
@Serializable
data class PrivateChatInfo(
    @SerialName("user_id")
    val userId: String,

    @SerialName("username")
    val username: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("is_online")
    val isOnline: Boolean = false,

    @SerialName("last_seen")
    val lastSeen: Long? = null,

    @SerialName("public_key")
    val publicKey: String? = null,

    @SerialName("is_verified")
    val isVerified: Boolean = false
)

/**
 * 群组信息
 */
@Serializable
data class GroupChatInfo(
    @SerialName("group_id")
    val groupId: String,

    @SerialName("name")
    val name: String,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("description")
    val description: String? = null,

    @SerialName("member_count")
    val memberCount: Int,

    @SerialName("max_members")
    val maxMembers: Int = 1000,

    @SerialName("is_public")
    val isPublic: Boolean = false,

    @SerialName("invite_link")
    val inviteLink: String? = null,

    @SerialName("created_by")
    val createdBy: String,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("group_encryption_key")
    val groupEncryptionKey: String? = null
)
