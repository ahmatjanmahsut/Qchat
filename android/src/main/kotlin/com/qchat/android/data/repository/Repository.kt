package com.qchat.android.data.repository

import com.qchat.android.data.local.dao.*
import com.qchat.android.data.local.entity.*
import com.qchat.android.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息仓库
 */
@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val chatParticipantDao: ChatParticipantDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 获取聊天消息
     */
    fun getMessages(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesByChat(chatId).map { entities ->
            entities.map { it.toMessage() }
        }
    }

    /**
     * 分页获取消息
     */
    suspend fun getMessagesPaginated(chatId: String, limit: Int, offset: Int): List<Message> {
        return messageDao.getMessagesByChatPaginated(chatId, limit, offset).map { it.toMessage() }
    }

    /**
     * 获取更早的消息
     */
    suspend fun getEarlierMessages(chatId: String, beforeTimestamp: Long, limit: Int = 20): List<Message> {
        return messageDao.getMessagesBefore(chatId, beforeTimestamp, limit).map { it.toMessage() }
    }

    /**
     * 获取消息通过ID
     */
    suspend fun getMessageById(messageId: String): Message? {
        return messageDao.getMessageById(messageId)?.toMessage()
    }

    /**
     * 保存消息
     */
    suspend fun saveMessage(message: Message) {
        messageDao.insertMessage(MessageEntity.fromMessage(message))
    }

    /**
     * 保存消息列表
     */
    suspend fun saveMessages(messages: List<Message>) {
        messageDao.insertMessages(messages.map { MessageEntity.fromMessage(it) })
    }

    /**
     * 更新消息状态
     */
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        messageDao.updateMessageStatus(messageId, status)
    }

    /**
     * 通过本地ID更新消息状态
     */
    suspend fun updateMessageStatusByLocalId(localId: String, status: MessageStatus) {
        messageDao.updateMessageStatusByLocalId(localId, status)
    }

    /**
     * 删除消息
     */
    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessage(messageId)
    }

    /**
     * 获取未读消息数
     */
    suspend fun getUnreadCount(chatId: String): Int {
        return messageDao.getUnreadCount(chatId)
    }

    /**
     * 获取过期消息
     */
    suspend fun getExpiredMessages(): List<Message> {
        return messageDao.getExpiredMessages(System.currentTimeMillis()).map { it.toMessage() }
    }

    /**
     * 删除过期消息
     */
    suspend fun deleteExpiredMessages() {
        val expiredMessages = messageDao.getExpiredMessages(System.currentTimeMillis())
        expiredMessages.forEach { messageDao.deleteMessage(it.id) }
    }
}

/**
 * 聊天仓库
 */
@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val chatParticipantDao: ChatParticipantDao,
    private val messageDao: MessageDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 获取所有聊天
     */
    fun getAllChats(): Flow<List<Chat>> {
        return chatDao.getAllChats().map { entities ->
            entities.map { entity ->
                val participants = chatParticipantDao.getParticipantsByChatSync(entity.id)
                entity.toChat(participants.map { it.toChatParticipant() })
            }
        }
    }

    /**
     * 获取活跃聊天（非归档）
     */
    fun getActiveChats(): Flow<List<Chat>> {
        return chatDao.getActiveChats().map { entities ->
            entities.map { entity ->
                val participants = chatParticipantDao.getParticipantsByChatSync(entity.id)
                entity.toChat(participants.map { it.toChatParticipant() })
            }
        }
    }

    /**
     * 获取聊天通过ID
     */
    suspend fun getChatById(chatId: String): Chat? {
        val entity = chatDao.getChatById(chatId) ?: return null
        val participants = chatParticipantDao.getParticipantsByChatSync(chatId)
        return entity.toChat(participants.map { it.toChatParticipant() })
    }

    /**
     * 获取聊天Flow
     */
    fun getChatByIdFlow(chatId: String): Flow<Chat?> {
        return chatDao.getChatByIdFlow(chatId).map { entity ->
            if (entity != null) {
                val participants = chatParticipantDao.getParticipantsByChatSync(entity.id)
                entity.toChat(participants.map { it.toChatParticipant() })
            } else null
        }
    }

    /**
     * 保存聊天
     */
    suspend fun saveChat(chat: Chat) {
        chatDao.insertChat(ChatEntity.fromChat(chat))
        chatParticipantDao.deleteAllParticipants(chat.id)
        chatParticipantDao.insertParticipants(
            chat.participants.map { ChatParticipantEntity.fromChatParticipant(chat.id, it) }
        )
    }

    /**
     * 更新最后消息
     */
    suspend fun updateLastMessage(chatId: String, message: Message) {
        chatDao.updateLastMessage(
            chatId = chatId,
            timestamp = message.timestamp,
            lastMessageJson = json.encodeToString(message)
        )
    }

    /**
     * 更新未读数
     */
    suspend fun updateUnreadCount(chatId: String, count: Int) {
        chatDao.updateUnreadCount(chatId, count)
    }

    /**
     * 标记已读
     */
    suspend fun markAsRead(chatId: String, lastReadMessageId: String, userId: String) {
        chatParticipantDao.updateLastReadMessageId(chatId, userId, lastReadMessageId)
        chatDao.updateUnreadCount(chatId, 0)
    }

    /**
     * 更新静音状态
     */
    suspend fun updateMutedStatus(chatId: String, isMuted: Boolean) {
        chatDao.updateMutedStatus(chatId, isMuted)
    }

    /**
     * 更新置顶状态
     */
    suspend fun updatePinnedStatus(chatId: String, isPinned: Boolean) {
        chatDao.updatePinnedStatus(chatId, isPinned)
    }

    /**
     * 更新归档状态
     */
    suspend fun updateArchivedStatus(chatId: String, isArchived: Boolean) {
        chatDao.updateArchivedStatus(chatId, isArchived)
    }

    /**
     * 删除聊天
     */
    suspend fun deleteChat(chatId: String) {
        messageDao.deleteMessagesByChat(chatId)
        chatParticipantDao.deleteAllParticipants(chatId)
        chatDao.deleteChatById(chatId)
    }
}

/**
 * 会话仓库
 */
@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val preKeyDao: PreKeyDao,
    private val signedPreKeyDao: SignedPreKeyDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 获取会话
     */
    suspend fun getSession(sessionId: String): SessionEntity? {
        return sessionDao.getSession(sessionId)
    }

    /**
     * 获取用户的所有会话
     */
    suspend fun getSessionsByUser(userId: String): List<SessionEntity> {
        return sessionDao.getSessionsByUser(userId)
    }

    /**
     * 获取用户和设备对应的会话
     */
    suspend fun getSessionByUserAndDevice(userId: String, deviceId: String): SessionEntity? {
        return sessionDao.getSessionByUserAndDevice(userId, deviceId)
    }

    /**
     * 保存会话
     */
    suspend fun saveSession(session: SessionEntity) {
        sessionDao.insertSession(session)
    }

    /**
     * 删除会话
     */
    suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteSessionById(sessionId)
    }

    /**
     * 删除用户的所有会话
     */
    suspend fun deleteSessionsByUser(userId: String) {
        sessionDao.deleteSessionsByUser(userId)
    }

    /**
     * 获取预密钥
     */
    suspend fun getPreKey(id: Int): PreKeyEntity? {
        return preKeyDao.getPreKey(id)
    }

    /**
     * 获取第一个可用预密钥
     */
    suspend fun getFirstPreKey(): PreKeyEntity? {
        return preKeyDao.getFirstPreKey()
    }

    /**
     * 获取所有预密钥
     */
    suspend fun getAllPreKeys(): List<PreKeyEntity> {
        return preKeyDao.getAllPreKeys()
    }

    /**
     * 获取预密钥数量
     */
    suspend fun getPreKeyCount(): Int {
        return preKeyDao.getPreKeyCount()
    }

    /**
     * 保存预密钥
     */
    suspend fun savePreKey(preKey: PreKeyEntity) {
        preKeyDao.insertPreKey(preKey)
    }

    /**
     * 保存预密钥列表
     */
    suspend fun savePreKeys(preKeys: List<PreKeyEntity>) {
        preKeyDao.insertPreKeys(preKeys)
    }

    /**
     * 删除预密钥
     */
    suspend fun deletePreKey(id: Int) {
        preKeyDao.deletePreKeyById(id)
    }

    /**
     * 获取签名预密钥
     */
    suspend fun getSignedPreKey(id: Int): SignedPreKeyEntity? {
        return signedPreKeyDao.getSignedPreKey(id)
    }

    /**
     * 获取最新的签名预密钥
     */
    suspend fun getLatestSignedPreKey(): SignedPreKeyEntity? {
        return signedPreKeyDao.getLatestSignedPreKey()
    }

    /**
     * 保存签名预密钥
     */
    suspend fun saveSignedPreKey(preKey: SignedPreKeyEntity) {
        signedPreKeyDao.insertSignedPreKey(preKey)
    }

    /**
     * 删除签名预密钥
     */
    suspend fun deleteSignedPreKey(id: Int) {
        signedPreKeyDao.deleteSignedPreKeyById(id)
    }
}
