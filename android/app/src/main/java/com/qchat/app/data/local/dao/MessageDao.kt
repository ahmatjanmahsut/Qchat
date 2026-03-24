package com.qchat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qchat.app.data.local.entity.MessageContentType
import com.qchat.app.data.local.entity.MessageEntity
import com.qchat.app.data.local.entity.MessageStatus
import com.qchat.app.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * 消息数据访问对象
 */
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(message: MessageEntity): Long

    @Update
    suspend fun update(message: MessageEntity)

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessageByIdFlow(messageId: String): Flow<MessageEntity?>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySessionFlow(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesBySessionPaginated(sessionId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND timestamp < :beforeTimestamp ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getMessagesBefore(sessionId: String, beforeTimestamp: Long, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND timestamp > :afterTimestamp ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getMessagesAfter(sessionId: String, afterTimestamp: Long, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE senderId = :senderId ORDER BY timestamp DESC")
    fun getMessagesBySenderFlow(senderId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE status = :status")
    suspend fun getMessagesByStatus(status: MessageStatus): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE syncStatus = :status")
    suspend fun getMessagesBySyncStatus(status: SyncStatus): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE contentType = :contentType")
    fun getMessagesByContentTypeFlow(contentType: MessageContentType): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE replyToMessageId = :messageId")
    fun getRepliesFlow(messageId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND deletedAt IS NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(sessionId: String): MessageEntity?

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE messages SET syncStatus = :status WHERE id = :messageId")
    suspend fun updateSyncStatus(messageId: String, status: SyncStatus)

    @Query("UPDATE messages SET serverTimestamp = :serverTimestamp, version = version + 1 WHERE id = :messageId")
    suspend fun updateServerTimestamp(messageId: String, serverTimestamp: Long)

    @Query("UPDATE messages SET status = :status, syncStatus = :syncStatus WHERE id = :messageId")
    suspend fun updateStatusAndSyncStatus(messageId: String, status: MessageStatus, syncStatus: SyncStatus)

    @Query("UPDATE messages SET editedAt = :editedAt, content = :content, version = version + 1 WHERE id = :messageId")
    suspend fun updateEditedMessage(messageId: String, editedAt: Long, content: String)

    @Query("UPDATE messages SET deletedAt = :deletedAt WHERE id = :messageId")
    suspend fun markAsDeleted(messageId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("DELETE FROM messages WHERE timestamp < :timestamp AND deletedAt IS NOT NULL")
    suspend fun deleteOldDeletedMessages(timestamp: Long)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId")
    suspend fun getMessageCount(sessionId: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId AND status = :status")
    suspend fun getMessageCountByStatus(sessionId: String, status: MessageStatus): Int

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId AND isOutgoing = 0 AND status != :readStatus")
    suspend fun getUnreadCount(sessionId: String, readStatus: MessageStatus = MessageStatus.READ): Int

    @Query("""
        SELECT * FROM messages
        WHERE sessionId = :sessionId
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp ASC
    """)
    suspend fun getMessagesInTimeRange(sessionId: String, startTime: Long, endTime: Long): List<MessageEntity>

    @Query("""
        SELECT * FROM messages
        WHERE id NOT IN (
            SELECT id FROM messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :excludeCount
        )
        AND sessionId = :sessionId
        AND deletedAt IS NOT NULL
        ORDER BY timestamp DESC
    """)
    suspend fun getDeletableMessages(sessionId: String, excludeCount: Int): List<MessageEntity>
}
