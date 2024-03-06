package com.tencent.devops.common.util

import org.apache.commons.codec.binary.Base64
import org.slf4j.LoggerFactory
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

object RSAUtil {

    private const val ALGORITHM: String = "RSA"

    private val logger = LoggerFactory.getLogger(RSAUtil::class.java)

    fun encryptByPublicKey(inputBytes: ByteArray, key: String): ByteArray {
        try {
            val decoded = Base64.decodeBase64(key)
            val pubKey = KeyFactory.getInstance(ALGORITHM).generatePublic(X509EncodedKeySpec(decoded))
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, pubKey)
            return cipher.doFinal(inputBytes)
        } catch (e: Exception) {
            logger.error("RSAUtil encrypt fail!", e)
        }
        return ByteArray(0)
    }

    fun decryptByPrivateKey(inputBytes: ByteArray, key: String): ByteArray {
        try {
            val decoded = Base64.decodeBase64(key)
            val priKey = KeyFactory.getInstance(ALGORITHM).generatePrivate(PKCS8EncodedKeySpec(decoded))
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, priKey)
            return cipher.doFinal(inputBytes)
        } catch (e: Exception) {
            logger.error("RSAUtil decrypt fail!", e)
        }
        return ByteArray(0)
    }
}
