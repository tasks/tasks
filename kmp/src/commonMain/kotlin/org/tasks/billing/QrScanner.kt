package org.tasks.billing

interface QrScanner {
    suspend fun scan(): String?
}
