package com.qchat.server.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Conversation table - stores both private and group conversations
 */
object Conversations : Table("conversations") {
    val id = varchar("id", 36).primaryKey() // UUID
    val type = enumeration("type", ConversationType::class)
    val name = varchar("name", 100).nullable() // Only for groups
    val description = text("description").nullable() // Only for groups
    val avatarUrl = varchar("avatar_url", 500).nullable() // Only for groups
    val ownerId = varchar("owner_id", 36).nullable() // Group owner
    val encryptionKey = text("encryption_key").nullable() // Group encryption key
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val lastMessageAt = timestamp("last_message_at").nullable()
    val isActive = bool("is_active").default(true)
}

/**
 * Conversation participant table - for private conversations
 */
object ConversationParticipants : Table("conversation_participants") {
    val id = varchar("id", 36).primaryKey()
    val conversationId = varchar("conversation_id", 36).references(Conversations.id)
    val userId = varchar("user_id", 36).references(Users.id)
    val joinedAt = timestamp("joined_at")
    val lastReadAt = timestamp("last_read_at").nullable()
    val isActive = bool("is_active").default(true)
}

/**
 * Group member table - for group conversations
 */
object GroupMembers : Table("group_members") {
    val id = varchar("id", 36).primaryKey()
    val conversationId = varchar("conversation_id", 36).references(Conversations.id)
    val userId = varchar("user_id", 36).references(Users.id)
    val role = enumeration("role", GroupRole::class).default(GroupRole.MEMBER)
    val joinedAt = timestamp("joined_at")
    val isActive = bool("is_active").default(true)
}

enum class ConversationType {
    PRIVATE, GROUP
}

enum class GroupRole {
    OWNER, ADMIN, MEMBER
}
