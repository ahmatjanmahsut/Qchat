package com.qchat.backend.database

import com.qchat.backend.database.entity.*
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 数据库配置和连接管理
 */
object DatabaseConfig {

    private var database: Database? = null

    fun connect(config: ApplicationConfig) {
        val dbConfig = config.config("database")

        database = Database.connect(
            url = dbConfig.property("url").getString(),
            user = dbConfig.property("user").getString(),
            password = dbConfig.property("password").getString(),
            driver = dbConfig.property("driver").getString()
        )

        // 创建表结构
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Sessions,
                Messages,
                Groups,
                GroupMembers,
                Contacts,
                Devices
            )
        }
    }

    fun getDatabase(): Database = database
        ?: throw IllegalStateException("Database not initialized. Call connect() first.")

    fun disconnect() {
        database?.disconnect()
        database = null
    }
}
