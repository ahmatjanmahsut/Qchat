package com.qchat.server.database

import com.qchat.server.config.AppConfig
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

/**
 * Redis connection factory for caching and session management
 */
object RedisFactory {
    private val logger = LoggerFactory.getLogger("RedisFactory")

    private val poolConfig = JedisPoolConfig().apply {
        maxTotal = 10
        maxIdle = 5
        minIdle = 2
        testOnBorrow = true
        testOnReturn = true
        testWhileIdle = true
    }

    private val jedisPool: JedisPool by lazy {
        logger.info("Initializing Redis connection pool...")
        logger.info("Redis URL: ${AppConfig.redisHost}:${AppConfig.redisPort}")

        JedisPool(
            poolConfig,
            AppConfig.redisHost,
            AppConfig.redisPort,
            2000, // connection timeout
            null  // no password
        )
    }

    fun getJedis(): Jedis {
        return jedisPool.resource
    }

    fun close() {
        logger.info("Closing Redis connection pool...")
        jedisPool.close()
    }
}
