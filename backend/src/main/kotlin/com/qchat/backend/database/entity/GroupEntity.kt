package com.qchat.backend.database.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.InstantColumnType

/**
 * 群组表
 */
object Groups : LongIdTable("groups") {
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val avatar = varchar("avatar", 500).nullable()
    val createdById = long("created_by_id").index()
    val memberCount = int("member_count").default(0)
    val maxMembers = int("max_members").default(500)
    val lastMessageId = long("last_message_id").nullable()
    val lastMessageTimestamp = registerColumn("last_message_timestamp", InstantColumnType()).nullable()
    val createdAt = registerColumn("created_at", InstantColumnType()).clientDefault { java.time.Instant.now() }
    val updatedAt = registerColumn("updated_at", InstantColumnType()).clientDefault { java.time.Instant.now() }
}

class Group(id: EntityID<Long>) : Entity<Long>(id) {
    companion object : EntityClass<Long, Group>(Groups)

    var name by Groups.name
    var description by Groups.description
    var avatar by Groups.avatar
    var createdById by Groups.createdById
    var memberCount by Groups.memberCount
    var maxMembers by Groups.maxMembers
    var lastMessageId by Groups.lastMessageId
    var lastMessageTimestamp by Groups.lastMessageTimestamp
    var createdAt by Groups.createdAt
    var updatedAt by Groups.updatedAt
}
