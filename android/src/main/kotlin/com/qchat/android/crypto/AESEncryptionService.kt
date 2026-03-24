package com.qchat.android.crypto

import android.util.Base64
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM加密服务
 *
 * 提供消息内容的端到端加密，使用AES-256-GCM模式
 * - AES-256: 256位高级加密标准
 * - GCM: Galois/Counter Mode，提供认证加密
 */
class AESEncryptionService {

    companion object {
        private const val AES_KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val HMAC_KEY_SIZE = 32
    }

    private val secureRandom = SecureRandom()

    /**
     * 使用AES-256-GCM加密数据
     *
     * @param plaintext 明文数据
     * @param key 256位密钥
     * @return 加密数据（IV + 密文 + 认证标签）
     */
    fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
        require(key.size == 32) { "Key must be 256 bits (32 bytes)" }

        // 生成随机IV
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        // 创建加密 cipher
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        // 加密
        val ciphertext = cipher.doFinal(plaintext)

        // 组合 IV + 密文
        val result = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(ciphertext, 0, result, iv.size, ciphertext.size)

        return result
    }

    /**
     * 使用AES-256-GCM解密数据
     *
     * @param ciphertext 加密数据（IV + 密文 + 认证标签）
     * @param key 256位密钥
     * @return 解密后的明文
     */
    fun decrypt(ciphertext: ByteArray, key: ByteArray): ByteArray {
        require(key.size == 32) { "Key must be 256 bits (32 bytes)" }
        require(ciphertext.size > GCM_IV_LENGTH) { "Ciphertext too short" }

        // 提取IV
        val iv = ciphertext.copyOfRange(0, GCM_IV_LENGTH)
        val actualCiphertext = ciphertext.copyOfRange(GCM_IV_LENGTH, ciphertext.size)

        // 创建解密 cipher
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        // 解密
        return cipher.doFinal(actualCiphertext)
    }

    /**
     * 使用AES-256-GCM加密字符串
     *
     * @param plaintext 明文字符串
     * @param key 256位密钥
     * @return Base64编码的加密数据
     */
    fun encryptString(plaintext: String, key: ByteArray): String {
        val encrypted = encrypt(plaintext.toByteArray(Charsets.UTF_8), key)
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /**
     * 使用AES-256-GCM解密字符串
     *
     * @param encryptedBase64 Base64编码的加密数据
     * @param key 256位密钥
     * @return 解密后的字符串
     */
    fun decryptString(encryptedBase64: String, key: ByteArray): String {
        val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val decrypted = decrypt(encrypted, key)
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * 生成随机AES密钥
     *
     * @return 256位随机密钥
     */
    fun generateKey(): ByteArray {
        val key = ByteArray(32)
        secureRandom.nextBytes(key)
        return key
    }

    /**
     * 从密码派生AES密钥
     *
     * @param password 密码
     * @param salt 盐值
     * @return 派生的256位密钥
     */
    fun deriveKeyFromPassword(password: String, salt: ByteArray): ByteArray {
        // 使用PBKDF2派生密钥
        val spec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt,
            100000, // 迭代次数
            256 // 密钥长度
        )
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    /**
     * 生成消息认证码（HMAC）
     *
     * @param data 数据
     * @param key 密钥
     * @return HMAC
     */
    fun generateHMAC(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data)
    }

    /**
     * 验证消息认证码
     *
     * @param data 数据
     * @param key 密钥
     * @param expectedHmac 期望的HMAC
     * @return 是否匹配
     */
    fun verifyHMAC(data: ByteArray, key: ByteArray, expectedHmac: ByteArray): Boolean {
        val actualHmac = generateHMAC(data, key)
        return MessageDigest.isEqual(actualHmac, expectedHmac)
    }

    /**
     * 生成带HMAC的加密消息
     *
     * @param plaintext 明文
     * @param encryptionKey 加密密钥
     * @param hmacKey HMAC密钥
     * @return 加密数据 + HMAC
     */
    fun encryptWithHMAC(plaintext: ByteArray, encryptionKey: ByteArray, hmacKey: ByteArray): ByteArray {
        // 加密
        val encrypted = encrypt(plaintext, encryptionKey)

        // 生成HMAC
        val hmac = generateHMAC(encrypted, hmacKey)

        // 组合
        val result = ByteArray(encrypted.size + hmac.size)
        System.arraycopy(encrypted, 0, result, 0, encrypted.size)
        System.arraycopy(hmac, 0, result, encrypted.size, hmac.size)

        return result
    }

    /**
     * 验证并解密带HMAC的消息
     *
     * @param data 加密数据 + HMAC
     * @param encryptionKey 加密密钥
     * @param hmacKey HMAC密钥
     * @return 解密后的明文，验证失败返回null
     */
    fun decryptWithHMAC(data: ByteArray, encryptionKey: ByteArray, hmacKey: ByteArray): ByteArray? {
        require(data.size > 32) { "Data too short" }

        // 分离加密数据和HMAC
        val encrypted = data.copyOfRange(0, data.size - 32)
        val hmac = data.copyOfRange(data.size - 32, data.size)

        // 验证HMAC
        if (!verifyHMAC(encrypted, hmacKey, hmac)) {
            return null
        }

        // 解密
        return decrypt(encrypted, encryptionKey)
    }

    /**
     * 创建消息认证数据
     *
     * @param messageId 消息ID
     * @param senderId 发送者ID
     * @param recipientId 接收者ID
     * @param timestamp 时间戳
     * @param ciphertext 加密内容
     * @param hmacKey HMAC密钥
     * @return 完整的认证数据
     */
    fun createAuthData(
        messageId: String,
        senderId: String,
        recipientId: String,
        timestamp: Long,
        ciphertext: ByteArray,
        hmacKey: ByteArray
    ): ByteArray {
        val authString = "$messageId|$senderId|$recipientId|$timestamp"
        return generateHMAC(authString.toByteArray(Charsets.UTF_8), hmacKey)
    }

    /**
     * 验证消息认证数据
     */
    fun verifyAuthData(
        messageId: String,
        senderId: String,
        recipientId: String,
        timestamp: Long,
        ciphertext: ByteArray,
        expectedAuth: ByteArray,
        hmacKey: ByteArray
    ): Boolean {
        val authData = createAuthData(messageId, senderId, recipientId, timestamp, ciphertext, hmacKey)
        return MessageDigest.isEqual(authData, expectedAuth)
    }
}
