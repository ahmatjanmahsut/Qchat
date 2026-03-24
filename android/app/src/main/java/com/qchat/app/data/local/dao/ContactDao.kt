package com.qchat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qchat.app.data.local.entity.ContactEntity
import com.qchat.app.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * 联系人数据访问对象
 */
@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    fun getContactByIdFlow(contactId: String): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE userId = :userId")
    suspend fun getContactByUserId(userId: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE userId = :userId")
    fun getContactByUserIdFlow(userId: String): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber")
    suspend fun getContactByPhoneNumber(phoneNumber: String): ContactEntity?

    @Query("SELECT * FROM contacts ORDER BY displayName ASC")
    fun getAllContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY displayName ASC")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE isBlocked = 1 ORDER BY displayName ASC")
    fun getBlockedContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isStarred = 1 ORDER BY displayName ASC")
    fun getStarredContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE syncStatus = :status")
    suspend fun getContactsBySyncStatus(status: SyncStatus): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE displayName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%'")
    fun searchContactsFlow(query: String): Flow<List<ContactEntity>>

    @Query("UPDATE contacts SET isBlocked = :isBlocked WHERE id = :contactId")
    suspend fun updateBlocked(contactId: String, isBlocked: Boolean)

    @Query("UPDATE contacts SET isMuted = :isMuted WHERE id = :contactId")
    suspend fun updateMuted(contactId: String, isMuted: Boolean)

    @Query("UPDATE contacts SET isStarred = :isStarred WHERE id = :contactId")
    suspend fun updateStarred(contactId: String, isStarred: Boolean)

    @Query("UPDATE contacts SET syncStatus = :status WHERE id = :contactId")
    suspend fun updateSyncStatus(contactId: String, status: SyncStatus)

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteById(contactId: String)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM contacts")
    fun getContactCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM contacts WHERE isBlocked = 1")
    fun getBlockedCountFlow(): Flow<Int>
}
