package com.qchat.android.crypto

import android.util.Base64
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPublicKey
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

/**
 * Double Ratchet算法实现
 *
 * Double Ratchet算法提供前向保密性和混合转发保密性。
 * 它结合了Symmetric Ratchet和DH Ratchet：
 * - Symmetric Ratchet: 每个消息使用从链密钥派生的不同消息密钥
 * - DH Ratchet: 定期用新的DH密钥对更新密钥对
 *
 * 每次消息加密/解密后，链密钥都会更新，确保即使密钥泄露，
 * 过去的消息仍然安全（前向保密）。
 */
class DoubleRatchet {

    companion object {
        private const val MESSAGE_KEY_LENGTH = 32
        private const val CHAIN_KEY_LENGTH = 32
        private const val DH_KEY_LENGTH = 32
        private const val ROOT_KEY_LENGTH = 32
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val MAX_CHAIN_LENGTH = 1000 // 防止无限链
    }

    /**
     * Ratchet状态
     */
    data class RatchetState(
        val sessionId: String,
        val remoteUserId: String,
        val remoteDeviceId: String,

        // 根密钥
        val rootKey: ByteArray,

        // 发送链密钥（用于发送消息）
        val sendingChainKey: ByteArray? = null,

        // 接收链密钥（用于接收消息）
        val receivingChainKey: ByteArray? = null,

        // 发送DH密钥对（当前轮次）
        val sendingRatchetKey: ByteArray? = null,

        // 接收DH公钥（远程最新）
        val receivingRatchetKey: ByteArray? = null,

        // 发送链索引
        val sendingChainIndex: Int = 0,

        // 接收链索引
        val receivingChainIndex: Int = 0,

        // 前一个链长度（用于完整前向保密）
        val previousChainLength: Int = 0,

        // 消息编号
        val messageNumber: Int = 0,

        // 跳过消息密钥（用于乱序接收）
        val skippedMessageKeys: Map<Pair<Int, Int>, ByteArray> = emptyMap(),

        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return sessionId == (other as RatchetState).sessionId
        }

        override fun hashCode(): Int = sessionId.hashCode()
    }

    /**
     * 加密结果
     */
    data class EncryptedMessage(
        val ciphertext: ByteArray,      // 加密内容（包含IV）
        val messageKeyId: String,       // 消息密钥ID
        val chainIndex: Int,            // 链索引
        val messageNumber: Int,         // 消息编号
        val previousChainLength: Int,   // 前一个链长度
        val ephemeralKey: ByteArray?    // 如果执行了DH ratchet，包含新的临时密钥
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return Base64.encodeToString(ciphertext, Base64.NO_WRAP) ==
                    Base64.encodeToString((other as EncryptedMessage).ciphertext, Base64.NO_WRAP)
        }

        override fun hashCode(): Int = Base64.encodeToString(ciphertext, Base64.NO_WRAP).hashCode()
    }

    /**
     * 解密结果
     */
    data class DecryptedMessage(
        val plaintext: ByteArray,
        val messageKeyId: String,
        val chainIndex: Int,
        val messageNumber: Int
    )

    /**
     * 初始化发送方会话
     */
    fun initializeSender(
        sessionId: String,
        remoteUserId: String,
        remoteDeviceId: String,
        sharedSecret: ByteArray,
        theirRatchetKey: ECPublicKey
    ): RatchetState {
        // 生成新的发送DH密钥对
        val sendingRatchetKeyPair = Curve.generateKeyPair()

        // 执行DH ratchet得到根密钥和链密钥
        val (newRootKey, sendingChainKey) = dhRatchet(
            sharedSecret,
            sendingRatchetKeyPair.privateKey,
            theirRatchetKey
        )

        return RatchetState(
            sessionId = sessionId,
            remoteUserId = remoteUserId,
            remoteDeviceId = remoteDeviceId,
            rootKey = newRootKey,
            sendingChainKey = sendingChainKey,
            sendingRatchetKey = sendingRatchetKeyPair.privateKey.serialize(),
            receivingRatchetKey = theirRatchetKey.serialize(),
            sendingChainIndex = 0,
            receivingChainIndex = 0,
            previousChainLength = 0
        )
    }

    /**
     * 初始化接收方会话
     */
    fun initializeReceiver(
        sessionId: String,
        remoteUserId: String,
        remoteDeviceId: String,
        sharedSecret: ByteArray,
        myRatchetKeyPair: ECKeyPair
    ): RatchetState {
        return RatchetState(
            sessionId = sessionId,
            remoteUserId = remoteUserId,
            remoteDeviceId = remoteDeviceId,
            rootKey = sharedSecret,
            sendingRatchetKey = myRatchetKeyPair.privateKey.serialize(),
            receivingRatchetKey = null,
            sendingChainIndex = 0,
            receivingChainIndex = 0,
            previousChainLength = 0
        )
    }

    /**
     * 加密消息
     */
    fun encrypt(state: RatchetState, plaintext: ByteArray): Pair<RatchetState, EncryptedMessage> {
        var currentState = state

        // 获取当前消息密钥
        val (messageKey, newChainKey) = deriveMessageKey(currentState.sendingChainKey!!)
        currentState = currentState.copy(sendingChainKey = newChainKey)

        // 生成消息密钥ID
        val messageKeyId = generateMessageKeyId(messageKey)

        // 加密消息
        val iv = ByteArray(GCM_IV_LENGTH)
        java.security.SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(messageKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext)

        // 组合IV + 密文
        val combinedCiphertext = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combinedCiphertext, 0, iv.size)
        System.arraycopy(ciphertext, 0, combinedCiphertext, iv.size, ciphertext.size)

        // 更新状态
        val newState = currentState.copy(
            sendingChainIndex = currentState.sendingChainIndex + 1,
            messageNumber = currentState.messageNumber + 1,
            updatedAt = System.currentTimeMillis()
        )

        val encryptedMessage = EncryptedMessage(
            ciphertext = combinedCiphertext,
            messageKeyId = messageKeyId,
            chainIndex = currentState.sendingChainIndex,
            messageNumber = currentState.messageNumber,
            previousChainLength = currentState.previousChainLength,
            ephemeralKey = null
        )

        return Pair(newState, encryptedMessage)
    }

    /**
     * 解密消息
     */
    fun decrypt(state: RatchetState, encryptedMessage: EncryptedMessage): Pair<RatchetState, DecryptedMessage> {
        var currentState = state

        // 检查是否有跳过的消息密钥
        val skippedKey = currentState.skippedMessageKeys[Pair(encryptedMessage.chainIndex, encryptedMessage.messageNumber)]
        if (skippedKey != null) {
            // 使用跳过的密钥解密
            val newSkippedKeys = currentState.skippedMessageKeys.toMutableMap()
            newSkippedKeys.remove(Pair(encryptedMessage.chainIndex, encryptedMessage.messageNumber))

            val decrypted = decryptWithKey(encryptedMessage.ciphertext, skippedKey)
            return Pair(
                currentState.copy(skippedMessageKeys = newSkippedKeys, updatedAt = System.currentTimeMillis()),
                DecryptedMessage(
                    plaintext = decrypted,
                    messageKeyId = encryptedMessage.messageKeyId,
                    chainIndex = encryptedMessage.chainIndex,
                    messageNumber = encryptedMessage.messageNumber
                )
            )
        }

        // 检查是否需要执行DH ratchet
        if (encryptedMessage.ephemeralKey != null && currentState.receivingRatchetKey != null) {
            // 执行DH ratchet
            currentState = performDHRatchet(currentState, encryptedMessage.ephemeralKey)
        }

        // 从链密钥派生消息密钥
        val (messageKey, newChainKey) = deriveMessageKey(currentState.receivingChainKey!!)
        currentState = currentState.copy(receivingChainKey = newChainKey)

        // 解密消息
        val decrypted = decryptWithKey(encryptedMessage.ciphertext, messageKey)

        // 更新状态
        val newState = currentState.copy(
            receivingChainIndex = currentState.receivingChainIndex + 1,
            messageNumber = currentState.messageNumber + 1,
            updatedAt = System.currentTimeMillis()
        )

        return Pair(
            newState,
            DecryptedMessage(
                plaintext = decrypted,
                messageKeyId = encryptedMessage.messageKeyId,
                chainIndex = encryptedMessage.chainIndex,
                messageNumber = encryptedMessage.messageNumber
            )
        )
    }

    /**
     * 执行DH Ratchet
     */
    private fun performDHRatchet(state: RatchetState, theirNewRatchetKey: ByteArray): RatchetState {
        // 保存当前链信息
        val previousChainLength = state.sendingChainIndex + state.receivingChainIndex

        // 生成新的发送DH密钥对
        val newSendingKeyPair = Curve.generateKeyPair()

        // 从接收链密钥派生出接收链密钥（用于接收消息）
        val (newRootKey1, receivingChainKey) = dhRatchet(
            state.rootKey,
            newSendingKeyPair.privateKey,
            Curve.decodePoint(theirNewRatchetKey, 0)
        )

        // 从新的根密钥派发送链密钥（用于发送消息）
        val (newRootKey2, sendingChainKey) = dhRatchet(
            newRootKey1,
            newSendingKeyPair.privateKey,
            Curve.decodePoint(theirNewRatchetKey, 0)
        )

        return state.copy(
            rootKey = newRootKey2,
            sendingChainKey = sendingChainKey,
            receivingChainKey = receivingChainKey,
            sendingRatchetKey = newSendingKeyPair.privateKey.serialize(),
            receivingRatchetKey = theirNewRatchetKey,
            sendingChainIndex = 0,
            receivingChainIndex = 0,
            previousChainLength = previousChainLength
        )
    }

    /**
     * DH Ratchet步骤
     */
    private fun dhRatchet(
        rootKey: ByteArray,
        privateKey: org.signal.libsignal.protocol.ecc.ECPrivateKey,
        remotePublicKey: ECPublicKey
    ): Pair<ByteArray, ByteArray> {
        // 计算DH输出
        val dhOutput = Curve.calculateAgreement(remotePublicKey, privateKey)

        // 使用HKDF派生新的根密钥和链密钥
        return hkdf2(rootKey, dhOutput)
    }

    /**
     * 从链密钥派生消息密钥和新的链密钥
     */
    private fun deriveMessageKey(chainKey: ByteArray): Pair<ByteArray, ByteArray> {
        val info = "QchatMessageKey".toByteArray()
        val derived = hkdf(chainKey, info)

        val messageKey = derived.copyOfRange(0, MESSAGE_KEY_LENGTH)
        val newChainKey = derived.copyOfRange(MESSAGE_KEY_LENGTH, MESSAGE_KEY_LENGTH + CHAIN_KEY_LENGTH)

        return Pair(messageKey, newChainKey)
    }

    /**
     * HKDF 密钥派生函数 (RFC 5869)
     * 使用标准 HMAC-SHA256 实现
     */
    private fun hkdf(inputKeyMaterial: ByteArray, info: ByteArray): ByteArray {
        // Extract 阶段：PRK = HMAC-Hash(salt, IKM)
        val salt = ByteArray(32)
        java.security.SecureRandom().nextBytes(salt)
        val prk = hmacSha256(salt, inputKeyMaterial)

        // Expand 阶段：OKM = T(1) | T(2) | T(3) | ...
        val outputLength = MESSAGE_KEY_LENGTH + CHAIN_KEY_LENGTH
        val n = (outputLength + 31) / 32
        val okm = ByteArray(outputLength)
        
        var t = ByteArray(0)
        var counter: Byte = 1
        
        for (i in 0 until n) {
            // T(i) = HMAC-Hash(PRK, T(i-1) | info | i)
            val hmacInput = ByteArray(t.size + info.size + 1)
            System.arraycopy(t, 0, hmacInput, 0, t.size)
            System.arraycopy(info, 0, hmacInput, t.size, info.size)
            hmacInput[hmacInput.size - 1] = counter
            
            t = hmacSha256(prk, hmacInput)
            System.arraycopy(t, 0, okm, i * 32, minOf(32, outputLength - i * 32))
            counter++
        }

        return okm
    }

    /**
     * HMAC-SHA256 实现
     */
    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        val secretKey = javax.crypto.spec.SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data)
    }

    /**
     * HKDF2实现（用于根密钥派生）
     */
    private fun hkdf2(rootKey: ByteArray, dhOutput: ByteArray): Pair<ByteArray, ByteArray> {
        val combined = ByteArray(rootKey.size + dhOutput.size)
        System.arraycopy(rootKey, 0, combined, 0, rootKey.size)
        System.arraycopy(dhOutput, 0, combined, rootKey.size, dhOutput.size)

        val derived = hkdf(combined, "QchatRatchet".toByteArray())

        return Pair(
            derived.copyOfRange(0, ROOT_KEY_LENGTH),
            derived.copyOfRange(ROOT_KEY_LENGTH, ROOT_KEY_LENGTH + CHAIN_KEY_LENGTH)
        )
    }

    /**
     * 使用指定密钥解密
     */
    private fun decryptWithKey(ciphertext: ByteArray, key: ByteArray): ByteArray {
        val iv = ciphertext.copyOfRange(0, GCM_IV_LENGTH)
        val actualCiphertext = ciphertext.copyOfRange(GCM_IV_LENGTH, ciphertext.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(actualCiphertext)
    }

    /**
     * 生成消息密钥ID
     */
    private fun generateMessageKeyId(messageKey: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.encodeToString(digest.digest(messageKey), Base64.NO_WRAP)
    }

    /**
     * 跳过丢失的消息（用于处理乱序消息）
     */
    fun skipMessages(state: RatchetState, untilChainIndex: Int): RatchetState {
        if (state.receivingChainKey == null) return state

        var currentChainKey = state.receivingChainKey
        val newSkippedKeys = state.skippedMessageKeys.toMutableMap()

        // 跳过直到指定链索引的所有消息
        for (i in state.receivingChainIndex until min(untilChainIndex, MAX_CHAIN_LENGTH)) {
            val (messageKey, newChainKey) = deriveMessageKey(currentChainKey)
            newSkippedKeys[Pair(i, 0)] = messageKey
            currentChainKey = newChainKey
        }

        return state.copy(
            receivingChainKey = currentChainKey,
            receivingChainIndex = min(untilChainIndex, MAX_CHAIN_LENGTH),
            skippedMessageKeys = newSkippedKeys
        )
    }

    /**
     * 序列化Ratchet状态用于存储
     */
    fun serializeState(state: RatchetState): String {
        val data = buildString {
            append(state.sessionId).append("|")
            append(state.remoteUserId).append("|")
            append(state.remoteDeviceId).append("|")
            append(Base64.encodeToString(state.rootKey, Base64.NO_WRAP)).append("|")
            append(state.sendingChainKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: "").append("|")
            append(state.receivingChainKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: "").append("|")
            append(state.sendingRatchetKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: "").append("|")
            append(state.receivingRatchetKey?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: "").append("|")
            append(state.sendingChainIndex).append("|")
            append(state.receivingChainIndex).append("|")
            append(state.previousChainLength).append("|")
            append(state.messageNumber).append("|")
            append(state.createdAt).append("|")
            append(state.updatedAt)
        }
        return data
    }

    /**
     * 从序列化数据恢复Ratchet状态
     */
    fun deserializeState(data: String): RatchetState? {
        return try {
            val parts = data.split("|")
            if (parts.size < 15) return null

            RatchetState(
                sessionId = parts[0],
                remoteUserId = parts[1],
                remoteDeviceId = parts[2],
                rootKey = Base64.decode(parts[3], Base64.NO_WRAP),
                sendingChainKey = parts[4].ifEmpty { null }?.let { Base64.decode(it, Base64.NO_WRAP) },
                receivingChainKey = parts[5].ifEmpty { null }?.let { Base64.decode(it, Base64.NO_WRAP) },
                sendingRatchetKey = parts[6].ifEmpty { null }?.let { Base64.decode(it, Base64.NO_WRAP) },
                receivingRatchetKey = parts[7].ifEmpty { null }?.let { Base64.decode(it, Base64.NO_WRAP) },
                sendingChainIndex = parts[8].toInt(),
                receivingChainIndex = parts[9].toInt(),
                previousChainLength = parts[10].toInt(),
                messageNumber = parts[11].toInt(),
                createdAt = parts[12].toLong(),
                updatedAt = parts[13].toLong()
            )
        } catch (e: Exception) {
            null
        }
    }
}
