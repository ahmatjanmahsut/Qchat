package com.qchat.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val phoneNumber: String,
    val username: String? = null,
    val displayName: String,
    val profilePicture: String? = null,
    val publicKey: String,
    val lastSeen: Long? = null,
    val isCurrentUser: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SessionDto(
    val id: String,
    val type: String, // PRIVATE, GROUP
    val participantId: String? = null,
    val groupId: String? = null,
    val name: String,
    val avatar: String? = null,
    val lastMessageId: String? = null,
    val lastMessageContent: String? = null,
    val lastMessageTimestamp: Long? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val draftMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class MessageDto(
    val id: String,
    val sessionId: String,
    val senderId: String,
    val content: String,
    val contentType: String, // TEXT, IMAGE, VIDEO, etc.
    val status: String, // SENDING, SENT, DELIVERED, READ, FAILED
    val isOutgoing: Boolean,
    val replyToMessageId: String? = null,
    val editedAt: Long? = null,
    val deletedAt: Long? = null,
    val localTimestamp: Long = System.currentTimeMillis(),
    val serverTimestamp: Long? = null,
    val version: Int = 1
)

@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val avatar: String? = null,
    val createdBy: String,
    val memberCount: Int = 0,
    val maxMembers: Int = 500,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class GroupMemberDto(
    val groupId: String,
    val userId: String,
    val role: String, // OWNER, ADMIN, MEMBER
    val nickname: String? = null,
    val joinedAt: Long = System.currentTimeMillis(),
    val leftAt: Long? = null
)

@Serializable
data class ContactDto(
    val id: String,
    val userId: String,
    val phoneNumber: String,
    val displayName: String,
    val profilePicture: String? = null,
    val isBlocked: Boolean = false,
    val isMuted: Boolean = false,
    val isStarred: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
