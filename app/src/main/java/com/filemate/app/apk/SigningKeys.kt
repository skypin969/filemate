package com.filemate.app.apk

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Calendar
import java.util.Date
import javax.security.auth.x500.X500Principal

/** Pasangan kunci dan sertifikat untuk menandatangani APK. */
class SigningKey(val privateKey: PrivateKey, val certificate: X509Certificate)

/**
 * Kunci tanda tangan disimpan di Android Keystore milik HP.
 * Kunci ini tidak bisa diekspor. Jika aplikasi dihapus, kunci ikut hilang.
 */
object SigningKeys {

    private const val ALIAS = "filemate_signing_key"

    fun load(): SigningKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(ALIAS)) {
            create()
        }
        val key = keyStore.getKey(ALIAS, null) as PrivateKey
        val cert = keyStore.getCertificate(ALIAS) as X509Certificate
        return SigningKey(key, cert)
    }

    private fun create() {
        val notBefore = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        val notAfter: Date = Calendar.getInstance().apply { set(2060, Calendar.JANUARY, 1) }.time

        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setCertificateSubject(X500Principal("CN=Filemate"))
            .setCertificateSerialNumber(BigInteger.ONE)
            .setCertificateNotBefore(notBefore)
            .setCertificateNotAfter(notAfter)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").apply {
            initialize(spec)
            generateKeyPair()
        }
    }
}
