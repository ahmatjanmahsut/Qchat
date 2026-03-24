package com.qchat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qchat.app.data.local.entity.GroupEntity
import com.qchat.app.data.local.entity.GroupRole
import com.qchat.app.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * 群组数据访问对象
 */
@Dao
interface GroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>)

    @Update
    suspend fun update(group: GroupEntity)

    @Delete
    suspend fun delete(group: GroupEntity)

    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: String): GroupEntity?

    @Query("SELECT * FROM groups WHERE id = :groupId")
    fun getGroupByIdFlow(groupId: String): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE createdBy = :userId")
    suspend fun getGroupsByCreator(userId: String): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE isJoined = 1 ORDER BY lastMessageTimestamp DESC")
    fun getJoinedGroupsFlow(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE isJoined = 1 ORDER BY lastMessageTimestamp DESC")
    suspend fun getJoinedGroups(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE syncStatus = :status")
    suspend fun getGroupsBySyncStatus(status: SyncStatus): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE name LIKE '%' || :query || '%'")
    fun searchGroupsFlow(query: String): Flow<List<GroupEntity>>

    @Query("UPDATE groups SET syncStatus = :status WHERE id = :groupId")
    suspend fun updateSyncStatus(groupId: String, status: SyncStatus)

    @Query("UPDATE groups SET memberCount = :count WHERE id = :groupId")
    suspend fun updateMemberCount(groupId: String, count: Int)

    @Query("UPDATE groups SET lastMessageId = :messageId, lastMessageTimestamp = :timestamp WHERE id = :groupId")
    suspend fun updateLastMessage(groupId: String, messageId: String, timestamp: Long)

    @Query("UPDATE groups SET unreadCount = :count WHERE id = :groupId")
    suspend fun updateUnreadCount(groupId: String, count: Int)

    @Query("UPDATE groups SET role = :role WHERE id = :groupId")
    suspend fun updateUserRole(groupId: String, role: GroupRole)

    @Query("UPDATE groups SET isJoined = :isJoined WHERE id = :groupId")
    suspend fun updateJoinedStatus(groupId: String, isJoined: Boolean)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteById(groupId: String)

    @Query("DELETE FROM groups")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM groups WHERE isJoined = 1")
    fun getJoinedGroupCountFlow(): Flow<Int>
}
