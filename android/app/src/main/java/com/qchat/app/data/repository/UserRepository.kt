package com.qchat.app.data.repository

import com.qchat.app.data.local.dao.UserDao
import com.qchat.app.data.local.entity.SyncStatus
import com.qchat.app.data.local.entity.UserEntity
import com.qchat.app.data.remote.api.UserApi
import com.qchat.app.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户仓库接口
 */
interface UserRepository {
    fun getCurrentUserFlow(): Flow<UserEntity?>
    suspend fun getCurrentUser(): UserEntity?
    suspend fun getUserById(userId: String): UserEntity?
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>
    suspend fun getUserByPhoneNumber(phoneNumber: String): UserEntity?
    suspend fun getUserByUsername(username: String): UserEntity?
    fun getAllUsersFlow(): Flow<List<UserEntity>>
    suspend fun saveUser(user: UserEntity)
    suspend fun saveUsers(users: List<UserEntity>)
    suspend fun updateUser(user: UserEntity)
    suspend fun deleteUser(user: UserEntity)
    suspend fun syncUsers()
    suspend fun getPendingSyncUsers(): List<UserEntity>
    suspend fun updateSyncStatus(userId: String, status: SyncStatus)
}

/**
 * 用户仓库实现
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userApi: UserApi
) : UserRepository {

    override fun getCurrentUserFlow(): Flow<UserEntity?> = userDao.getCurrentUserFlow()

    override suspend fun getCurrentUser(): UserEntity? = userDao.getCurrentUser()

    override suspend fun getUserById(userId: String): UserEntity? = userDao.getUserById(userId)

    override fun getUserByIdFlow(userId: String): Flow<UserEntity?> = userDao.getUserByIdFlow(userId)

    override suspend fun getUserByPhoneNumber(phoneNumber: String): UserEntity? =
        userDao.getUserByPhoneNumber(phoneNumber)

    override suspend fun getUserByUsername(username: String): UserEntity? =
        userDao.getUserByUsername(username)

    override fun getAllUsersFlow(): Flow<List<UserEntity>> = userDao.getAllUsersFlow()

    override suspend fun saveUser(user: UserEntity) {
        userDao.insert(user)
    }

    override suspend fun saveUsers(users: List<UserEntity>) {
        userDao.insertAll(users)
    }

    override suspend fun updateUser(user: UserEntity) {
        userDao.update(user.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteUser(user: UserEntity) {
        userDao.delete(user)
    }

    override suspend fun syncUsers() {
        try {
            val response = userApi.getContacts()
            if (response.isSuccessful) {
                response.body()?.let { users ->
                    val entities = users.map { it.toEntity() }
                    userDao.insertAll(entities)
                }
            }
        } catch (e: Exception) {
            // 处理网络错误
            e.printStackTrace()
        }
    }

    override suspend fun getPendingSyncUsers(): List<UserEntity> =
        userDao.getUsersBySyncStatus(SyncStatus.PENDING)

    override suspend fun updateSyncStatus(userId: String, status: SyncStatus) {
        userDao.updateSyncStatus(userId, status)
    }
}

/**
 * 用户DTO扩展函数
 */
fun UserDto.toEntity(): UserEntity = UserEntity(
    id = id,
    phoneNumber = phoneNumber,
    username = username,
    displayName = displayName,
    profilePicture = profilePicture,
    publicKey = publicKey,
    lastSeen = lastSeen,
    isCurrentUser = isCurrentUser,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)
