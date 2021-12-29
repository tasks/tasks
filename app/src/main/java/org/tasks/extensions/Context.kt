package org.tasks.extensions

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.widget.Toast
import androidx.annotation.AnyRes
import androidx.browser.customtabs.CustomTabsIntent
import org.tasks.R

object Context {
    private const val HTTP = "http"
    private const val HTTPS = "https"

    fun Context.safeStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.no_app_found)
        }
    }

    fun Context.openUri(resId: Int, vararg formatArgs: Any) = openUri(getString(resId, formatArgs))

    fun Context.openUri(url: String?) =
        url?.let { Uri.parse(it) }?.let {
            when {
                it.scheme.equals(HTTPS, true) || it.scheme.equals(HTTP, true) ->
                    try {
                        CustomTabsIntent.Builder()
                            .setUrlBarHidingEnabled(true)
                            .setShowTitle(true)
                            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                            .build()
                            .launchUrl(this, it)
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.no_app_found)
                    }
                else -> safeStartActivity(Intent(ACTION_VIEW, it))
            }
        }

    fun Context.toast(resId: Int, vararg formatArgs: Any, duration: Int = Toast.LENGTH_LONG) =
        toast(getString(resId, *formatArgs), duration)

    fun Context.toast(text: String?, duration: Int = Toast.LENGTH_LONG) =
        text?.let { Toast.makeText(this, it, duration).show() }

    fun Context.getResourceUri(@AnyRes res: Int) =
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(packageName)
            .path(res.toString())
            .build()
}
