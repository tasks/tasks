package org.tasks.auth

import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.io.IOException

object OAuthTokenRefresh {
    const val EXPIRY_MARGIN_MS = 60_000L

    fun isExpired(data: OAuthTokenData, refreshWhenExpiryUnknown: Boolean): Boolean =
        if (data.expiresAt <= 0) {
            refreshWhenExpiryUnknown
        } else {
            currentTimeMillis() > data.expiresAt - EXPIRY_MARGIN_MS
        }

    fun OAuthTokenData.withRefreshResult(result: TasksOAuthClient.RefreshResult): OAuthTokenData =
        copy(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken ?: refreshToken,
            expiresAt = result.expiresIn
                ?.let { currentTimeMillis() + it * 1000 }
                ?: 0,
        )

    inline fun refreshOrThrowIO(
        refresh: () -> TasksOAuthClient.RefreshResult,
    ): TasksOAuthClient.RefreshResult =
        try {
            refresh()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(e.message, e)
        }
}
