package org.tasks.auth

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import co.touchlab.kermit.Logger

class OAuthRedirectActivity : Activity() {
    private val logger = Logger.withTag(TAG)
    private var launchedAuthUrl: String? = null
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchedAuthUrl = savedInstanceState?.getString(KEY_LAUNCHED_AUTH_URL)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val redirect = intent?.data?.toString()
        if (redirect == null) {
            val authUrl = PendingRedirect.pendingAuthUrl()
            if (authUrl != null && authUrl != launchedAuthUrl) {
                launchedAuthUrl = authUrl
                handled = false
                launchBrowser(authUrl)
                return
            }
        }
        if (handled) {
            return
        }
        handled = true
        val delivered = if (redirect == null) {
            logger.d { "Browser dismissed without a redirect" }
            PendingRedirect.cancel()
        } else {
            when (PendingRedirect.deliver(redirect)) {
                Delivery.DELIVERED -> true
                Delivery.NOTHING_WAITING -> false
                Delivery.IGNORED -> {
                    logger.w { "Ignoring a redirect that is not for the pending sign-in" }
                    PendingRedirect.pendingAuthUrl()?.let { authUrl ->
                        launchedAuthUrl = authUrl
                        handled = false
                        launchBrowser(authUrl)
                        return
                    }
                    false
                }
            }
        }
        if (!delivered) {
            logger.w { "Nothing was waiting for the redirect" }
            packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
        }
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_LAUNCHED_AUTH_URL, launchedAuthUrl)
    }

    private fun launchBrowser(authUrl: String) {
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(this, authUrl.toUri())
        } catch (e: ActivityNotFoundException) {
            PendingRedirect.fail(e)
            finish()
        }
    }

    companion object {
        private const val TAG = "OAuthRedirectActivity"
        private const val KEY_LAUNCHED_AUTH_URL = "launched_auth_url"

        fun start(context: Context) {
            context.startActivity(
                Intent(context, OAuthRedirectActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
