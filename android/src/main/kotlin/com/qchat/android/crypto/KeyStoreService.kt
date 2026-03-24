package com.qchat.android.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.KeyPair
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper

/**
 * 密钥生成服务 - 使用Android KeyStore安全存储密钥
 */
class KeyStoreService {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val IDENTITY_KEY_ALIAS = "qchat_identity_key"
        private const val SIGNED_PRE_KEY_ALIAS = "qchat_signed_pre_key"
        private const val PRE_KEY_PREFIX = "qchat_pre_key_"
        private const val AES_GCM_KEY_SIZE = 256
        private const val AES_GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private val secureRandom = SecureRandom()

    /**
     * 生成Curve25519身份密钥对
     */
    fun generateIdentityKeyPair(): IdentityKeyPair {
        val keyPair = Curve.generateKeyPair()
        val publicKey = IdentityKey(keyPair.publicKey)
        val privateKey = keyPair.privateKey
        return IdentityKeyPair(publicKey, privateKey)
    }

    /**
     * 生成签名预密钥
     */
    fun generateSignedPreKey(identityKeyPair: IdentityKeyPair, signedPreKeyId: Int): SignedPreKeyRecord {
        val keyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(identityKeyPair.privateKey, keyPair.publicKey.serialize())
        return SignedPreKeyRecord(
            signedPreKeyId,
            System.currentTimeMillis(),
            keyPair,
            signature
        )
    }

    /**
     * 生成一次性预密钥
     */
    fun generatePreKeys(startId: Int, count: Int): List<PreKeyRecord> {
        return (startId until startId + count).map { id ->
            val keyPair = Curve.generateKeyPair()
            PreKeyRecord(id, keyPair)
        }
    }

    /**
     * 将密钥对存储到Android KeyStore
     */
    fun storeIdentityKey(identityKeyPair: IdentityKeyPair) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )

        val keyGenSpec = KeyGenParameterSpec.Builder(
            IDENTITY_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(256)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()

        keyGenerator.init(keyGenSpec)
    }

    /**
     * 生成AES-256对称密钥
     */
    fun generateAesKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenSpec = KeyGenParameterSpec.Builder(
            "qchat_aes_${System.currentTimeMillis()}",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_GCM_KEY_SIZE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * 从原始字节创建AES密钥
     */
    fun createAesKeyFromBytes(keyBytes: ByteArray): SecretKey {
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * 生成安全的随机字节
     */
    fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    /**
     * 生成会话ID
     */
    fun generateSessionId(): String {
        return Base64.encodeToString(
            generateRandomBytes(32),
            Base64.NO_WRAP
        )
    }

    /**
     * 生成消息密钥ID
     */
    fun generateMessageKeyId(): String {
        return Base64.encodeToString(
            generateRandomBytes(16),
            Base64.NO_WRAP
        )
    }

    /**
     * 存储预密钥到KeyStore
     */
    fun storePreKey(preKeyId: Int, preKey: PreKeyRecord) {
        keyStore.setEntry(
            "$PRE_KEY_PREFIX$preKeyId",
            KeyStore.SecretKeyEntry(
                SecretKeySpec(preKey.keyPair.privateKey.serialize(), "AES")
            ),
            null
        )
    }

    /**
     * 从KeyStore获取预密钥
     */
    fun getPreKey(preKeyId: Int): PreKeyRecord? {
        return try {
            val entry = keyStore.getEntry("$PRE_KEY_PREFIX$preKeyId", null) as? KeyStore.SecretKeyEntry
            entry?.let {
                val privateKeyBytes = it.secretKey.encoded
                // 重建PreKeyRecord
                PreKeyRecord(preKeyId, Curve.decodePrivateKey(privateKeyBytes))
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 删除预密钥
     */
    fun removePreKey(preKeyId: Int) {
        if (keyStore.containsAlias("$PRE_KEY_PREFIX$preKeyId")) {
            keyStore.deleteEntry("$PRE_KEY_PREFIX$preKeyId")
        }
    }

    /**
     * 获取当前用户的注册ID
     */
    fun generateRegistrationId(): Int {
        return KeyHelper.generateRegistrationId(false)
    }
}

/**
 * PreKey记录结构
 */
data class PreKeyRecord(
    val id: Int,
    val keyPair: ECKeyPair
)

/**
 * 预密钥包结构
 */
data class PreKeyPackage(
    val registrationId: Int,
    val preKeyId: Int,
    val preKeyPublic: ByteArray,
    val signedPreKeyId: Int,
    val signedPreKeyPublic: ByteArray,
    val signedPreKeySignature: ByteArray,
    val identityKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreKeyPackage

        if (registrationId != other.registrationId) return false
        if (preKeyId != other.preKeyId) return false
        if (!preKeyPublic.contentEquals(other.preKeyPublic)) return false
        if (signedPreKeyId != other.signedPreKeyId) return false
        if (!signedPreKeyPublic.contentEquals(other.signedPreKeyPublic)) return false
        if (!signedPreKeySignature.contentEquals(other.signedPreKeySignature)) return false
        if (!identityKey.contentEquals(other.identityKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = registrationId
        result = 31 * result + preKeyId
        result = 31 * result + preKeyPublic.contentHashCode()
        result = 31 * result + signedPreKeyId
        result = 31 * result + signedPreKeyPublic.contentHashCode()
        result = 31 * result + signedPreKeySignature.contentHashCode()
        result = 31 * result + identityKey.contentHashCode()
        return result
    }
}
