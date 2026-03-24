package com.qchat.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qchat.app.data.local.entity.SessionEntity
import com.qchat.app.data.local.entity.SessionType
import com.qchat.app.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * 会话数据访问对象
 */
@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionByIdFlow(sessionId: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE participantId = :participantId AND type = :type LIMIT 1")
    suspend fun getSessionByParticipant(participantId: String, type: SessionType = SessionType.PRIVATE): SessionEntity?

    @Query("SELECT * FROM sessions WHERE groupId = :groupId AND type = :type LIMIT 1")
    suspend fun getSessionByGroup(groupId: String, type: SessionType = SessionType.GROUP): SessionEntity?

    @Query("SELECT * FROM sessions WHERE syncStatus = :status")
    suspend fun getSessionsBySyncStatus(status: SyncStatus): List<SessionEntity>

    @Query("SELECT * FROM sessions ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE isArchived = 0 ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getActiveSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE isArchived = 1 ORDER BY lastMessageTimestamp DESC")
    fun getArchivedSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE isPinned = 1 ORDER BY lastMessageTimestamp DESC")
    fun getPinnedSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE unreadCount > 0 ORDER BY lastMessageTimestamp DESC")
    fun getUnreadSessionsFlow(): Flow<List<SessionEntity>>

    @Query("UPDATE sessions SET unreadCount = :count WHERE id = :sessionId")
    suspend fun updateUnreadCount(sessionId: String, count: Int)

    @Query("UPDATE sessions SET lastMessageId = :messageId, lastMessageContent = :content, lastMessageTimestamp = :timestamp WHERE id = :sessionId")
    suspend fun updateLastMessage(sessionId: String, messageId: String, content: String?, timestamp: Long)

    @Query("UPDATE sessions SET syncStatus = :status WHERE id = :sessionId")
    suspend fun updateSyncStatus(sessionId: String, status: SyncStatus)

    @Query("UPDATE sessions SET draftMessage = :draft WHERE id = :sessionId")
    suspend fun updateDraft(sessionId: String, draft: String?)

    @Query("UPDATE sessions SET isPinned = :isPinned WHERE id = :sessionId")
    suspend fun updatePinned(sessionId: String, isPinned: Boolean)

    @Query("UPDATE sessions SET isMuted = :isMuted WHERE id = :sessionId")
    suspend fun updateMuted(sessionId: String, isMuted: Boolean)

    @Query("UPDATE sessions SET isArchived = :isArchived WHERE id = :sessionId")
    suspend fun updateArchived(sessionId: String, isArchived: Boolean)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int

    @Query("SELECT COUNT(*) FROM sessions WHERE unreadCount > 0")
    fun getUnreadSessionCountFlow(): Flow<Int>
}
