package org.tasks.security

class PlainTextEncryption : Encryption {
    override suspend fun encrypt(text: String): String = text

    override suspend fun decrypt(text: String?): String? = text
}
