package com.qchat.android.crypto

import android.util.Base64
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPublicKey
import java.security.MessageDigest
import javax.crypto.KeyAgreement
import javax.crypto.spec.DHParameterSpec

/**
 * X3DH密钥协商协议实现
 *
 * X3DH (Extended Triple Diffie-Hellman) 是一种密钥协商协议，
 * 用于在两个用户之间安全地建立共享密钥。
 *
 * 密钥交换流程：
 * 1. 发起方获取接收方的预密钥包
 * 2. 发起方生成临时密钥对 (EKB, EKA)
 * 3. 计算4个Diffie-Hellman密钥共享：
 *    - DH1 = DH(IKB, SPKB)  身份密钥与签名预密钥
 *    - DH2 = DH(EKA, SPKB)  临时密钥与签名预密钥
 *    - DH3 = DH(EKA, IKB)   临时密钥与身份密钥
 *    - DH4 = DH(EKA, PKB)   临时密钥与预密钥
 * 4. 使用HKDF从DH1||DH2||DH3||DH4派生共享密钥
 * 5. 发送初始化消息给接收方
 */
class X3DHKeyAgreement {

    companion object {
        private const val CURVE25519_KEY_SIZE = 255
        private const val DH_PARAMETER_Q = "57896044618658097711785492504343953926634992332820282019728792003956564821047"
        private const val DH_PARAMETER_P = "57896044618658097711785492504343953926634992332820282019728792003956564821041"
        private const val HKDF_INFO = "QchatX3DH"
        private const val MESSAGE_KEY_LENGTH = 32
        private const val CHAIN_KEY_LENGTH = 32
    }

    /**
     * X3DH初始化消息
     */
    data class InitMessage(
        val identityKey: ByteArray,        // 发起方的身份公钥
        val ephemeralKey: ByteArray,       // 发起方的临时公钥
        val usedPreKeyId: Int,             // 使用的一次性预密钥ID
        val signedPreKeyId: Int,           // 使用的签名预密钥ID
        val encryptedMessage: ByteArray    // 使用共享密钥加密的初始消息
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return identityKey.contentEquals((other as InitMessage).identityKey) &&
                    ephemeralKey.contentEquals(other.ephemeralKey) &&
                    usedPreKeyId == other.usedPreKeyId &&
                    signedPreKeyId == other.signedPreKeyId &&
                    encryptedMessage.contentEquals(other.encryptedMessage)
        }

        override fun hashCode(): Int {
            var result = identityKey.contentHashCode()
            result = 31 * result + ephemeralKey.contentHashCode()
            result = 31 * result + usedPreKeyId
            result = 31 * result + signedPreKeyId
            result = 31 * result + encryptedMessage.contentHashCode()
            return result
        }
    }

    /**
     * 发起方：建立X3DH会话
     *
     * @param myIdentityKeyPair 发起方的身份密钥对
     * @param theirIdentityKey 接收方的身份公钥
     * @param theirSignedPreKey 接收方的签名预密钥公钥
     * @param theirPreKey 接收方的一次性预密钥公钥（可选）
     * @param initialMessage 初始消息
     * @return X3DH结果包含初始化消息和共享密钥
     */
    fun initiateSession(
        myIdentityKeyPair: ECKeyPair,
        theirIdentityKey: ECPublicKey,
        theirSignedPreKey: ECPublicKey,
        theirPreKey: ECPublicKey?,
        initialMessage: ByteArray
    ): X3DHResult {
        // 1. 生成临时密钥对
        val ephemeralKeyPair = Curve.generateKeyPair()

        // 2. 计算4个DH共享密钥
        // DH1 = DH(IKB, SPKB)
        val dh1 = Curve.calculateAgreement(theirSignedPreKey, myIdentityKeyPair.privateKey)

        // DH2 = DH(EKA, SPKB)
        val dh2 = Curve.calculateAgreement(theirSignedPreKey, ephemeralKeyPair.privateKey)

        // DH3 = DH(EKA, IKB)
        val dh3 = Curve.calculateAgreement(theirIdentityKey, ephemeralKeyPair.privateKey)

        // 3. 组合 DH 输出
        // 如果有一性预密钥，使用 4 个 DH 输出；否则使用 3 个 DH 输出
        val combinedDHSecret = if (theirPreKey != null) {
            // DH4 = DH(EKA, PKB)
            val dh4 = Curve.calculateAgreement(theirPreKey, ephemeralKeyPair.privateKey)
            combineDHOutputs(dh1, dh2, dh3, dh4)
        } else {
            // 没有预密钥时，只使用 3 个 DH 输出（不使用零值）
            combineDHOutputs(dh1, dh2, dh3)
        }

        // 4. 使用HKDF派生共享密钥
        val sharedSecret = hkdf(combinedDHSecret, myIdentityKeyPair.publicKey.serialize())

        // 5. 创建初始化消息
        val initMessage = InitMessage(
            identityKey = myIdentityKeyPair.publicKey.serialize(),
            ephemeralKey = ephemeralKeyPair.publicKey.serialize(),
            usedPreKeyId = 0, // 将在后面设置
            signedPreKeyId = 0, // 将在后面设置
            encryptedMessage = encryptWithSharedKey(initialMessage, sharedSecret)
        )

        // 6. 生成会话ID
        val sessionId = generateSessionId(theirIdentityKey.serialize(), ephemeralKeyPair.publicKey.serialize())

        return X3DHResult(
            sessionId = sessionId,
            sharedSecret = Base64.encodeToString(sharedSecret, Base64.NO_WRAP),
            initMessage = Base64.encodeToString(serializeInitMessage(initMessage), Base64.NO_WRAP),
            usedPreKeyId = initMessage.usedPreKeyId
        )
    }

    /**
     * 接收方：响应X3DH初始化
     *
     * @param myIdentityKeyPair 接收方的身份密钥对
     * @param mySignedPreKey 接收方的签名预密钥私钥
     * @param myPreKey 接收方的一次性预密钥私钥（如果使用）
     * @param theirIdentityKey 发起方的身份公钥
     * @param theirEphemeralKey 发起方的临时公钥
     * @return 派生的共享密钥
     */
    fun respondToSession(
        myIdentityKeyPair: ECKeyPair,
        mySignedPreKey: ECKeyPair,
        myPreKey: ECKeyPair?,
        theirIdentityKey: ECPublicKey,
        theirEphemeralKey: ECPublicKey
    ): ByteArray {
        // 计算 DH 共享密钥（接收方的角色相反）
        // DH1 = DH(SPKB, IKA)
        val dh1 = Curve.calculateAgreement(theirIdentityKey, mySignedPreKey.privateKey)

        // DH3 = DH(IKB, EKA)
        val dh3 = Curve.calculateAgreement(theirEphemeralKey, myIdentityKeyPair.privateKey)

        // DH4 = DH(SPKB, EKA)
        val dh4 = Curve.calculateAgreement(theirEphemeralKey, mySignedPreKey.privateKey)

        // 组合 DH 输出
        // 如果有一性预密钥，使用 4 个 DH 输出；否则使用 3 个 DH 输出
        val combinedDHSecret = if (myPreKey != null) {
            // DH2 = DH(PKB, EKA)
            val dh2 = Curve.calculateAgreement(theirEphemeralKey, myPreKey.privateKey)
            combineDHOutputs(dh1, dh2, dh3, dh4)
        } else {
            // 没有预密钥时，只使用 3 个 DH 输出（不使用零值）
            combineDHOutputs(dh1, dh3, dh4)
        }

        // 使用 HKDF 派生共享密钥
        return hkdf(combinedDHSecret, theirIdentityKey.serialize())
    }

    /**
     * 组合多个DH输出
     */
    private fun combineDHOutputs(vararg outputs: ByteArray): ByteArray {
        val combined = ByteArray(outputs.sumOf { it.size })
        var offset = 0
        for (output in outputs) {
            System.arraycopy(output, 0, combined, offset, output.size)
            offset += output.size
        }
        return combined
    }

    /**
     * HKDF 密钥派生函数 (RFC 5869)
     * 使用标准 HMAC-SHA256 实现
     */
    private fun hkdf(inputKeyMaterial: ByteArray, info: ByteArray): ByteArray {
        // Extract 阶段：PRK = HMAC-Hash(salt, IKM)
        val salt = ByteArray(32) // 使用 SHA-256 输出长度作为 salt
        SecureRandom().nextBytes(salt)
        val prk = hmacSha256(salt, inputKeyMaterial)

        // Expand 阶段：OKM = T(1) | T(2) | T(3) | ...
        val outputLength = MESSAGE_KEY_LENGTH + CHAIN_KEY_LENGTH
        val n = (outputLength + 31) / 32 // 需要迭代的次数
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
     * 使用共享密钥加密消息
     */
    private fun encryptWithSharedKey(message: ByteArray, sharedKey: ByteArray): ByteArray {
        // 使用AES-256-GCM加密
        val keySpec = SecretKeySpec(sharedKey.copyOfRange(0, 32), "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)

        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val encrypted = cipher.doFinal(message)

        // 组合IV + 密文
        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)

        return result
    }

    /**
     * 生成会话ID
     */
    private fun generateSessionId(vararg parts: ByteArray): String {
        val combined = ByteArray(parts.sumOf { it.size })
        var offset = 0
        for (part in parts) {
            System.arraycopy(part, 0, combined, offset, part.size)
            offset += part.size
        }
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.encodeToString(digest.digest(combined), Base64.NO_WRAP)
    }

    /**
     * 序列化初始化消息
     */
    private fun serializeInitMessage(msg: InitMessage): ByteArray {
        // 格式：[identityKeyLen(4)][identityKey][ephemeralKeyLen(4)][ephemeralKey]
        //      [usedPreKeyId(4)][signedPreKeyId(4)][encryptedMessageLen(4)][encryptedMessage]
        val identityKeyLen = ByteBuffer.allocate(4).putInt(msg.identityKey.size).array()
        val ephemeralKeyLen = ByteBuffer.allocate(4).putInt(msg.ephemeralKey.size).array()
        val usedPreKeyId = ByteBuffer.allocate(4).putInt(msg.usedPreKeyId).array()
        val signedPreKeyId = ByteBuffer.allocate(4).putInt(msg.signedPreKeyId).array()
        val encryptedMessageLen = ByteBuffer.allocate(4).putInt(msg.encryptedMessage.size).array()

        return ByteArray(
            identityKeyLen.size + msg.identityKey.size +
            ephemeralKeyLen.size + msg.ephemeralKey.size +
            usedPreKeyId.size + signedPreKeyId.size +
            encryptedMessageLen.size + msg.encryptedMessage.size
        ).apply {
            var offset = 0
            System.arraycopy(identityKeyLen, 0, this, offset, identityKeyLen.size)
            offset += identityKeyLen.size
            System.arraycopy(msg.identityKey, 0, this, offset, msg.identityKey.size)
            offset += msg.identityKey.size
            System.arraycopy(ephemeralKeyLen, 0, this, offset, ephemeralKeyLen.size)
            offset += ephemeralKeyLen.size
            System.arraycopy(msg.ephemeralKey, 0, this, offset, msg.ephemeralKey.size)
            offset += msg.ephemeralKey.size
            System.arraycopy(usedPreKeyId, 0, this, offset, usedPreKeyId.size)
            offset += usedPreKeyId.size
            System.arraycopy(signedPreKeyId, 0, this, offset, signedPreKeyId.size)
            offset += signedPreKeyId.size
            System.arraycopy(encryptedMessageLen, 0, this, offset, encryptedMessageLen.size)
            offset += encryptedMessageLen.size
            System.arraycopy(msg.encryptedMessage, 0, this, offset, msg.encryptedMessage.size)
        }
    }
}

import java.nio.ByteBuffer

/**
 * X3DH结果
 */
data class X3DHResult(
    val sessionId: String,
    val sharedSecret: String,
    val initMessage: String,
    val usedPreKeyId: Int
)
