package com.qchat.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户模型
 */
@Serializable
data class User(
    @SerialName("id")
    val id: String,

    @SerialName("username")
    val username: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("phone")
    val phone: String? = null,

    @SerialName("bio")
    val bio: String? = null,

    @SerialName("is_online")
    val isOnline: Boolean = false,

    @SerialName("last_seen")
    val lastSeen: Long? = null,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("public_key")
    val publicKey: String? = null,

    @SerialName("is_verified")
    val isVerified: Boolean = false
)

/**
 * 预密钥包 - 用于X3DH密钥协商
 */
@Serializable
data class PreKeyBundle(
    @SerialName("user_id")
    val userId: String,

    @SerialName("device_id")
    val deviceId: String,

    @SerialName("registration_id")
    val registrationId: Int,

    @SerialName("pre_key_id")
    val preKeyId: Int,

    @SerialName("pre_key_public")
    val preKeyPublic: String, // Base64编码

    @SerialName("signed_pre_key_id")
    val signedPreKeyId: Int,

    @SerialName("signed_pre_key_public")
    val signedPreKeyPublic: String, // Base64编码

    @SerialName("signed_pre_key_signature")
    val signedPreKeySignature: String, // Base64编码

    @SerialName("identity_key")
    val identityKey: String // Base64编码
)

/**
 * 一次性预密钥
 */
@Serializable
data class PreKey(
    @SerialName("id")
    val id: Int,
    @SerialName("public_key")
    val publicKey: String // Base64编码
)

/**
 * 签名预密钥
 */
@Serializable
data class SignedPreKey(
    @SerialName("id")
    val id: Int,
    @SerialName("public_key")
    val publicKey: String, // Base64编码
    @SerialName("signature")
    val signature: String // Base64编码
)

/**
 * 设备信息
 */
@Serializable
data class DeviceInfo(
    @SerialName("device_id")
    val deviceId: String,

    @SerialName("device_name")
    val deviceName: String,

    @SerialName("platform")
    val platform: String,

    @SerialName("app_version")
    val appVersion: String,

    @SerialName("is_primary")
    val isPrimary: Boolean = false,

    @SerialName("last_seen")
    val lastSeen: Long? = null,

    @SerialName("created_at")
    val createdAt: Long
)

/**
 * 密钥材料
 */
@Serializable
data class KeyMaterial(
    @SerialName("identity_key_private")
    val identityKeyPrivate: String, // Base64编码

    @SerialName("identity_key_public")
    val identityKeyPublic: String, // Base64编码

    @SerialName("signed_pre_key_private")
    val signedPreKeyPrivate: String, // Base64编码

    @SerialName("signed_pre_key_public")
    val signedPreKeyPublic: String, // Base64编码

    @SerialName("signed_pre_key_signature")
    val signedPreKeySignature: String, // Base64编码

    @SerialName("pre_keys")
    val preKeys: List<PreKey>
)

/**
 * 会话状态
 */
@Serializable
data class SessionState(
    @SerialName("session_id")
    val sessionId: String,

    @SerialName("remote_user_id")
    val remoteUserId: String,

    @SerialName("remote_device_id")
    val remoteDeviceId: String,

    @SerialName("root_key")
    val rootKey: String, // Base64编码

    @SerialName("chain_key")
    val chainKey: String, // Base64编码

    @SerialName("sending_chain_key")
    val sendingChainKey: String? = null, // Base64编码

    @SerialName("receiving_chain_key")
    val receivingChainKey: String? = null, // Base64编码

    @SerialName("sending_ratchet_key")
    val sendingRatchetKey: String? = null, // Base64编码

    @SerialName("receiving_ratchet_key")
    val receivingRatchetKey: String? = null, // Base64编码

    @SerialName("sending_chain_index")
    val sendingChainIndex: Int = 0,

    @SerialName("receiving_chain_index")
    val receivingChainIndex: Int = 0,

    @SerialName("previous_chain_length")
    val previousChainLength: Int = 0,

    @SerialName("message_number")
    val messageNumber: Int = 0,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("updated_at")
    val updatedAt: Long
)

/**
 * X3DH密钥协商结果
 */
@Serializable
data class X3DHResult(
    @SerialName("session_id")
    val sessionId: String,

    @SerialName("shared_secret")
    val sharedSecret: String, // Base64编码的派生共享密钥

    @SerialName("init_message")
    val initMessage: String, // Base64编码的初始化消息

    @SerialName("used_pre_key_id")
    val usedPreKeyId: Int
)
