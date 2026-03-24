package com.qchat.app.data.repository

import com.qchat.app.data.local.dao.GroupDao
import com.qchat.app.data.local.dao.GroupMemberDao
import com.qchat.app.data.local.entity.GroupEntity
import com.qchat.app.data.local.entity.GroupMemberEntity
import com.qchat.app.data.local.entity.GroupRole
import com.qchat.app.data.local.entity.SyncStatus
import com.qchat.app.data.remote.api.GroupApi
import com.qchat.app.data.remote.dto.GroupDto
import com.qchat.app.data.remote.dto.GroupMemberDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 群组仓库接口
 */
interface GroupRepository {
    fun getJoinedGroupsFlow(): Flow<List<GroupEntity>>
    fun getGroupByIdFlow(groupId: String): Flow<GroupEntity?>
    fun searchGroupsFlow(query: String): Flow<List<GroupEntity>>
    suspend fun getGroupById(groupId: String): GroupEntity?
    suspend fun getJoinedGroups(): List<GroupEntity>
    suspend fun createGroup(group: GroupEntity): Result<GroupEntity>
    suspend fun updateGroup(group: GroupEntity)
    suspend fun deleteGroup(groupId: String)
    suspend fun leaveGroup(groupId: String)
    suspend fun syncGroups(): Result<List<GroupEntity>>
    fun getGroupMembersFlow(groupId: String): Flow<List<GroupMemberEntity>>
    suspend fun getGroupMembers(groupId: String): List<GroupMemberEntity>
    suspend fun addGroupMember(member: GroupMemberEntity): Result<GroupMemberEntity>
    suspend fun removeGroupMember(groupId: String, userId: String)
    suspend fun updateMemberRole(groupId: String, userId: String, role: GroupRole)
    suspend fun syncGroupMembers(groupId: String): Result<List<GroupMemberEntity>>
}

/**
 * 群组仓库实现
 */
@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val groupDao: GroupDao,
    private val groupMemberDao: GroupMemberDao,
    private val groupApi: GroupApi
) : GroupRepository {

    override fun getJoinedGroupsFlow(): Flow<List<GroupEntity>> = groupDao.getJoinedGroupsFlow()

    override fun getGroupByIdFlow(groupId: String): Flow<GroupEntity?> = groupDao.getGroupByIdFlow(groupId)

    override fun searchGroupsFlow(query: String): Flow<List<GroupEntity>> = groupDao.searchGroupsFlow(query)

    override suspend fun getGroupById(groupId: String): GroupEntity? = groupDao.getGroupById(groupId)

    override suspend fun getJoinedGroups(): List<GroupEntity> = groupDao.getJoinedGroups()

    override suspend fun createGroup(group: GroupEntity): Result<GroupEntity> {
        return try {
            val response = groupApi.createGroup(group.toDto())
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val entity = dto.toEntity()
                    groupDao.insert(entity)
                    Result.success(entity)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Create group failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateGroup(group: GroupEntity) {
        groupDao.update(group.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteGroup(groupId: String) {
        groupDao.deleteById(groupId)
    }

    override suspend fun leaveGroup(groupId: String) {
        groupDao.updateJoinedStatus(groupId, false)
    }

    override suspend fun syncGroups(): Result<List<GroupEntity>> {
        return try {
            val response = groupApi.getGroups()
            if (response.isSuccessful) {
                response.body()?.let { groups ->
                    val entities = groups.map { it.toEntity() }
                    groupDao.insertAll(entities)
                    Result.success(entities)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Sync groups failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getGroupMembersFlow(groupId: String): Flow<List<GroupMemberEntity>> =
        groupMemberDao.getMembersByGroupFlow(groupId)

    override suspend fun getGroupMembers(groupId: String): List<GroupMemberEntity> =
        groupMemberDao.getMembersByGroup(groupId)

    override suspend fun addGroupMember(member: GroupMemberEntity): Result<GroupMemberEntity> {
        return try {
            val response = groupApi.addMember(member.groupId, member.toDto())
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val entity = dto.toEntity()
                    groupMemberDao.insert(entity)
                    Result.success(entity)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Add member failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeGroupMember(groupId: String, userId: String) {
        groupMemberDao.markAsLeft(groupId, userId)
    }

    override suspend fun updateMemberRole(groupId: String, userId: String, role: GroupRole) {
        groupMemberDao.updateRole(groupId, userId, role)
    }

    override suspend fun syncGroupMembers(groupId: String): Result<List<GroupMemberEntity>> {
        return try {
            val response = groupApi.getMembers(groupId)
            if (response.isSuccessful) {
                response.body()?.let { members ->
                    val entities = members.map { it.toEntity() }
                    groupMemberDao.insertAll(entities)
                    Result.success(entities)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Sync members failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 群组DTO扩展函数
 */
fun GroupEntity.toDto() = com.qchat.app.data.remote.dto.GroupDto(
    id = id,
    name = name,
    description = description,
    avatar = avatar,
    createdBy = createdBy,
    memberCount = memberCount,
    maxMembers = maxMembers,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun GroupDto.toEntity(): GroupEntity = GroupEntity(
    id = id,
    name = name,
    description = description,
    avatar = avatar,
    createdBy = createdBy,
    memberCount = memberCount,
    maxMembers = maxMembers,
    isJoined = true,
    role = GroupRole.MEMBER,
    lastMessageId = null,
    lastMessageTimestamp = null,
    unreadCount = 0,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

/**
 * 群组成员DTO扩展函数
 */
fun GroupMemberEntity.toDto() = GroupMemberDto(
    groupId = groupId,
    userId = userId,
    role = role.name,
    nickname = nickname,
    joinedAt = joinedAt,
    leftAt = leftAt
)

fun GroupMemberDto.toEntity(): GroupMemberEntity = GroupMemberEntity(
    groupId = groupId,
    userId = userId,
    role = GroupRole.valueOf(role),
    nickname = nickname,
    joinedAt = joinedAt,
    leftAt = leftAt,
    syncStatus = SyncStatus.SYNCED
)
