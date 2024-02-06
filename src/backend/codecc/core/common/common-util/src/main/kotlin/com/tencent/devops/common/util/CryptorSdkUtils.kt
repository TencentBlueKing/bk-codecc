package com.tencent.devops.common.util

import com.tencent.bk.sdk.crypto.cryptor.ASymmetricCryptorFactory
import com.tencent.bk.sdk.crypto.cryptor.SymmetricCryptorFactory
import com.tencent.bk.sdk.crypto.cryptor.consts.CryptorNames
import com.tencent.bk.sdk.crypto.util.SM2Util
import com.tencent.kona.crypto.spec.SM2PrivateKeySpec
import com.tencent.kona.crypto.spec.SM2PublicKeySpec
import org.apache.commons.codec.binary.Base64
import java.security.KeyFactory

/**
 * 国密加密工具
 */
object CryptorSdkUtils {

    private var initFactory = false

    @Synchronized
    private fun initFactory() {
        if (initFactory) {
            return
        }
        SymmetricCryptorFactory.getCryptor(CryptorNames.SM4)
        ASymmetricCryptorFactory.getCryptor(CryptorNames.SM2)
        initFactory = true
    }

    fun sm4Encrypted(key: String, content: String): String {
        if (!initFactory) {
            initFactory()
        }
        val cryptor = SymmetricCryptorFactory.getCryptor(CryptorNames.SM4)
        return cryptor.encrypt(key, content)
    }

    fun sm4Decrypted(key: String, content: String): String {
        if (!initFactory) {
            initFactory()
        }
        val cryptor = SymmetricCryptorFactory.getCryptor(CryptorNames.SM4)
        return cryptor.decrypt(key, content)
    }

    fun sm2Encrypted(key: String, content: String): String {
        if (!initFactory) {
            initFactory()
        }
        val keyFactory = KeyFactory.getInstance(CryptorNames.SM2, SM2Util.PROVIDER_NAME_KONA_CRYPTO)
        val pubKeySpec = SM2PublicKeySpec(Base64.decodeBase64(key))
        val pubKey = keyFactory.generatePublic(pubKeySpec)
        val cryptor = ASymmetricCryptorFactory.getCryptor(CryptorNames.SM2)
        return cryptor.encrypt(pubKey, content)
    }

    fun sm2Decrypted(key: String, content: String): String {
        if (!initFactory) {
            initFactory()
        }
        val keyFactory = KeyFactory.getInstance(CryptorNames.SM2, SM2Util.PROVIDER_NAME_KONA_CRYPTO)
        val priKeySpec = SM2PrivateKeySpec(Base64.decodeBase64(key))
        val priKey = keyFactory.generatePrivate(priKeySpec)
        val cryptor = ASymmetricCryptorFactory.getCryptor(CryptorNames.SM2)
        return cryptor.decrypt(priKey, content)
    }
}
