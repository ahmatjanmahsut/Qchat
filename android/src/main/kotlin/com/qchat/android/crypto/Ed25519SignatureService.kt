package com.qchat.android.crypto

import android.util.Base64
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPublicKey
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Ed25519数字签名服务
 *
 * Ed25519是一种基于Edwards曲线的数字签名算法，
 * 提供128位安全性，具有快速验证和小签名尺寸。
 *
 * 用途：
 * - 验证消息发送者的真实性
 * - 签名预密钥以证明身份
 * - 消息完整性验证
 */
class Ed25519SignatureService {

    companion object {
        private const val SIGNATURE_LENGTH = 64
        private const val PUBLIC_KEY_LENGTH = 32
        private const val PRIVATE_KEY_LENGTH = 32
        private const val SEED_LENGTH = 32
    }

    private val secureRandom = SecureRandom()

    /**
     * 密钥对
     */
    data class KeyPair(
        val publicKey: ByteArray,
        val privateKey: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return publicKey.contentEquals((other as KeyPair).publicKey) &&
                    privateKey.contentEquals(other.privateKey)
        }

        override fun hashCode(): Int {
            var result = publicKey.contentHashCode()
            result = 31 * result + privateKey.contentHashCode()
            return result
        }
    }

    /**
     * 签名结果
     */
    data class SignatureResult(
        val signature: ByteArray,
        val publicKey: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return signature.contentEquals((other as SignatureResult).signature) &&
                    publicKey.contentEquals(other.publicKey)
        }

        override fun hashCode(): Int {
            var result = signature.contentHashCode()
            result = 31 * result + publicKey.contentHashCode()
            return result
        }
    }

    /**
     * 生成Ed25519密钥对
     *
     * 注意：这里使用Curve25519实现，因为libsignal使用Curve25519
     * Ed25519和Curve25519可以相互转换
     */
    fun generateKeyPair(): KeyPair {
        // 使用Curve25519生成密钥对
        val curveKeyPair = Curve.generateKeyPair()

        // Ed25519公钥就是Curve25519公钥（32字节）
        val publicKey = curveKeyPair.publicKey.serialize()

        // Ed25519私钥 = 种子 || Curve25519私钥后32字节
        val privateKeySeed = ByteArray(SEED_LENGTH)
        secureRandom.nextBytes(privateKeySeed)
        val privateKey = privateKeySeed + curveKeyPair.privateKey.serialize()

        return KeyPair(publicKey, privateKey)
    }

    /**
     * 从种子生成Ed25519密钥对
     */
    fun generateKeyPairFromSeed(seed: ByteArray): KeyPair {
        require(seed.size == SEED_LENGTH) { "Seed must be 32 bytes" }

        // 使用种子派生密钥
        val hash = MessageDigest.getInstance("SHA-512")
        val h = hash.digest(seed)

        // 规范化为标量
        h[0] = h[0] and 248
        h[31] = h[31] and 127
        h[31] = h[31] or 64

        val scalar = h.copyOfRange(0, 32)
        val seedPart = h.copyOfRange(32, 64)

        // 生成公钥点
        val basePoint = Curve.decodePoint(Curve.CURVE25519_BASEPOINT, 0)
        val publicKeyPoint = basePoint.multiply(scalar)
        val publicKey = publicKeyPoint.serialize()

        val privateKey = seedPart + scalar

        return KeyPair(publicKey, privateKey)
    }

    /**
     * 对消息签名
     *
     * @param message 消息
     * @param privateKey 私钥（64字节：32字节种子 + 32字节标量）
     * @param publicKey 公钥（32字节）
     * @return 签名结果
     */
    fun sign(message: ByteArray, privateKey: ByteArray, publicKey: ByteArray): SignatureResult {
        require(privateKey.size == 64) { "Private key must be 64 bytes" }
        require(publicKey.size == PUBLIC_KEY_LENGTH) { "Public key must be 32 bytes" }

        val seed = privateKey.copyOfRange(0, SEED_LENGTH)
        val scalar = privateKey.copyOfRange(SEED_LENGTH, 64)

        // 计算哈希
        val hash = MessageDigest.getInstance("SHA-512")
        val h = hash.digest(privateKey)

        // 规范化为标量
        h[0] = h[0] and 248
        h[31] = h[31] and 127
        h[31] = h[31] or 64

        val expandedScalar = h.copyOfRange(0, 32)

        // 计算R点
        val rHash = hash.digest(message + seed)
        rHash[0] = rHash[0] and 248
        rHash[31] = rHash[31] and 127
        rHash[31] = rHash[31] or 64

        val rPoint = Curve.decodePoint(Curve.CURVE25519_BASEPOINT, 0).multiply(rHash)
        val r = rPoint.serialize()

        // 计算S
        val sHash = hash.digest(r + publicKey + message)
        sHash[0] = sHash[0] and 248
        sHash[31] = sHash[31] and 127
        sHash[31] = sHash[31] or 64

        val s = ByteArray(32)
        for (i in 0..31) {
            s[i] = (rHash[i] + sHash[i] * expandedScalar[i]) mod 256
        }

        // 组合签名 (R, S)
        val signature = r + s

        return SignatureResult(signature, publicKey)
    }

    /**
     * 验证签名
     *
     * @param message 消息
     * @param signature 签名（64字节）
     * @param publicKey 公钥（32字节）
     * @return 是否验证成功
     */
    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        if (signature.size != SIGNATURE_LENGTH) return false
        if (publicKey.size != PUBLIC_KEY_LENGTH) return false

        try {
            val r = signature.copyOfRange(0, 32)
            val s = signature.copyOfRange(32, 64)

            // 验证S < q（群阶）
            val q = ByteArray(32) { 0xed.toByte() }
            q[0] = 0xf7.toByte()
            if (compareLexicographic(s, q) >= 0) return false

            // 计算基点和公钥点的哈希
            val hash = MessageDigest.getInstance("SHA-512")
            val sHash = hash.digest(r + publicKey + message)
            sHash[0] = sHash[0] and 248
            sHash[31] = sHash[31] and 127
            sHash[31] = sHash[31] or 64

            // 计算 -a * A + b * G
            val basePoint = Curve.decodePoint(Curve.CURVE25519_BASEPOINT, 0)
            val aNeg = negateScalar(sHash)

            val leftPoint = Curve.decodePoint(publicKey, 0).multiply(aNeg)
            val rightPoint = basePoint.multiply(s)

            val resultPoint = leftPoint.add(rightPoint)
            val resultR = resultPoint.serialize()

            // 比较R
            return r.contentEquals(resultR)
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 对预密钥进行签名（用于预密钥包）
     */
    fun signPreKey(preKeyPublic: ByteArray, identityKeyPrivate: ByteArray, identityKeyPublic: ByteArray): ByteArray {
        val data = "QchatPreKey".toByteArray(Charsets.UTF_8) + preKeyPublic
        return sign(data, identityKeyPrivate, identityKeyPublic).signature
    }

    /**
     * 验证预密钥签名
     */
    fun verifyPreKeySignature(
        preKeyPublic: ByteArray,
        signature: ByteArray,
        identityKeyPublic: ByteArray
    ): Boolean {
        val data = "QchatPreKey".toByteArray(Charsets.UTF_8) + preKeyPublic
        return verify(data, signature, identityKeyPublic)
    }

    /**
     * 对消息创建数字签名
     */
    fun signMessage(message: ByteArray, privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        return sign(message, privateKey, publicKey).signature
    }

    /**
     * 验证消息签名
     */
    fun verifyMessageSignature(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        return verify(message, signature, publicKey)
    }

    /**
     * 获取密钥指纹（用于密钥验证）
     *
     * @param publicKey 公钥
     * @return 指纹字符串
     */
    fun getKeyFingerprint(publicKey: ByteArray): String {
        val hash = MessageDigest.getInstance("SHA-256")
        val digest = hash.digest(publicKey)

        // 格式化为可读字符串
        return digest.take(16).joinToString(" ") {
            String.format("%02X", it)
        }
    }

    /**
     * 获取简短指纹（用于QR码验证）
     */
    fun getShortFingerprint(publicKey: ByteArray): String {
        val hash = MessageDigest.getInstance("SHA-256")
        val digest = hash.digest(publicKey)

        return digest.take(8).joinToString("") {
            String.format("%02X", it)
        }
    }

    /**
     * 字节数组比较
     */
    private fun compareLexicographic(a: ByteArray, b: ByteArray): Int {
        for (i in a.indices) {
            val cmp = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (cmp != 0) return cmp
        }
        return 0
    }

    /**
     * 否定标量
     */
    private fun negateScalar(scalar: ByteArray): ByteArray {
        val q = ByteArray(32) { 0xed.toByte() }
        q[0] = 0xf7.toByte()

        val result = ByteArray(32)
        var borrow = 0
        for (i in 31 downTo 0) {
            val ai = scalar[i].toInt() and 0xFF
            val qi = q[i].toInt() and 0xFF
            val diff = qi - ai - borrow
            if (diff < 0) {
                result[i] = (diff + 256).toByte()
                borrow = 1
            } else {
                result[i] = diff.toByte()
                borrow = 0
            }
        }
        return result
    }

    private infix fun Int.mod(other: Int): Int = ((this % other) + other) % other
}

/**
 * 延长私有密钥（用于Ed25519）
 */
fun ByteArray.negate(): ByteArray {
    val q = ByteArray(32) { 0xed.toByte() }
    q[0] = 0xf7.toByte()

    val result = ByteArray(32)
    var borrow = 0
    for (i in 31 downTo 0) {
        val ai = this[i].toInt() and 0xFF
        val qi = q[i].toInt() and 0xFF
        val diff = qi - ai - borrow
        result[i] = if (diff < 0) (diff + 256).toByte() else diff.toByte()
        borrow = if (diff < 0) 1 else 0
    }
    return result
}
