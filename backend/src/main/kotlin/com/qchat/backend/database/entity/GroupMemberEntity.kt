package com.qchat.backend.database.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.CompositeId
import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.InstantColumnType

/**
 * 群组成员表
 */
object GroupMembers : CompositeIdTable("group_members", "group_id", "user_id") {
    val groupId = long("group_id")
    val userId = long("user_id")
    val role = enumeration<GroupRole>("role").default(GroupRole.MEMBER)
    val nickname = varchar("nickname", 100).nullable()
    val joinedAt = registerColumn("joined_at", InstantColumnType()).clientDefault { java.time.Instant.now() }
    val leftAt = registerColumn("left_at", InstantColumnType()).nullable()
}

class GroupMember(id: EntityID<CompositeId>) : Entity<CompositeId>(id) {
    companion object : EntityClass<CompositeId, GroupMember>(GroupMembers)

    val groupId: Long get() = id.value["group_id"] as Long
    val userId: Long get() = id.value["user_id"] as Long

    var role by GroupMembers.role
    var nickname by GroupMembers.nickname
    var joinedAt by GroupMembers.joinedAt
    var leftAt by GroupMembers.leftAt
}

enum class GroupRole {
    OWNER,
    ADMIN,
    MEMBER
}
