package org.tasks.security

interface Encryption {
    suspend fun encrypt(text: String): String?
    suspend fun decrypt(text: String?): String?
}
