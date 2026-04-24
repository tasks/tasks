package org.tasks.billing

interface DesktopLinkClient {
    suspend fun createLink(): LinkResult?
    suspend fun pollStatus(code: String): StatusResult?
    suspend fun onLinkSuccess(jwt: String, refreshToken: String, sku: String?, formattedPrice: String?)
}
