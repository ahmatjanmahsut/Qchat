package com.qchat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qchat.app.data.local.entity.SyncStatus
import com.qchat.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据访问对象
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Update
    suspend fun update(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber")
    suspend fun getUserByPhoneNumber(phoneNumber: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE syncStatus = :status")
    suspend fun getUsersBySyncStatus(status: SyncStatus): List<UserEntity>

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("UPDATE users SET syncStatus = :status WHERE id = :userId")
    suspend fun updateSyncStatus(userId: String, status: SyncStatus)

    @Query("UPDATE users SET lastSeen = :lastSeen WHERE id = :userId")
    suspend fun updateLastSeen(userId: String, lastSeen: Long)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: String)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
