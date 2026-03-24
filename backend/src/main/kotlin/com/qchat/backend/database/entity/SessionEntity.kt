package com.qchat.backend.database.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.InstantColumnType

/**
 * 会话表
 */
object Sessions : LongIdTable("sessions") {
    val type = enumeration<SessionType>("type")
    val participant1Id = long("participant1_id").nullable()
    val participant2Id = long("participant2_id").nullable()
    val groupId = long("group_id").nullable()
    val name = varchar("name", 100)
    val avatar = varchar("avatar", 500).nullable()
    val lastMessageId = long("last_message_id").nullable()
    val lastMessageContent = text("last_message_content").nullable()
    val lastMessageTimestamp = registerColumn("last_message_timestamp", InstantColumnType()).nullable()
    val unreadCount1 = int("unread_count_1").default(0)
    val unreadCount2 = int("unread_count_2").default(0)
    val isPinned = bool("is_pinned").default(false)
    val isMuted = bool("is_muted").default(false)
    val isArchived = bool("is_archived").default(false)
    val draftMessage = text("draft_message").nullable()
    val createdAt = registerColumn("created_at", InstantColumnType()).clientDefault { java.time.Instant.now() }
    val updatedAt = registerColumn("updated_at", InstantColumnType()).clientDefault { java.time.Instant.now() }
}

class Session(id: EntityID<Long>) : Entity<Long>(id) {
    companion object : EntityClass<Long, Session>(Sessions)

    var type by Sessions.type
    var participant1Id by Sessions.participant1Id
    var participant2Id by Sessions.participant2Id
    var groupId by Sessions.groupId
    var name by Sessions.name
    var avatar by Sessions.avatar
    var lastMessageId by Sessions.lastMessageId
    var lastMessageContent by Sessions.lastMessageContent
    var lastMessageTimestamp by Sessions.lastMessageTimestamp
    var unreadCount1 by Sessions.unreadCount1
    var unreadCount2 by Sessions.unreadCount2
    var isPinned by Sessions.isPinned
    var isMuted by Sessions.isMuted
    var isArchived by Sessions.isArchived
    var draftMessage by Sessions.draftMessage
    var createdAt by Sessions.createdAt
    var updatedAt by Sessions.updatedAt
}

enum class SessionType {
    PRIVATE,
    GROUP
}
