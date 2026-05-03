package org.tasks.billing

interface DesktopLinkService {
    suspend fun confirmLink(code: String): Boolean
}
