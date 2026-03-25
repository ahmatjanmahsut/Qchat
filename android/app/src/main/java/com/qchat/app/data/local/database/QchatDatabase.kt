package com.qchat.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.qchat.app.data.local.dao.ContactDao
import com.qchat.app.data.local.dao.GroupDao
import com.qchat.app.data.local.dao.GroupMemberDao
import com.qchat.app.data.local.dao.MessageDao
import com.qchat.app.data.local.dao.SessionDao
import com.qchat.app.data.local.dao.UserDao
import com.qchat.app.data.local.entity.ContactEntity
import com.qchat.app.data.local.entity.GroupEntity
import com.qchat.app.data.local.entity.GroupMemberEntity
import com.qchat.app.data.local.entity.MessageEntity
import com.qchat.app.data.local.entity.SessionEntity
import com.qchat.app.data.local.entity.UserEntity

/**
 * Qchat Room数据库
 */
@Database(
    entities = [
        UserEntity::class,
        SessionEntity::class,
        MessageEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        ContactEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class QchatDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun contactDao(): ContactDao

    companion object {
        const val DATABASE_NAME = "qchat_database"
    }
}
