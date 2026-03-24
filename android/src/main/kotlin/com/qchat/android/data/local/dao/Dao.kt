package com.qchat.android.data.local.dao

import androidx.room.*
import com.qchat.android.data.local.entity.*
import com.qchat.android.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * 消息数据访问对象
 */
@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC")
    fun getMessagesByChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesByChatPaginated(chatId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE localId = :localId")
    suspend fun getMessageByLocalId(localId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND status = :status")
    suspend fun getMessagesByStatus(chatId: String, status: MessageStatus): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND timestamp < :beforeTimestamp ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getMessagesBefore(chatId: String, beforeTimestamp: Long, limit: Int): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE messages SET status = :status WHERE localId = :localId")
    suspend fun updateMessageStatusByLocalId(localId: String, status: MessageStatus)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChat(chatId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId AND status != 'READ'")
    suspend fun getUnreadCount(chatId: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId AND timestamp > :lastReadTimestamp")
    suspend fun getUnreadCountSince(chatId: String, lastReadTimestamp: Long): Int

    @Query("SELECT * FROM messages WHERE expireAt IS NOT NULL AND expireAt < :currentTime")
    suspend fun getExpiredMessages(currentTime: Long): List<MessageEntity>
}

/**
 * 聊天数据访问对象
 */
@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatByIdFlow(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE type = :type ORDER BY lastMessageTime DESC")
    fun getChatsByType(type: String): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getActiveChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY lastMessageTime DESC")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("UPDATE chats SET lastMessageTime = :timestamp, lastMessageJson = :lastMessageJson WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, timestamp: Long, lastMessageJson: String)

    @Query("UPDATE chats SET unreadCount = :count WHERE id = :chatId")
    suspend fun updateUnreadCount(chatId: String, count: Int)

    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun updateMutedStatus(chatId: String, isMuted: Boolean)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun updatePinnedStatus(chatId: String, isPinned: Boolean)

    @Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    suspend fun updateArchivedStatus(chatId: String, isArchived: Boolean)

    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)
}

/**
 * 聊天参与者数据访问对象
 */
@Dao
interface ChatParticipantDao {

    @Query("SELECT * FROM chat_participants WHERE chatId = :chatId")
    fun getParticipantsByChat(chatId: String): Flow<List<ChatParticipantEntity>>

    @Query("SELECT * FROM chat_participants WHERE chatId = :chatId")
    suspend fun getParticipantsByChatSync(chatId: String): List<ChatParticipantEntity>

    @Query("SELECT * FROM chat_participants WHERE chatId = :chatId AND userId = :userId")
    suspend fun getParticipant(chatId: String, userId: String): ChatParticipantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ChatParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ChatParticipantEntity>)

    @Update
    suspend fun updateParticipant(participant: ChatParticipantEntity)

    @Delete
    suspend fun deleteParticipant(participant: ChatParticipantEntity)

    @Query("DELETE FROM chat_participants WHERE chatId = :chatId AND userId = :userId")
    suspend fun removeParticipant(chatId: String, userId: String)

    @Query("DELETE FROM chat_participants WHERE chatId = :chatId")
    suspend fun deleteAllParticipants(chatId: String)

    @Query("UPDATE chat_participants SET lastReadMessageId = :messageId WHERE chatId = :chatId AND userId = :userId")
    suspend fun updateLastReadMessageId(chatId: String, userId: String, messageId: String)
}

/**
 * 会话数据访问对象
 */
@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    fun getSessionFlow(sessionId: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE remoteUserId = :userId")
    suspend fun getSessionsByUser(userId: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE remoteUserId = :userId AND remoteDeviceId = :deviceId")
    suspend fun getSessionByUserAndDevice(userId: String, deviceId: String): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM sessions WHERE remoteUserId = :userId")
    suspend fun deleteSessionsByUser(userId: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()
}

/**
 * 预密钥数据访问对象
 */
@Dao
interface PreKeyDao {

    @Query("SELECT * FROM pre_keys WHERE id = :id")
    suspend fun getPreKey(id: Int): PreKeyEntity?

    @Query("SELECT * FROM pre_keys ORDER BY id ASC LIMIT 1")
    suspend fun getFirstPreKey(): PreKeyEntity?

    @Query("SELECT * FROM pre_keys ORDER BY id ASC")
    suspend fun getAllPreKeys(): List<PreKeyEntity>

    @Query("SELECT COUNT(*) FROM pre_keys")
    suspend fun getPreKeyCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreKey(preKey: PreKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreKeys(preKeys: List<PreKeyEntity>)

    @Delete
    suspend fun deletePreKey(preKey: PreKeyEntity)

    @Query("DELETE FROM pre_keys WHERE id = :id")
    suspend fun deletePreKeyById(id: Int)

    @Query("DELETE FROM pre_keys")
    suspend fun deleteAllPreKeys()
}

/**
 * 签名预密钥数据访问对象
 */
@Dao
interface SignedPreKeyDao {

    @Query("SELECT * FROM signed_pre_keys WHERE id = :id")
    suspend fun getSignedPreKey(id: Int): SignedPreKeyEntity?

    @Query("SELECT * FROM signed_pre_keys ORDER BY id DESC LIMIT 1")
    suspend fun getLatestSignedPreKey(): SignedPreKeyEntity?

    @Query("SELECT * FROM signed_pre_keys ORDER BY id DESC")
    suspend fun getAllSignedPreKeys(): List<SignedPreKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignedPreKey(preKey: SignedPreKeyEntity)

    @Delete
    suspend fun deleteSignedPreKey(preKey: SignedPreKeyEntity)

    @Query("DELETE FROM signed_pre_keys WHERE id = :id")
    suspend fun deleteSignedPreKeyById(id: Int)

    @Query("DELETE FROM signed_pre_keys")
    suspend fun deleteAllSignedPreKeys()
}
