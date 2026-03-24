package com.qchat.backend.database.dao

import com.qchat.backend.database.entity.User
import com.qchat.backend.database.entity.Users
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.update
import java.time.Instant

/**
 * 用户数据访问对象
 */
class UserDao {

    fun findById(id: Long): User? = User.findById(id)

    fun findByPhoneNumber(phoneNumber: String): User? =
        User.find { Users.phoneNumber eq phoneNumber }.firstOrNull()

    fun findByUsername(username: String): User? =
        User.find { Users.username eq username }.firstOrNull()

    fun findByIds(ids: List<Long>): List<User> =
        User.find { Users.id inList ids }.toList()

    fun findAll(): List<User> = User.all().toList()

    fun create(
        phoneNumber: String,
        username: String?,
        displayName: String,
        passwordHash: String,
        publicKey: String,
        privateKeyEncrypted: String? = null,
        profilePicture: String? = null
    ): User = User.new {
        this.phoneNumber = phoneNumber
        this.username = username
        this.displayName = displayName
        this.passwordHash = passwordHash
        this.publicKey = publicKey
        this.privateKeyEncrypted = privateKeyEncrypted
        this.profilePicture = profilePicture
        this.isOnline = false
    }

    fun update(
        userId: Long,
        username: String? = null,
        displayName: String? = null,
        profilePicture: String? = null,
        publicKey: String? = null
    ): User? {
        val user = findById(userId) ?: return null
        transaction(user) {
            username?.let { user.username = it }
            displayName?.let { user.displayName = it }
            profilePicture?.let { user.profilePicture = it }
            publicKey?.let { user.publicKey = it }
            user.updatedAt = Instant.now()
        }
        return user
    }

    fun updateLastSeen(userId: Long, lastSeen: Instant) {
        Users.update({ Users.id eq userId }) {
            it[Users.lastSeen] = lastSeen
        }
    }

    fun setOnlineStatus(userId: Long, isOnline: Boolean) {
        Users.update({ Users.id eq userId }) {
            it[Users.isOnline] = isOnline
            if (!isOnline) {
                it[Users.lastSeen] = Instant.now()
            }
        }
    }

    fun search(query: String): List<User> =
        User.find {
            Users.username like "%$query%"
                or Users.displayName like "%$query%"
                or Users.phoneNumber like "%$query%"
        }.toList()

    fun delete(userId: Long): Boolean {
        val user = findById(userId) ?: return false
        transaction(user) { user.delete() }
        return true
    }
}
