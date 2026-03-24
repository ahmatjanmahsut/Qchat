package com.qchat.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.qchat.android.data.local.dao.*
import com.qchat.android.data.local.entity.*

/**
 * Qchat数据库
 */
@Database(
    entities = [
        MessageEntity::class,
        ChatEntity::class,
        ChatParticipantEntity::class,
        SessionEntity::class,
        PreKeyEntity::class,
        SignedPreKeyEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class QchatDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun chatDao(): ChatDao
    abstract fun chatParticipantDao(): ChatParticipantDao
    abstract fun sessionDao(): SessionDao
    abstract fun preKeyDao(): PreKeyDao
    abstract fun signedPreKeyDao(): SignedPreKeyDao

    companion object {
        const val DATABASE_NAME = "qchat_database"
    }
}
