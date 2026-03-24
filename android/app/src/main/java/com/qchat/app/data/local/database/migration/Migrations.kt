package com.qchat.app.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移脚本
 */

// 从版本1到版本2的迁移
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加用户索引
        database.execSQL("CREATE INDEX IF NOT EXISTS index_users_phone ON users(phone)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_users_username ON users(username)")

        // 添加消息索引以提高查询性能
        database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_session_timestamp ON messages(sessionId, timestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_server_timestamp ON messages(serverTimestamp)")

        // 添加草稿消息字段到会话表
        database.execSQL("ALTER TABLE sessions ADD COLUMN draftMessage TEXT")
    }
}

// 从版本2到版本3的迁移
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加群组描述字段
        database.execSQL("ALTER TABLE `groups` ADD COLUMN description TEXT")

        // 添加消息编辑时间字段
        database.execSQL("ALTER TABLE messages ADD COLUMN editedAt INTEGER")

        // 创建消息搜索表（用于全文搜索）
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS message_search (
                messageId TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY (messageId) REFERENCES messages(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // 创建消息搜索索引
        database.execSQL("CREATE INDEX IF NOT EXISTS index_message_search_session ON message_search(sessionId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_message_search_timestamp ON message_search(timestamp)")
    }
}

// 从版本3到版本4的迁移 - 添加联系人功能
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加联系人相关字段到用户表
        database.execSQL("ALTER TABLE users ADD COLUMN isContact INTEGER DEFAULT 0")
        database.execSQL("ALTER TABLE users ADD COLUMN isBlocked INTEGER DEFAULT 0")

        // 创建联系人分组表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS contact_groups (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                sortOrder INTEGER DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        // 添加联系人与分组的关联
        database.execSQL("ALTER TABLE contacts ADD COLUMN contactGroupId TEXT")
    }
}

// 从版本4到版本5的迁移 - 性能优化
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 为会话表添加额外索引
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_type ON sessions(type)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_unread ON sessions(unreadCount)")

        // 创建待同步消息队列表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS sync_queue (
                id TEXT PRIMARY KEY,
                entityType TEXT NOT NULL,
                entityId TEXT NOT NULL,
                operation TEXT NOT NULL,
                payload TEXT NOT NULL,
                retryCount INTEGER DEFAULT 0,
                createdAt INTEGER NOT NULL,
                lastRetryAt INTEGER
            )
        """.trimIndent())

        // 创建同步队列索引
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entity ON sync_queue(entityType, entityId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_created ON sync_queue(createdAt)")
    }
}

// 从版本5到版本6的迁移 - 多设备支持
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加设备信息表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS devices (
                id TEXT PRIMARY KEY,
                userId TEXT NOT NULL,
                deviceName TEXT,
                deviceType TEXT,
                lastActiveAt INTEGER,
                isCurrentDevice INTEGER DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        // 添加设备索引
        database.execSQL("CREATE INDEX IF NOT EXISTS index_devices_user ON devices(userId)")

        // 为消息添加设备ID
        database.execSQL("ALTER TABLE messages ADD COLUMN deviceId TEXT")

        // 创建消息版本历史表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS message_history (
                id TEXT PRIMARY KEY,
                messageId TEXT NOT NULL,
                content TEXT NOT NULL,
                editedAt INTEGER NOT NULL,
                deviceId TEXT,
                FOREIGN KEY (messageId) REFERENCES messages(id) ON DELETE CASCADE
            )
        """.trimIndent())

        database.execSQL("CREATE INDEX IF NOT EXISTS index_message_history_message ON message_history(messageId)")
    }
}

// 所有迁移的列表
val ALL_MIGRATIONS = listOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6
)
