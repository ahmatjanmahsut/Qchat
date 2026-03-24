package com.qchat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qchat.app.data.local.entity.GroupMemberEntity
import com.qchat.app.data.local.entity.GroupRole
import com.qchat.app.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * 群组成员数据访问对象
 */
@Dao
interface GroupMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<GroupMemberEntity>)

    @Update
    suspend fun update(member: GroupMemberEntity)

    @Delete
    suspend fun delete(member: GroupMemberEntity)

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun getMember(groupId: String, userId: String): GroupMemberEntity?

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND userId = :userId")
    fun getMemberFlow(groupId: String, userId: String): Flow<GroupMemberEntity?>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY role DESC, joinedAt ASC")
    fun getMembersByGroupFlow(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY role DESC, joinedAt ASC")
    suspend fun getMembersByGroup(groupId: String): List<GroupMemberEntity>

    @Query("SELECT * FROM group_members WHERE userId = :userId AND leftAt IS NULL")
    fun getGroupsByUserFlow(userId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE userId = :userId AND leftAt IS NULL")
    suspend fun getGroupsByUser(userId: String): List<GroupMemberEntity>

    @Query("SELECT * FROM group_members WHERE syncStatus = :status")
    suspend fun getMembersBySyncStatus(status: SyncStatus): List<GroupMemberEntity>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND role = :role")
    fun getMembersByRoleFlow(groupId: String, role: GroupRole): Flow<List<GroupMemberEntity>>

    @Query("SELECT COUNT(*) FROM group_members WHERE groupId = :groupId AND leftAt IS NULL")
    suspend fun getActiveMemberCount(groupId: String): Int

    @Query("SELECT COUNT(*) FROM group_members WHERE groupId = :groupId AND leftAt IS NULL")
    fun getActiveMemberCountFlow(groupId: String): Flow<Int>

    @Query("UPDATE group_members SET role = :role WHERE groupId = :groupId AND userId = :userId")
    suspend fun updateRole(groupId: String, userId: String, role: GroupRole)

    @Query("UPDATE group_members SET nickname = :nickname WHERE groupId = :groupId AND userId = :userId")
    suspend fun updateNickname(groupId: String, userId: String, nickname: String?)

    @Query("UPDATE group_members SET leftAt = :leftAt WHERE groupId = :groupId AND userId = :userId")
    suspend fun markAsLeft(groupId: String, userId: String, leftAt: Long = System.currentTimeMillis())

    @Query("UPDATE group_members SET syncStatus = :status WHERE groupId = :groupId AND userId = :userId")
    suspend fun updateSyncStatus(groupId: String, userId: String, status: SyncStatus)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun deleteByGroupAndUser(groupId: String, userId: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)

    @Query("DELETE FROM group_members")
    suspend fun deleteAll()

    @Query("SELECT userId FROM group_members WHERE groupId = :groupId AND leftAt IS NULL")
    suspend fun getActiveMemberIds(groupId: String): List<String>
}
