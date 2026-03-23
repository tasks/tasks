package org.tasks.security

import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class DesktopKeyProvider(
    private val keyFile: File,
) : KeyProvider {
    override fun getKey(): SecretKey {
        if (!keyFile.exists()) {
            val key = ByteArray(32)
            SecureRandom().nextBytes(key)
            keyFile.parentFile?.mkdirs()
            keyFile.writeText(Base64.getEncoder().encodeToString(key))
        }
        val encoded = Base64.getDecoder().decode(keyFile.readText().trim())
        return SecretKeySpec(encoded, "AES")
    }
}
