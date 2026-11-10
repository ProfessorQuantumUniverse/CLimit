package io.github.professorquantumuniverse.climit.data

import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the claude.ai session cookie encrypted at rest.
 *
 * The AES-256-GCM key lives in the Android Keystore and cannot be exported; it is kept in
 * StrongBox where the device has one. Only IV + ciphertext are written to app-private storage.
 * The key is deliberately not bound to user authentication, because widgets refresh in the
 * background while the device may be locked.
 */
class SessionVault(context: Context) {

    private val appContext = context.applicationContext
    private val file = AtomicFile(File(appContext.noBackupFilesDir, FILE_NAME))

    /** Encrypts and stores [sessionKey], replacing any previous value. */
    @Synchronized
    fun save(sessionKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(sessionKey.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        check(iv.size == IV_BYTES) { "Unexpected IV size" }

        val stream = file.startWrite()
        try {
            stream.write(iv)
            stream.write(ciphertext)
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
    }

    /**
     * Returns the stored session key, or null if there is none. A value that can no longer be
     * decrypted (key lost, file damaged) is wiped and reported as absent, so the app falls back
     * to a fresh sign-in instead of failing forever.
     */
    @Synchronized
    fun load(): String? {
        val blob = try {
            file.readFully()
        } catch (_: FileNotFoundException) {
            return null
        }
        if (blob.size <= IV_BYTES) {
            clear()
            return null
        }
        return try {
            val key = existingKey() ?: throw GeneralSecurityException("Key is gone")
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES))
            String(cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES), Charsets.UTF_8)
        } catch (_: GeneralSecurityException) {
            clear()
            null
        }
    }

    fun hasSession(): Boolean = file.baseFile.exists()

    /** Deletes the ciphertext and the key. */
    @Synchronized
    fun clear() {
        file.delete()
        val keyStore = keyStore()
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun existingKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun getOrCreateKey(): SecretKey {
        existingKey()?.let { return it }
        val hasStrongBox =
            appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        if (hasStrongBox) {
            try {
                return generateKey(strongBox = true)
            } catch (_: StrongBoxUnavailableException) {
                // Advertised but not usable right now: fall back to the TEE-backed keystore.
            }
        }
        return generateKey(strongBox = false)
    }

    private fun generateKey(strongBox: Boolean): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setIsStrongBoxBacked(strongBox)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "climit.session.v1"
        const val FILE_NAME = "session.bin"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
