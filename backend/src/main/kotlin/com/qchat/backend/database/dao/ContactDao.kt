package com.qchat.backend.database.dao

import com.qchat.backend.database.entity.Contact
import com.qchat.backend.database.entity.Contacts
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update

/**
 * 联系人数据访问对象
 */
class ContactDao {

    fun findById(id: Long): Contact? = Contact.findById(id)

    fun findByOwnerAndUser(ownerId: Long, userId: Long): Contact? =
        Contact.find {
            Contacts.ownerId eq ownerId and (Contacts.userId eq userId)
        }.firstOrNull()

    fun findByPhoneNumber(ownerId: Long, phoneNumber: String): Contact? =
        Contact.find {
            Contacts.ownerId eq ownerId and (Contacts.phoneNumber eq phoneNumber)
        }.firstOrNull()

    fun findByOwnerId(ownerId: Long): List<Contact> =
        Contact.find { Contacts.ownerId eq ownerId }.toList()

    fun findBlockedByOwnerId(ownerId: Long): List<Contact> =
        Contact.find {
            Contacts.ownerId eq ownerId and (Contacts.isBlocked eq true)
        }.toList()

    fun findStarredByOwnerId(ownerId: Long): List<Contact> =
        Contact.find {
            Contacts.ownerId eq ownerId and (Contacts.isStarred eq true)
        }.toList()

    fun search(ownerId: Long, query: String): List<Contact> =
        Contact.find {
            Contacts.ownerId eq ownerId and (
                Contacts.displayName like "%$query%" or
                    Contacts.phoneNumber like "%$query%"
            )
        }.toList()

    fun create(
        ownerId: Long,
        userId: Long,
        phoneNumber: String,
        displayName: String,
        profilePicture: String? = null
    ): Contact = Contact.new {
        this.ownerId = ownerId
        this.userId = userId
        this.phoneNumber = phoneNumber
        this.displayName = displayName
        this.profilePicture = profilePicture
    }

    fun update(
        contactId: Long,
        displayName: String? = null,
        profilePicture: String? = null
    ): Contact? {
        val contact = findById(contactId) ?: return null
        transaction(contact) {
            displayName?.let { contact.displayName = it }
            profilePicture?.let { contact.profilePicture = it }
        }
        return contact
    }

    fun setBlocked(contactId: Long, blocked: Boolean): Boolean {
        val updated = Contacts.update({ Contacts.id eq contactId }) {
            it[Contacts.isBlocked] = blocked
        }
        return updated > 0
    }

    fun setMuted(contactId: Long, muted: Boolean): Boolean {
        val updated = Contacts.update({ Contacts.id eq contactId }) {
            it[Contacts.isMuted] = muted
        }
        return updated > 0
    }

    fun setStarred(contactId: Long, starred: Boolean): Boolean {
        val updated = Contacts.update({ Contacts.id eq contactId }) {
            it[Contacts.isStarred] = starred
        }
        return updated > 0
    }

    fun delete(contactId: Long): Boolean {
        val contact = findById(contactId) ?: return false
        transaction(contact) { contact.delete() }
        return true
    }

    fun getContactCount(ownerId: Long): Int =
        Contact.find { Contacts.ownerId eq ownerId }.count().toInt()
}
