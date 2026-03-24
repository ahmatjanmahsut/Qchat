package com.qchat.server.models

import kotlinx.serialization.Serializable

/**
 * Conversation creation request
 */
@Serializable
data class CreateConversationRequest(
    val type: String, // "PRIVATE" or "GROUP"
    val name: String? = null,
    val description: String? = null,
    val participantIds: List<String>? = null // For group creation
)

/**
 * Conversation response
 */
@Serializable
data class ConversationResponse(
    val id: String,
    val type: String,
    val name: String? = null,
    val description: String? = null,
    val avatarUrl: String? = null,
    val ownerId: String? = null,
    val participants: List<UserInfo>? = null,
    val lastMessage: MessageResponse? = null,
    val unreadCount: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Add participant request
 */
@Serializable
data class AddParticipantRequest(
    val userId: String
)

/**
 * Remove participant request
 */
@Serializable
data class RemoveParticipantRequest(
    val userId: String
)

/**
 * Group member role update request
 */
@Serializable
data class UpdateMemberRoleRequest(
    val userId: String,
    val role: String
)
