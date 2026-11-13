package com.filemate.app.apk

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
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

    /** Berkas kunci pilihan user. Kosong berarti kunci bawaan Filemate yang dipakai. */
    @Volatile
    var customPath: String = ""

    /** Kata sandi berkas kunci (hanya untuk berkas keystore seperti .p12). */
    @Volatile
    var customPassword: String = ""

    /** Akhiran berkas kunci yang bisa dipakai. */
    val KEY_EXTENSIONS = setOf("pk8", "p12", "pfx", "bks")

    /** Kunci yang dipakai untuk menandatangani: kunci pilihan user jika bisa dibaca, kalau tidak kunci bawaan. */
    fun load(): SigningKey {
        if (customPath.isNotEmpty()) {
            loadFile(File(customPath), customPassword)?.let { return it }
        }
        return builtIn()
    }

    /** Kunci bawaan Filemate, disimpan di Android Keystore. Dibuat saat pertama kali dipakai. */
    fun builtIn(): SigningKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(ALIAS)) {
            create()
        }
        val key = keyStore.getKey(ALIAS, null) as PrivateKey
        val cert = keyStore.getCertificate(ALIAS) as X509Certificate
        return SigningKey(key, cert)
    }

    /**
     * Membaca kunci dari berkas. Yang didukung: pasangan nama.pk8 dan nama.x509.pem (atau nama.pem),
     * serta keystore .p12, .pfx, dan .bks dengan kata sandi. Null jika tidak bisa dibaca.
     */
    fun loadFile(file: File, password: String): SigningKey? = try {
        when (file.extension.lowercase()) {
            "pk8" -> {
                val base = file.path.removeSuffix("." + file.extension)
                val certFile = listOf("$base.x509.pem", "$base.pem", "$base.crt", "$base.cer")
                    .map { File(it) }
                    .first { it.isFile }
                val spec = PKCS8EncodedKeySpec(file.readBytes())
                // Jenis kunci dicoba satu per satu, karena berkas pk8 tidak menyebut jenisnya di nama
                val key = listOf("RSA", "EC", "DSA").firstNotNullOf { type ->
                    try {
                        KeyFactory.getInstance(type).generatePrivate(spec)
                    } catch (e: Exception) {
                        null
                    }
                }
                val cert = certFile.inputStream().use {
                    CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
                }
                SigningKey(key, cert)
            }
            else -> {
                val type = if (file.extension.equals("bks", ignoreCase = true)) "BKS" else "PKCS12"
                val chars = password.toCharArray()
                val store = KeyStore.getInstance(type)
                file.inputStream().use { store.load(it, chars) }
                val alias = store.aliases().toList().first { store.isKeyEntry(it) }
                SigningKey(store.getKey(alias, chars) as PrivateKey, store.getCertificate(alias) as X509Certificate)
            }
        }
    } catch (e: Exception) {
        null
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
