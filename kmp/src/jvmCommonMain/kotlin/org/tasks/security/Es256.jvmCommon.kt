package org.tasks.security

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

internal actual fun verifyEs256(
    publicKey: ByteArray,
    signed: ByteArray,
    derSignature: ByteArray,
): Boolean = try {
    val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicKey))
    Signature.getInstance("SHA256withECDSA").run {
        initVerify(key)
        update(signed)
        verify(derSignature)
    }
} catch (_: Exception) {
    false
}
