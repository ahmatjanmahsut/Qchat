package com.qchat.backend.database.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.InstantColumnType

/**
 * 设备表 - 用于多设备支持
 */
object Devices : LongIdTable("devices") {
    val userId = long("user_id").index()
    val deviceName = varchar("device_name", 100).nullable()
    val deviceType = varchar("device_type", 50).nullable()
    val deviceToken = varchar("device_token", 255).nullable()
    val lastActiveAt = registerColumn("last_active_at", InstantColumnType()).nullable()
    val isCurrentDevice = bool("is_current_device").default(false)
    val createdAt = registerColumn("created_at", InstantColumnType()).clientDefault { java.time.Instant.now() }
}

class Device(id: EntityID<Long>) : Entity<Long>(id) {
    companion object : EntityClass<Long, Device>(Devices)

    var userId by Devices.userId
    var deviceName by Devices.deviceName
    var deviceType by Devices.deviceType
    var deviceToken by Devices.deviceToken
    var lastActiveAt by Devices.lastActiveAt
    var isCurrentDevice by Devices.isCurrentDevice
    var createdAt by Devices.createdAt
}
