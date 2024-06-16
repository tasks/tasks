package org.tasks.extensions

import android.app.Activity
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.AnyRes
import androidx.browser.customtabs.CustomTabsIntent
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.todoroo.andlib.utility.AndroidUtilities.atLeastS
import org.tasks.R
import org.tasks.notifications.NotificationManager.Companion.NOTIFICATION_CHANNEL_DEFAULT

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

    fun Context.hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    val Context.nightMode: Int
        get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    val Context.isNightMode: Boolean
        get() = nightMode == Configuration.UI_MODE_NIGHT_YES

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

    fun Context.getResourceUri(@AnyRes res: Int): Uri =
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(packageName)
            .path(res.toString())
            .build()

    fun Context.cookiePersistor(key: String? = null) =
        SharedPrefsCookiePersistor(
            getSharedPreferences(
                "CookiePersistence${key?.let { "_$it" } ?: ""}",
                MODE_PRIVATE
            )
        )

    fun Context.hasNetworkConnectivity(): Boolean {
        return try {
            with(getSystemService(ConnectivityManager::class.java)) {
                getNetworkCapabilities(activeNetwork)?.hasCapability(NET_CAPABILITY_INTERNET) == true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

    fun Context.canScheduleExactAlarms(): Boolean =
        !atLeastS() || (getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

    fun Context.openReminderSettings() {
        if (atLeastS()) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .apply { data = Uri.parse("package:$packageName") }
            )
        }
    }

    fun Context.openAppNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }
    }

    fun Context.openChannelNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, NOTIFICATION_CHANNEL_DEFAULT)
            )
        }
    }
}
