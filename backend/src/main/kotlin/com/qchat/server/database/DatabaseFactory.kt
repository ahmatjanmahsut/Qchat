package com.qchat.server.database

import com.qchat.server.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Database factory for managing PostgreSQL connection pool
 */
object DatabaseFactory {
    private val logger = LoggerFactory.getLogger("DatabaseFactory")

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = AppConfig.databaseUrl
        username = AppConfig.databaseUsername
        password = AppConfig.databasePassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 2
        idleTimeout = 30000
        connectionTimeout = 30000
        maxLifetime = 1800000
    }

    private val database: Database by lazy {
        Database.connect(HikariDataSource(hikariConfig))
    }

    fun init() {
        logger.info("Initializing database connection...")
        logger.info("Database URL: ${AppConfig.databaseUrl}")

        transaction(database) {
            // Create all tables
            SchemaUtils.create(
                Users,
                Sessions,
                Messages,
                Conversations,
                ConversationParticipants,
                GroupMembers,
                UserKeys,
                UserStatus
            )
        }

        logger.info("Database initialized successfully")
    }

    fun getDatabase(): Database = database
}
