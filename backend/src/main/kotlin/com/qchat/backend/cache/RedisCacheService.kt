package com.qchat.backend.cache

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.ktor.server.config.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration

/**
 * Redis缓存配置和连接管理
 */
object RedisConfig {

    private var redisClient: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null

    fun connect(config: ApplicationConfig) {
        val redisConfig = config.config("redis")
        val host = redisConfig.property("host").getString()
        val port = redisConfig.property("port").getString().toInt()

        val redisUri = RedisURI.builder()
            .withHost(host)
            .withPort(port)
            .build()

        redisClient = RedisClient.create(redisUri)
        connection = redisClient?.connect()

        // 设置过期时间
        connection?.timeout = Duration.ofSeconds(10)
    }

    fun getConnection(): StatefulRedisConnection<String, String> =
        connection ?: throw IllegalStateException("Redis not initialized. Call connect() first.")

    fun getCommands(): RedisCommands<String, String> = getConnection().sync()

    fun disconnect() {
        connection?.close()
        redisClient?.shutdown()
        connection = null
        redisClient = null
    }
}

/**
 * Redis缓存服务
 */
class RedisCacheService {

    private val json = Json { ignoreUnknownKeys = true }
    private val commands: RedisCommands<String, String> = RedisConfig.getCommands()

    companion object {
        private const val SESSION_PREFIX = "session:"
        private const val USER_PREFIX = "user:"
        private const val MESSAGE_PREFIX = "message:"
        private const val ONLINE_STATUS_PREFIX = "online:"
        private const val RATE_LIMIT_PREFIX = "ratelimit:"

        private const val DEFAULT_TTL_SECONDS = 3600L // 1小时
    }

    // Session缓存
    fun cacheSession(sessionId: String, sessionData: String, ttlSeconds: Long = DEFAULT_TTL_SECONDS) {
        commands.setex("$SESSION_PREFIX$sessionId", ttlSeconds, sessionData)
    }

    fun getSession(sessionId: String): String? = commands.get("$SESSION_PREFIX$sessionId")

    fun deleteSession(sessionId: String) {
        commands.del("$SESSION_PREFIX$sessionId")
    }

    // User缓存
    fun cacheUser(userId: Long, userData: String, ttlSeconds: Long = DEFAULT_TTL_SECONDS) {
        commands.setex("$USER_PREFIX$userId", ttlSeconds, userData)
    }

    fun getUser(userId: Long): String? = commands.get("$USER_PREFIX$userId")

    fun deleteUser(userId: Long) {
        commands.del("$USER_PREFIX$userId")
    }

    // Message缓存
    fun cacheMessage(messageId: String, messageData: String, ttlSeconds: Long = DEFAULT_TTL_SECONDS) {
        commands.setex("$MESSAGE_PREFIX$messageId", ttlSeconds, messageData)
    }

    fun getMessage(messageId: String): String? = commands.get("$MESSAGE_PREFIX$messageId")

    fun deleteMessage(messageId: String) {
        commands.del("$MESSAGE_PREFIX$messageId")
    }

    // 在线状态
    fun setUserOnline(userId: Long) {
        commands.set("$ONLINE_STATUS_PREFIX$userId", System.currentTimeMillis().toString())
    }

    fun setUserOffline(userId: Long) {
        commands.del("$ONLINE_STATUS_PREFIX$userId")
    }

    fun isUserOnline(userId: Long): Boolean = commands.exists("$ONLINE_STATUS_PREFIX$userId") > 0

    fun getOnlineUsers(): List<Long> {
        val keys = commands.keys("$ONLINE_STATUS_PREFIX*")
        return keys.mapNotNull { key ->
            key.removePrefix(ONLINE_STATUS_PREFIX).toLongOrNull()
        }
    }

    // 限流
    fun checkRateLimit(key: String, limit: Int, windowSeconds: Long): Boolean {
        val fullKey = "$RATE_LIMIT_PREFIX$key"
        val current = commands.incr(fullKey)

        if (current == 1L) {
            commands.expire(fullKey, windowSeconds)
        }

        return current <= limit
    }

    fun getRateLimitRemaining(key: String, limit: Int): Int {
        val fullKey = "$RATE_LIMIT_PREFIX$key"
        val current = commands.get(fullKey)?.toIntOrNull() ?: 0
        return (limit - current).coerceAtLeast(0)
    }

    // 通用操作
    fun <T> cache(key: String, value: T, ttlSeconds: Long = DEFAULT_TTL_SECONDS) where T : Any {
        val serialized = json.encodeToString(value)
        commands.setex(key, ttlSeconds, serialized)
    }

    fun get(key: String): String? = commands.get(key)

    inline fun <reified T> getAs(key: String): T? {
        val value = get(key) ?: return null
        return try {
            json.decodeFromString<T>(value)
        } catch (e: Exception) {
            null
        }
    }

    fun delete(key: String) {
        commands.del(key)
    }

    fun exists(key: String): Boolean = commands.exists(key) > 0

    // 批量操作
    fun mget(keys: List<String>): List<String?> = commands.mget(*keys.toTypedArray()).map { it.value }

    fun mset(map: Map<String, String>, ttlSeconds: Long? = null) {
        if (ttlSeconds != null) {
            map.forEach { (key, value) ->
                commands.setex(key, ttlSeconds, value)
            }
        } else {
            commands.mset(map)
        }
    }

    fun mdel(keys: List<String>) {
        commands.del(*keys.toTypedArray())
    }
}
