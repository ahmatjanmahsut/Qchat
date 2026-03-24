package com.qchat.backend.database.entity

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.InstantColumnType

/**
 * 联系人表
 */
object Contacts : LongIdTable("contacts") {
    val ownerId = long("owner_id").index()
    val userId = long("user_id").index()
    val phoneNumber = varchar("phone_number", 20)
    val displayName = varchar("display_name", 100)
    val profilePicture = varchar("profile_picture", 500).nullable()
    val isBlocked = bool("is_blocked").default(false)
    val isMuted = bool("is_muted").default(false)
    val isStarred = bool("is_starred").default(false)
    val addedAt = registerColumn("added_at", InstantColumnType()).clientDefault { java.time.Instant.now() }
    val updatedAt = registerColumn("updated_at", InstantColumnType()).clientDefault { java.time.Instant.now() }
}

class Contact(id: EntityID<Long>) : Entity<Long>(id) {
    companion object : EntityClass<Long, Contact>(Contacts)

    var ownerId by Contacts.ownerId
    var userId by Contacts.userId
    var phoneNumber by Contacts.phoneNumber
    var displayName by Contacts.displayName
    var profilePicture by Contacts.profilePicture
    var isBlocked by Contacts.isBlocked
    var isMuted by Contacts.isMuted
    var isStarred by Contacts.isStarred
    var addedAt by Contacts.addedAt
    var updatedAt by Contacts.updatedAt
}
