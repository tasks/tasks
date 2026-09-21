package org.tasks.auth

import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString

object PKCE {
    fun generateVerifier(): String = secureRandomBytes(32).toByteString().base64Url().trimEnd('=')

    fun generateChallenge(verifier: String): String = verifier.encodeUtf8().sha256().base64Url().trimEnd('=')
}

internal expect fun secureRandomBytes(count: Int): ByteArray
