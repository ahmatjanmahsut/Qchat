package com.qchat.app.data.repository

import com.qchat.app.data.local.dao.ContactDao
import com.qchat.app.data.local.entity.ContactEntity
import com.qchat.app.data.local.entity.SyncStatus
import com.qchat.app.data.remote.api.ContactApi
import com.qchat.app.data.remote.dto.ContactDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 联系人仓库接口
 */
interface ContactRepository {
    fun getAllContactsFlow(): Flow<List<ContactEntity>>
    fun getBlockedContactsFlow(): Flow<List<ContactEntity>>
    fun getStarredContactsFlow(): Flow<List<ContactEntity>>
    fun searchContactsFlow(query: String): Flow<List<ContactEntity>>
    fun getContactByUserIdFlow(userId: String): Flow<ContactEntity?>
    suspend fun getContactById(contactId: String): ContactEntity?
    suspend fun getContactByUserId(userId: String): ContactEntity?
    suspend fun getContactByPhoneNumber(phoneNumber: String): ContactEntity?
    suspend fun addContact(contact: ContactEntity): Result<ContactEntity>
    suspend fun updateContact(contact: ContactEntity)
    suspend fun deleteContact(contactId: String)
    suspend fun blockContact(contactId: String, blocked: Boolean)
    suspend fun muteContact(contactId: String, muted: Boolean)
    suspend fun starContact(contactId: String, starred: Boolean)
    suspend fun syncContacts(): Result<List<ContactEntity>>
    suspend fun importContacts(contacts: List<ContactEntity>): Result<List<ContactEntity>>
    fun getContactCountFlow(): Flow<Int>
}

/**
 * 联系人仓库实现
 */
@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val contactApi: ContactApi
) : ContactRepository {

    override fun getAllContactsFlow(): Flow<List<ContactEntity>> = contactDao.getAllContactsFlow()

    override fun getBlockedContactsFlow(): Flow<List<ContactEntity>> = contactDao.getBlockedContactsFlow()

    override fun getStarredContactsFlow(): Flow<List<ContactEntity>> = contactDao.getStarredContactsFlow()

    override fun searchContactsFlow(query: String): Flow<List<ContactEntity>> = contactDao.searchContactsFlow(query)

    override fun getContactByUserIdFlow(userId: String): Flow<ContactEntity?> =
        contactDao.getContactByUserIdFlow(userId)

    override suspend fun getContactById(contactId: String): ContactEntity? =
        contactDao.getContactById(contactId)

    override suspend fun getContactByUserId(userId: String): ContactEntity? =
        contactDao.getContactByUserId(userId)

    override suspend fun getContactByPhoneNumber(phoneNumber: String): ContactEntity? =
        contactDao.getContactByPhoneNumber(phoneNumber)

    override suspend fun addContact(contact: ContactEntity): Result<ContactEntity> {
        return try {
            val response = contactApi.addContact(contact.toDto())
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val entity = dto.toEntity()
                    contactDao.insert(entity)
                    Result.success(entity)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Add contact failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateContact(contact: ContactEntity) {
        contactDao.update(contact.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteContact(contactId: String) {
        contactDao.deleteById(contactId)
    }

    override suspend fun blockContact(contactId: String, blocked: Boolean) {
        contactDao.updateBlocked(contactId, blocked)
    }

    override suspend fun muteContact(contactId: String, muted: Boolean) {
        contactDao.updateMuted(contactId, muted)
    }

    override suspend fun starContact(contactId: String, starred: Boolean) {
        contactDao.updateStarred(contactId, starred)
    }

    override suspend fun syncContacts(): Result<List<ContactEntity>> {
        return try {
            val response = contactApi.getContacts()
            if (response.isSuccessful) {
                response.body()?.let { contacts ->
                    val entities = contacts.map { it.toEntity() }
                    contactDao.insertAll(entities)
                    Result.success(entities)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Sync contacts failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importContacts(contacts: List<ContactEntity>): Result<List<ContactEntity>> {
        return try {
            val response = contactApi.importContacts(contacts.map { it.toDto() })
            if (response.isSuccessful) {
                response.body()?.let { importedContacts ->
                    val entities = importedContacts.map { it.toEntity() }
                    contactDao.insertAll(entities)
                    Result.success(entities)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Import contacts failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getContactCountFlow(): Flow<Int> = contactDao.getContactCountFlow()
}

/**
 * 联系人DTO扩展函数
 */
fun ContactEntity.toDto() = ContactDto(
    id = id,
    userId = userId,
    phoneNumber = phoneNumber,
    displayName = displayName,
    profilePicture = profilePicture,
    isBlocked = isBlocked,
    isMuted = isMuted,
    isStarred = isStarred,
    addedAt = addedAt,
    updatedAt = updatedAt
)

fun ContactDto.toEntity(): ContactEntity = ContactEntity(
    id = id,
    userId = userId,
    phoneNumber = phoneNumber,
    displayName = displayName,
    profilePicture = profilePicture,
    isBlocked = isBlocked,
    isMuted = isMuted,
    isStarred = isStarred,
    addedAt = addedAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)
