package org.tasks.analytics

import android.content.Context
import androidx.annotation.StringRes
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Firebase @Inject constructor(
        @ApplicationContext private val context: Context,
        private val preferences: Preferences
) {
    private val crashlytics by lazy {
        if (preferences.isTrackingEnabled) {
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(true)
            }
        } else {
            null
        }
    }

    private val posthogEnabled: Boolean by lazy {
        val apiKey = context.getString(R.string.posthog_key)
        if (preferences.isTrackingEnabled && apiKey.isNotBlank()) {
            PostHogAndroid.setup(
                context,
                PostHogAndroidConfig(
                    apiKey = apiKey,
                    host = POSTHOG_HOST
                ).apply {
                    sessionReplay = BuildConfig.DEBUG
                    sessionReplayConfig.maskAllTextInputs = true
                    sessionReplayConfig.maskAllImages = false
                    sessionReplayConfig.screenshot = true
                }
            )
            true
        } else {
            false
        }
    }
    
    private val remoteConfig by lazy {
        if (preferences.isTrackingEnabled) {
            FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(remoteConfigSettings {
                    minimumFetchIntervalInSeconds =
                            TimeUnit.HOURS.toSeconds(WorkManager.REMOTE_CONFIG_INTERVAL_HOURS)
                })
                setDefaultsAsync(R.xml.remote_config_defaults)
            }
        } else {
            null
        }
    }

    fun reportException(t: Throwable) {
        Timber.e(t)
        crashlytics?.recordException(t)
    }

    fun reportIabResult(result: String, sku: String, state: String) {
        logEvent(
            R.string.event_purchase_result,
            R.string.param_sku to sku,
            R.string.param_result to result,
            R.string.param_state to state,
        )
    }

    fun updateRemoteConfig() {
        remoteConfig?.fetchAndActivate()?.addOnSuccessListener {
            Timber.d(it.toString())
        }
    }

    fun addTask(source: String) =
        logEventForNewUsers(R.string.event_add_task, R.string.param_type to source)

    fun completeTask(source: String) =
        logEventForNewUsers(R.string.event_complete_task, R.string.param_type to source)

    private val loggedOnceEvents = mutableSetOf<Int>()

    fun logEvent(@StringRes event: Int, vararg p: Pair<Int, Any>) {
        val eventName = context.getString(event)
        val properties = p.associate { context.getString(it.first) to it.second }
        Timber.d("$eventName -> $properties")
        if (posthogEnabled) {
            PostHog.capture(
                event = eventName,
                properties = properties
            )
        }
    }

    fun logEventOnce(@StringRes event: Int, vararg p: Pair<Int, Any>) {
        if (loggedOnceEvents.add(event)) {
            logEvent(event, *p)
        }
    }

    fun logEventOncePerDay(@StringRes event: Int, vararg p: Pair<Int, Any>) {
        val eventName = context.getString(event)
        val prefKey = "last_logged_$eventName"
        val today = currentTimeMillis().startOfDay()
        val lastLogged = preferences.getLong(prefKey, 0L)
        if (lastLogged < today) {
            preferences.setLong(prefKey, today)
            logEvent(event, *p)
        }
    }

    fun logEventForNewUsers(@StringRes event: Int, vararg p: Pair<Int, Any>) {
        val installDate = preferences.installDate
        // Only track for users installed within last 30 days
        // installDate of 0 means very old user (pre-tracking) - skip them
        if (installDate > 0 && currentTimeMillis() - installDate < TimeUnit.DAYS.toMillis(30)) {
            logEvent(event, *p)
        }
    }

    private val installCooldown: Boolean
        get() = preferences.installDate + days("install_cooldown", 14L) > currentTimeMillis()

    val reviewCooldown: Boolean
        get() = installCooldown || preferences.lastReviewRequest + days("review_cooldown", 30L) > currentTimeMillis()

    val subscribeCooldown: Boolean
        get() = installCooldown
                || preferences.lastSubscribeRequest + days("subscribe_cooldown", 30L) > currentTimeMillis()

    private fun days(key: String, default: Long): Long =
            TimeUnit.DAYS.toMillis(remoteConfig?.getLong(key) ?: default)

    fun getTosVersion(): Int {
        val default = context.resources.getInteger(R.integer.default_tos_version)
        return remoteConfig
            ?.getLong(context.getString(R.string.remote_config_tos_version))
            ?.toInt()
            ?.takeIf { it >= default }
            ?: default
    }

    companion object {
        private const val POSTHOG_HOST = "https://us.i.posthog.com"
    }
}
