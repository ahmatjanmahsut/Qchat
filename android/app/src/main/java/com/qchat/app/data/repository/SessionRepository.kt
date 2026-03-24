package com.qchat.app.data.repository

import com.qchat.app.data.local.dao.SessionDao
import com.qchat.app.data.local.entity.SessionEntity
import com.qchat.app.data.local.entity.SessionType
import com.qchat.app.data.local.entity.SyncStatus
import com.qchat.app.data.remote.api.SessionApi
import com.qchat.app.data.remote.dto.SessionDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话仓库接口
 */
interface SessionRepository {
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>
    fun getActiveSessionsFlow(): Flow<List<SessionEntity>>
    fun getArchivedSessionsFlow(): Flow<List<SessionEntity>>
    fun getPinnedSessionsFlow(): Flow<List<SessionEntity>>
    fun getUnreadSessionsFlow(): Flow<List<SessionEntity>>
    suspend fun getSessionById(sessionId: String): SessionEntity?
    fun getSessionByIdFlow(sessionId: String): Flow<SessionEntity?>
    suspend fun getSessionByParticipant(participantId: String): SessionEntity?
    suspend fun getOrCreatePrivateSession(participantId: String, name: String, avatar: String?): SessionEntity
    suspend fun getOrCreateGroupSession(groupId: String, name: String, avatar: String?): SessionEntity
    suspend fun saveSession(session: SessionEntity)
    suspend fun updateSession(session: SessionEntity)
    suspend fun deleteSession(sessionId: String)
    suspend fun updateDraft(sessionId: String, draft: String?)
    suspend fun updatePinned(sessionId: String, isPinned: Boolean)
    suspend fun updateMuted(sessionId: String, isMuted: Boolean)
    suspend fun updateArchived(sessionId: String, isArchived: Boolean)
    suspend fun markSessionAsRead(sessionId: String)
    suspend fun syncSessions(): Result<List<SessionEntity>>
    suspend fun getPendingSyncSessions(): List<SessionEntity>
    fun getUnreadCountFlow(): Flow<Int>
}

/**
 * 会话仓库实现
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val sessionApi: SessionApi
) : SessionRepository {

    override fun getAllSessionsFlow(): Flow<List<SessionEntity>> = sessionDao.getAllSessionsFlow()

    override fun getActiveSessionsFlow(): Flow<List<SessionEntity>> = sessionDao.getActiveSessionsFlow()

    override fun getArchivedSessionsFlow(): Flow<List<SessionEntity>> = sessionDao.getArchivedSessionsFlow()

    override fun getPinnedSessionsFlow(): Flow<List<SessionEntity>> = sessionDao.getPinnedSessionsFlow()

    override fun getUnreadSessionsFlow(): Flow<List<SessionEntity>> = sessionDao.getUnreadSessionsFlow()

    override suspend fun getSessionById(sessionId: String): SessionEntity? = sessionDao.getSessionById(sessionId)

    override fun getSessionByIdFlow(sessionId: String): Flow<SessionEntity?> = sessionDao.getSessionByIdFlow(sessionId)

    override suspend fun getSessionByParticipant(participantId: String): SessionEntity? =
        sessionDao.getSessionByParticipant(participantId)

    override suspend fun getOrCreatePrivateSession(
        participantId: String,
        name: String,
        avatar: String?
    ): SessionEntity {
        val existing = sessionDao.getSessionByParticipant(participantId)
        if (existing != null) return existing

        val newSession = SessionEntity(
            id = generateSessionId(participantId),
            type = SessionType.PRIVATE,
            participantId = participantId,
            groupId = null,
            name = name,
            avatar = avatar
        )
        sessionDao.insert(newSession)
        return newSession
    }

    override suspend fun getOrCreateGroupSession(
        groupId: String,
        name: String,
        avatar: String?
    ): SessionEntity {
        val existing = sessionDao.getSessionByGroup(groupId)
        if (existing != null) return existing

        val newSession = SessionEntity(
            id = generateSessionId(groupId),
            type = SessionType.GROUP,
            participantId = null,
            groupId = groupId,
            name = name,
            avatar = avatar
        )
        sessionDao.insert(newSession)
        return newSession
    }

    override suspend fun saveSession(session: SessionEntity) {
        sessionDao.insert(session)
    }

    override suspend fun updateSession(session: SessionEntity) {
        sessionDao.update(session.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteById(sessionId)
    }

    override suspend fun updateDraft(sessionId: String, draft: String?) {
        sessionDao.updateDraft(sessionId, draft)
    }

    override suspend fun updatePinned(sessionId: String, isPinned: Boolean) {
        sessionDao.updatePinned(sessionId, isPinned)
    }

    override suspend fun updateMuted(sessionId: String, isMuted: Boolean) {
        sessionDao.updateMuted(sessionId, isMuted)
    }

    override suspend fun updateArchived(sessionId: String, isArchived: Boolean) {
        sessionDao.updateArchived(sessionId, isArchived)
    }

    override suspend fun markSessionAsRead(sessionId: String) {
        sessionDao.updateUnreadCount(sessionId, 0)
    }

    override suspend fun syncSessions(): Result<List<SessionEntity>> {
        return try {
            val response = sessionApi.getSessions()
            if (response.isSuccessful) {
                response.body()?.let { sessions ->
                    val entities = sessions.map { it.toEntity() }
                    sessionDao.insertAll(entities)
                    Result.success(entities)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Sync failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingSyncSessions(): List<SessionEntity> =
        sessionDao.getSessionsBySyncStatus(SyncStatus.PENDING)

    override fun getUnreadCountFlow(): Flow<Int> = sessionDao.getUnreadSessionCountFlow()

    private fun generateSessionId(participantId: String): String =
        "session_${participantId}_${System.currentTimeMillis()}"
}

/**
 * 会话DTO扩展函数
 */
fun SessionDto.toEntity(): SessionEntity = SessionEntity(
    id = id,
    type = SessionType.valueOf(type),
    participantId = participantId,
    groupId = groupId,
    name = name,
    avatar = avatar,
    lastMessageId = lastMessageId,
    lastMessageContent = lastMessageContent,
    lastMessageTimestamp = lastMessageTimestamp,
    unreadCount = unreadCount,
    isPinned = isPinned,
    isMuted = isMuted,
    isArchived = isArchived,
    draftMessage = draftMessage,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)
