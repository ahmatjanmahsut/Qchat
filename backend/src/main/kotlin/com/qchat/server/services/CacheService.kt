package com.qchat.server.services

import com.qchat.server.database.RedisFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Redis cache service for managing cached data
 */
class CacheService {
    private val logger = LoggerFactory.getLogger("CacheService")
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val USER_PREFIX = "user:"
        private const val SESSION_PREFIX = "session:"
        private const val CONVERSATION_PREFIX = "conversation:"
        private const val MESSAGE_PREFIX = "message:"
        private const val ONLINE_STATUS_PREFIX = "online:"
        private const val DEFAULT_EXPIRE_SECONDS = 3600L // 1 hour
    }

    // User caching
    fun cacheUser(userId: String, userData: UserCacheData) {
        try {
            val jedis = RedisFactory.getJedis()
            val key = "$USER_PREFIX$userId"
            val value = json.encodeToString(UserCacheData.serializer(), userData)
            jedis.setex(key, DEFAULT_EXPIRE_SECONDS, value)
            jedis.close()
            logger.debug("Cached user: $userId")
        } catch (e: Exception) {
            logger.error("Error caching user: $userId", e)
        }
    }

    fun getCachedUser(userId: String): UserCacheData? {
        return try {
            val jedis = RedisFactory.getJedis()
            val key = "$USER_PREFIX$userId"
            val value = jedis.get(key)
            jedis.close()
            value?.let { json.decodeFromString(UserCacheData.serializer(), it) }
        } catch (e: Exception) {
            logger.error("Error getting cached user: $userId", e)
            null
        }
    }

    fun invalidateUser(userId: String) {
        try {
            val jedis = RedisFactory.getJedis()
            jedis.del("$USER_PREFIX$userId")
            jedis.close()
            logger.debug("Invalidated user cache: $userId")
        } catch (e: Exception) {
            logger.error("Error invalidating user cache: $userId", e)
        }
    }

    // Session caching
    fun cacheSession(sessionId: String, sessionData: SessionCacheData) {
        try {
            val jedis = RedisFactory.getJedis()
            val key = "$SESSION_PREFIX$sessionId"
            val value = json.encodeToString(SessionCacheData.serializer(), sessionData)
            jedis.setex(key, DEFAULT_EXPIRE_SECONDS, value)
            jedis.close()
            logger.debug("Cached session: $sessionId")
        } catch (e: Exception) {
            logger.error("Error caching session: $sessionId", e)
        }
    }

    fun getCachedSession(sessionId: String): SessionCacheData? {
        return try {
            val jedis = RedisFactory.getJedis()
            val key = "$SESSION_PREFIX$sessionId"
            val value = jedis.get(key)
            jedis.close()
            value?.let { json.decodeFromString(SessionCacheData.serializer(), it) }
        } catch (e: Exception) {
            logger.error("Error getting cached session: $sessionId", e)
            null
        }
    }

    fun invalidateSession(sessionId: String) {
        try {
            val jedis = RedisFactory.getJedis()
            jedis.del("$SESSION_PREFIX$sessionId")
            jedis.close()
            logger.debug("Invalidated session cache: $sessionId")
        } catch (e: Exception) {
            logger.error("Error invalidating session: $sessionId", e)
        }
    }

    // Online status
    fun setUserOnline(userId: String) {
        try {
            val jedis = RedisFactory.getJedis()
            val key = "$ONLINE_STATUS_PREFIX$userId"
            jedis.setex(key, 300, "online") // 5 minutes expire
            jedis.sadd("online_users", userId)
            jedis.close()
            logger.debug("Set user online: $userId")
        } catch (e: Exception) {
            logger.error("Error setting user online: $userId", e)
        }
    }

    fun setUserOffline(userId: String) {
        try {
            val jedis = RedisFactory.getJedis()
            val key = "$ONLINE_STATUS_PREFIX$userId"
            jedis.del(key)
            jedis.srem("online_users", userId)
            jedis.close()
            logger.debug("Set user offline: $userId")
        } catch (e: Exception) {
            logger.error("Error setting user offline: $userId", e)
        }
    }

    fun isUserOnline(userId: String): Boolean {
        return try {
            val jedis = RedisFactory.getJedis()
            val key = "$ONLINE_STATUS_PREFIX$userId"
            val isOnline = jedis.exists(key)
            jedis.close()
            isOnline
        } catch (e: Exception) {
            logger.error("Error checking user online status: $userId", e)
            false
        }
    }

    fun getOnlineUsers(): Set<String> {
        return try {
            val jedis = RedisFactory.getJedis()
            val users = jedis.smembers("online_users")
            jedis.close()
            users
        } catch (e: Exception) {
            logger.error("Error getting online users", e)
            emptySet()
        }
    }

    // Conversation unread count
    fun incrementUnreadCount(conversationId: String, userId: String) {
        try {
            val jedis = RedisFactory.getJedis()
            val key = "unread:$conversationId:$userId"
            jedis.incr(key)
            jedis.expire(key, 86400) // 24 hours
            jedis.close()
        } catch (e: Exception) {
            logger.error("Error incrementing unread count", e)
        }
    }

    fun getUnreadCount(conversationId: String, userId: String): Long {
        return try {
            val jedis = RedisFactory.getJedis()
            val key = "unread:$conversationId:$userId"
            val count = jedis.get(key)?.toLongOrNull() ?: 0L
            jedis.close()
            count
        } catch (e: Exception) {
            logger.error("Error getting unread count", e)
            0L
        }
    }

    fun clearUnreadCount(conversationId: String, userId: String) {
        try {
            val jedis = RedisFactory.getJedis()
            val key = "unread:$conversationId:$userId"
            jedis.del(key)
            jedis.close()
        } catch (e: Exception) {
            logger.error("Error clearing unread count", e)
        }
    }
}

@Serializable
data class UserCacheData(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null
)

@Serializable
data class SessionCacheData(
    val sessionId: String,
    val userId: String,
    val deviceId: String? = null
)
