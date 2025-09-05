package org.tasks.security

import java.util.Base64

class AndroidKeyStoreEncryption() : KeyStoreEncryption(AndroidKeyProvider()) {
    override fun decodeBase64(text: String): ByteArray {
        return try {
            Base64.getDecoder().decode(text)
        } catch (_: IllegalArgumentException) {
            try {
                android.util.Base64.decode(text, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                logger.e(e) { "Failed to decode Base64 data with both decoders" }
                ByteArray(0)
            }
        }
    }
}