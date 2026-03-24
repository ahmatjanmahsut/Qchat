package com.qchat.android.crypto

import android.util.Base64
import com.qchat.android.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPublicKey

/**
 * 统一加密服务管理器
 *
 * 整合X3DH、Double Ratchet、AES-256-GCM和Ed25519签名，
 * 提供端到端加密的完整解决方案。
 */
class EncryptionServiceManager(
    private val keyStoreService: KeyStoreService,
    private val x3dh: X3DHKeyAgreement,
    private val doubleRatchet: DoubleRatchet,
    private val aesEncryption: AESEncryptionService,
    private val signatureService: Ed25519SignatureService
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 加密消息
     *
     * @param message 原始消息
     * @param sessionState 会话状态
     * @param senderIdentityKey 发送方身份密钥
     * @return 加密结果
     */
    fun encryptMessage(
        message: Message,
        sessionState: SessionState,
        senderIdentityKey: IdentityKeyPair
    ): EncryptedMessageResult {
        // 序列化消息内容
        val messageJson = json.encodeToString(message.content)
        val plaintext = messageJson.toByteArray(Charsets.UTF_8)

        // 获取或创建Ratchet状态
        var ratchetState = getRatchetState(sessionState)

        // 加密消息
        val (newRatchetState, encrypted) = doubleRatchet.encrypt(ratchetState, plaintext)

        // 生成HMAC
        val hmac = aesEncryption.generateHMAC(
            encrypted.ciphertext,
            sessionState.rootKey.toByteArray(Charsets.UTF_8)
        )

        // 对消息签名
        val signature = signatureService.signMessage(
            encrypted.ciphertext,
            senderIdentityKey.privateKey.serialize(),
            senderIdentityKey.publicKey.serialize()
        )

        // 构建加密元数据
        val encryptionMetadata = EncryptionMetadata(
            sessionId = sessionState.sessionId,
            messageKeyId = encrypted.messageKeyId,
            ratchetState = doubleRatchet.serializeState(newRatchetState),
            chainIndex = encrypted.chainIndex,
            previousChainLength = encrypted.previousChainLength,
            hmac = Base64.encodeToString(hmac, Base64.NO_WRAP),
            signature = Base64.encodeToString(signature, Base64.NO_WRAP)
        )

        // 构建加密消息
        val encryptedMessage = EncryptedMessage(
            id = message.id,
            sender = message.senderId,
            recipient = message.chatId,
            encryptedContent = Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP),
            encryptionMetadata = encryptionMetadata,
            timestamp = message.timestamp,
            type = message.type
        )

        return EncryptedMessageResult(
            encryptedMessage = encryptedMessage,
            newRatchetState = newRatchetState
        )
    }

    /**
     * 解密消息
     *
     * @param encryptedMessage 加密消息
     * @param sessionState 会话状态
     * @param senderIdentityKey 发送方身份公钥
     * @return 解密后的消息
     */
    fun decryptMessage(
        encryptedMessage: EncryptedMessage,
        sessionState: SessionState,
        senderIdentityKey: ECPublicKey
    ): DecryptedMessageResult {
        // 获取Ratchet状态
        var ratchetState = getRatchetState(sessionState)

        // 解析加密内容
        val ciphertext = Base64.decode(encryptedMessage.encryptedContent, Base64.NO_WRAP)

        // 验证HMAC
        val expectedHmac = Base64.decode(encryptedMessage.encryptionMetadata.hmac, Base64.NO_WRAP)
        val hmacValid = aesEncryption.verifyHMAC(
            ciphertext,
            sessionState.rootKey.toByteArray(Charsets.UTF_8),
            expectedHmac
        )

        if (!hmacValid) {
            return DecryptedMessageResult(
                success = false,
                error = "HMAC verification failed"
            )
        }

        // 验证签名
        if (encryptedMessage.encryptionMetadata.signature != null) {
            val signature = Base64.decode(encryptedMessage.encryptionMetadata.signature, Base64.NO_WRAP)
            val signatureValid = signatureService.verifyMessageSignature(
                ciphertext,
                signature,
                senderIdentityKey.serialize()
            )

            if (!signatureValid) {
                return DecryptedMessageResult(
                    success = false,
                    error = "Signature verification failed"
                )
            }
        }

        // 构建加密消息结构
        val encrypted = DoubleRatchet.EncryptedMessage(
            ciphertext = ciphertext,
            messageKeyId = encryptedMessage.encryptionMetadata.messageKeyId,
            chainIndex = encryptedMessage.encryptionMetadata.chainIndex,
            messageNumber = 0,
            previousChainLength = encryptedMessage.encryptionMetadata.previousChainLength,
            ephemeralKey = null
        )

        // 解密
        val (newRatchetState, decrypted) = doubleRatchet.decrypt(ratchetState, encrypted)

        // 反序列化消息内容
        val messageJson = String(decrypted.plaintext, Charsets.UTF_8)
        val messageContent = json.decodeFromString<MessageContent>(messageJson)

        return DecryptedMessageResult(
            success = true,
            content = messageContent,
            newRatchetState = newRatchetState
        )
    }

    /**
     * 建立新的加密会话
     */
    fun establishSession(
        myIdentityKeyPair: IdentityKeyPair,
        theirPreKeyBundle: PreKeyBundle,
        initialMessage: ByteArray
    ): SessionEstablishmentResult {
        // 解析对方的密钥
        val theirIdentityKey = Curve.decodePoint(
            Base64.decode(theirPreKeyBundle.identityKey, Base64.NO_WRAP),
            0
        )
        val theirSignedPreKey = Curve.decodePoint(
            Base64.decode(theirPreKeyBundle.signedPreKeyPublic, Base64.NO_WRAP),
            0
        )
        val theirPreKey = theirPreKeyBundle.preKeyPublic.let {
            if (it.isNotEmpty()) {
                Curve.decodePoint(Base64.decode(it, Base64.NO_WRAP), 0)
            } else null
        }

        // 执行X3DH密钥协商
        val x3dhResult = x3dh.initiateSession(
            myIdentityKeyPair = myIdentityKeyPair as ECKeyPair,
            theirIdentityKey = theirIdentityKey,
            theirSignedPreKey = theirSignedPreKey,
            theirPreKey = theirPreKey,
            initialMessage = initialMessage
        )

        // 初始化Double Ratchet
        val sharedSecret = Base64.decode(x3dhResult.sharedSecret, Base64.NO_WRAP)
        val sessionState = doubleRatchet.initializeSender(
            sessionId = x3dhResult.sessionId,
            remoteUserId = theirPreKeyBundle.userId,
            remoteDeviceId = theirPreKeyBundle.deviceId,
            sharedSecret = sharedSecret,
            theirRatchetKey = theirSignedPreKey
        )

        return SessionEstablishmentResult(
            sessionId = x3dhResult.sessionId,
            sessionState = sessionState,
            initMessage = x3dhResult.initMessage,
            usedPreKeyId = x3dhResult.usedPreKeyId
        )
    }

    /**
     * 从X3DH初始化消息响应建立会话
     */
    fun respondToSession(
        myIdentityKeyPair: IdentityKeyPair,
        mySignedPreKey: ECKeyPair,
        myPreKey: ECKeyPair?,
        initMessageData: String
    ): ByteArray {
        val initMessage = parseInitMessage(Base64.decode(initMessageData, Base64.NO_WRAP))

        val theirIdentityKey = Curve.decodePoint(initMessage.identityKey, 0)
        val theirEphemeralKey = Curve.decodePoint(initMessage.ephemeralKey, 0)

        return x3dh.respondToSession(
            myIdentityKeyPair = myIdentityKeyPair as ECKeyPair,
            mySignedPreKey = mySignedPreKey,
            myPreKey = myPreKey,
            theirIdentityKey = theirIdentityKey,
            theirEphemeralKey = theirEphemeralKey
        )
    }

    /**
     * 获取或创建Ratchet状态
     */
    private fun getRatchetState(sessionState: SessionState): DoubleRatchet.RatchetState {
        return doubleRatchet.deserializeState(sessionState.ratchetState)
            ?: throw IllegalStateException("Failed to deserialize ratchet state")
    }

    /**
     * 解析X3DH初始化消息
     */
    private fun parseInitMessage(data: ByteArray): X3DHKeyAgreement.InitMessage {
        val buffer = java.nio.ByteBuffer.wrap(data)

        val identityKeyLen = buffer.int
        val identityKey = ByteArray(identityKeyLen)
        buffer.get(identityKey)

        val ephemeralKeyLen = buffer.int
        val ephemeralKey = ByteArray(ephemeralKeyLen)
        buffer.get(ephemeralKey)

        val usedPreKeyId = buffer.int
        val signedPreKeyId = buffer.int

        val encryptedMessageLen = buffer.int
        val encryptedMessage = ByteArray(encryptedMessageLen)
        buffer.get(encryptedMessage)

        return X3DHKeyAgreement.InitMessage(
            identityKey = identityKey,
            ephemeralKey = ephemeralKey,
            usedPreKeyId = usedPreKeyId,
            signedPreKeyId = signedPreKeyId,
            encryptedMessage = encryptedMessage
        )
    }

    /**
     * 验证密钥指纹
     */
    fun verifyKeyFingerprint(localPublicKey: ByteArray, remotePublicKey: ByteArray): Boolean {
        val localFingerprint = signatureService.getKeyFingerprint(localPublicKey)
        val remoteFingerprint = signatureService.getKeyFingerprint(remotePublicKey)
        return localFingerprint == remoteFingerprint
    }

    /**
     * 生成会话状态摘要
     */
    fun generateSessionSummary(sessionState: SessionState): String {
        val ratchetState = getRatchetState(sessionState)
        return "Session: ${sessionState.sessionId}\n" +
                "Chain Index: ${ratchetState.sendingChainIndex}\n" +
                "Message Number: ${ratchetState.messageNumber}"
    }
}

/**
 * 加密结果
 */
data class EncryptedMessageResult(
    val encryptedMessage: EncryptedMessage,
    val newRatchetState: DoubleRatchet.RatchetState
)

/**
 * 解密结果
 */
data class DecryptedMessageResult(
    val success: Boolean,
    val content: MessageContent? = null,
    val newRatchetState: DoubleRatchet.RatchetState? = null,
    val error: String? = null
)

/**
 * 会话建立结果
 */
data class SessionEstablishmentResult(
    val sessionId: String,
    val sessionState: DoubleRatchet.RatchetState,
    val initMessage: String,
    val usedPreKeyId: Int
)

/**
 * 会话状态（持久化用）
 */
data class SessionState(
    val sessionId: String,
    val remoteUserId: String,
    val remoteDeviceId: String,
    val rootKey: String,
    val chainKey: String,
    val sendingChainKey: String?,
    val receivingChainKey: String?,
    val sendingRatchetKey: String?,
    val receivingRatchetKey: String?,
    val sendingChainIndex: Int,
    val receivingChainIndex: Int,
    val previousChainLength: Int,
    val messageNumber: Int,
    val ratchetState: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun fromRatchetState(ratchetState: DoubleRatchet.RatchetState): SessionState {
            return SessionState(
                sessionId = ratchetState.sessionId,
                remoteUserId = ratchetState.remoteUserId,
                remoteDeviceId = ratchetState.remoteDeviceId,
                rootKey = android.util.Base64.encodeToString(ratchetState.rootKey, android.util.Base64.NO_WRAP),
                chainKey = android.util.Base64.encodeToString(ratchetState.sendingChainKey ?: ByteArray(32), android.util.Base64.NO_WRAP),
                sendingChainKey = ratchetState.sendingChainKey?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) },
                receivingChainKey = ratchetState.receivingChainKey?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) },
                sendingRatchetKey = ratchetState.sendingRatchetKey?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) },
                receivingRatchetKey = ratchetState.receivingRatchetKey?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) },
                sendingChainIndex = ratchetState.sendingChainIndex,
                receivingChainIndex = ratchetState.receivingChainIndex,
                previousChainLength = ratchetState.previousChainLength,
                messageNumber = ratchetState.messageNumber,
                ratchetState = "",
                createdAt = ratchetState.createdAt,
                updatedAt = ratchetState.updatedAt
            )
        }
    }
}
