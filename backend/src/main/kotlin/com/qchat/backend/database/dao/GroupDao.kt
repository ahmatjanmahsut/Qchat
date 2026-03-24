package com.qchat.backend.database.dao

import com.qchat.backend.database.entity.Group
import com.qchat.backend.database.entity.GroupMembers
import com.qchat.backend.database.entity.GroupRole
import com.qchat.backend.database.entity.Groups
import com.qchat.backend.database.entity.Session
import com.qchat.backend.database.entity.Sessions
import com.qchat.backend.database.entity.SessionType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update
import java.time.Instant

/**
 * 群组数据访问对象
 */
class GroupDao {

    fun findById(id: Long): Group? = Group.findById(id)

    fun findAll(): List<Group> = Group.all().toList()

    fun findByCreatedBy(userId: Long): List<Group> =
        Group.find { Groups.createdById eq userId }.toList()

    fun findByUserId(userId: Long): List<Group> {
        val memberGroupIds = GroupMemberDao().findGroupIdsByUserId(userId)
        return Group.find { Groups.id inList memberGroupIds }.toList()
    }

    fun search(query: String): List<Group> =
        Group.find { Groups.name like "%$query%" }.toList()

    fun create(
        name: String,
        description: String?,
        avatar: String?,
        createdById: Long
    ): Group = Group.new {
        this.name = name
        this.description = description
        this.avatar = avatar
        this.createdById = createdById
        this.memberCount = 1
    }

    fun update(
        groupId: Long,
        name: String? = null,
        description: String? = null,
        avatar: String? = null
    ): Group? {
        val group = findById(groupId) ?: return null
        transaction(group) {
            name?.let { group.name = it }
            description?.let { group.description = it }
            avatar?.let { group.avatar = it }
            group.updatedAt = Instant.now()
        }
        return group
    }

    fun updateMemberCount(groupId: Long, count: Int) {
        Groups.update({ Groups.id eq groupId }) {
            it[Groups.memberCount] = count
        }
    }

    fun updateLastMessage(groupId: Long, messageId: Long, timestamp: Instant) {
        Groups.update({ Groups.id eq groupId }) {
            it[Groups.lastMessageId] = messageId
            it[Groups.lastMessageTimestamp] = timestamp
        }
    }

    fun delete(groupId: Long): Boolean {
        val group = findById(groupId) ?: return false
        transaction(group) { group.delete() }
        return true
    }
}

/**
 * 群组成员数据访问对象
 */
class GroupMemberDao {

    fun findByGroupAndUser(groupId: Long, userId: Long): com.qchat.backend.database.entity.GroupMember? {
        return com.qchat.backend.database.entity.GroupMember.findById(
            com.qchat.backend.database.entity.CompositeId {
                put("group_id", groupId)
                put("user_id", userId)
            }
        )
    }

    fun findByGroupId(groupId: Long): List<com.qchat.backend.database.entity.GroupMember> =
        com.qchat.backend.database.entity.GroupMember.find {
            GroupMembers.groupId eq groupId and (GroupMembers.leftAt.isNull())
        }.orderBy(GroupMembers.role to SortOrder.Desc).toList()

    fun findGroupIdsByUserId(userId: Long): List<Long> =
        com.qchat.backend.database.entity.GroupMember.find {
            GroupMembers.userId eq userId and (GroupMembers.leftAt.isNull())
        }.map { it.groupId }

    fun isMember(groupId: Long, userId: Long): Boolean =
        findByGroupAndUser(groupId, userId) != null

    fun isAdminOrOwner(groupId: Long, userId: Long): Boolean {
        val member = findByGroupAndUser(groupId, userId) ?: return false
        return member.role == GroupRole.OWNER || member.role == GroupRole.ADMIN
    }

    fun create(
        groupId: Long,
        userId: Long,
        role: GroupRole = GroupRole.MEMBER,
        nickname: String? = null
    ): com.qchat.backend.database.entity.GroupMember {
        return com.qchat.backend.database.entity.GroupMember.new(
            com.qchat.backend.database.entity.CompositeId {
                put("group_id", groupId)
                put("user_id", userId)
            }
        ) {
            this.role = role
            this.nickname = nickname
        }
    }

    fun updateRole(groupId: Long, userId: Long, role: GroupRole): Boolean {
        val member = findByGroupAndUser(groupId, userId) ?: return false
        transaction(member) { member.role = role }
        return true
    }

    fun updateNickname(groupId: Long, userId: Long, nickname: String?): Boolean {
        val member = findByGroupAndUser(groupId, userId) ?: return false
        transaction(member) { member.nickname = nickname }
        return true
    }

    fun remove(groupId: Long, userId: Long): Boolean {
        val member = findByGroupAndUser(groupId, userId) ?: return false
        transaction(member) { member.leftAt = Instant.now() }
        return true
    }

    fun getMemberCount(groupId: Long): Int =
        com.qchat.backend.database.entity.GroupMember.find {
            GroupMembers.groupId eq groupId and (GroupMembers.leftAt.isNull())
        }.count().toInt()

    fun getMemberIds(groupId: Long): List<Long> =
        findByGroupId(groupId).map { it.userId }
}
