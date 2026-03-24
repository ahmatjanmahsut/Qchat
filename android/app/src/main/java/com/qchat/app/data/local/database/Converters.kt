package com.qchat.app.data.local.database

import androidx.room.TypeConverter
import com.qchat.app.data.local.entity.GroupRole
import com.qchat.app.data.local.entity.MessageContentType
import com.qchat.app.data.local.entity.MessageStatus
import com.qchat.app.data.local.entity.SessionType
import com.qchat.app.data.local.entity.SyncStatus

/**
 * Room类型转换器
 */
class Converters {

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromSessionType(type: SessionType): String = type.name

    @TypeConverter
    fun toSessionType(value: String): SessionType = SessionType.valueOf(value)

    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)

    @TypeConverter
    fun fromMessageContentType(type: MessageContentType): String = type.name

    @TypeConverter
    fun toMessageContentType(value: String): MessageContentType = MessageContentType.valueOf(value)

    @TypeConverter
    fun fromGroupRole(role: GroupRole): String = role.name

    @TypeConverter
    fun toGroupRole(value: String): GroupRole = GroupRole.valueOf(value)
}
