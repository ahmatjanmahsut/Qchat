package com.qchat.backend.database.dao

import com.qchat.backend.database.entity.Device
import com.qchat.backend.database.entity.Devices
import org.jetbrains.exposed.sql.update

/**
 * 设备数据访问对象
 */
class DeviceDao {

    fun findById(id: Long): Device? = Device.findById(id)

    fun findByUserId(userId: Long): List<Device> =
        Device.find { Devices.userId eq userId }.toList()

    fun findCurrentDevice(userId: Long): Device? =
        Device.find {
            Devices.userId eq userId and (Devices.isCurrentDevice eq true)
        }.firstOrNull()

    fun create(
        userId: Long,
        deviceName: String?,
        deviceType: String?,
        deviceToken: String? = null
    ): Device = Device.new {
        this.userId = userId
        this.deviceName = deviceName
        this.deviceType = deviceType
        this.deviceToken = deviceToken
        this.isCurrentDevice = false
    }

    fun setCurrentDevice(deviceId: Long, userId: Long): Boolean {
        // 先取消所有当前设备
        Devices.update({ Devices.userId eq userId }) {
            it[Devices.isCurrentDevice] = false
        }
        // 设置新当前设备
        val updated = Devices.update({ Devices.id eq deviceId }) {
            it[Devices.isCurrentDevice] = true
        }
        return updated > 0
    }

    fun updateLastActive(deviceId: Long) {
        Devices.update({ Devices.id eq deviceId }) {
            it[Devices.lastActiveAt] = java.time.Instant.now()
        }
    }

    fun delete(deviceId: Long): Boolean {
        val device = findById(deviceId) ?: return false
        transaction(device) { device.delete() }
        return true
    }

    fun deleteByUserId(userId: Long) {
        Device.find { Devices.userId eq userId }.forEach { device ->
            transaction(device) { device.delete() }
        }
    }
}
