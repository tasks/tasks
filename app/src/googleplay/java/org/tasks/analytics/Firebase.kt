package org.tasks.analytics

import android.content.Context
import android.content.SharedPreferences
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

    private val posthogEnabled: Boolean = run {
        val apiKey = context.getString(R.string.posthog_key)
        if (preferences.isTrackingEnabled && apiKey.isNotBlank()) {
            PostHogAndroid.setup(
                context,
                PostHogAndroidConfig(
                    apiKey = apiKey,
                    host = POSTHOG_HOST
                ).apply {
                    preloadFeatureFlags = false
                    sendFeatureFlagEvent = false
                    remoteConfig = false

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

    private val trackedPrefs by lazy { intArrayOf(
        // Look and feel
        R.string.p_theme,
        R.string.p_dynamic_color,
        R.string.p_theme_color,
        R.string.p_theme_launcher,
        R.string.p_markdown,
        R.string.p_open_last_viewed_list,
        R.string.p_default_open_filter,
        R.string.p_language,
        // Notifications
        R.string.p_rmd_persistent,
        R.string.p_wearable_notifications,
        R.string.p_bundle_notifications,
        R.string.p_voiceRemindersEnabled,
        R.string.p_rmd_swipe_to_snooze_enabled,
        R.string.p_rmd_swipe_to_snooze_time_minutes,
        R.string.p_rmd_time_enabled,
        R.string.p_rmd_time,
        R.string.p_badges_enabled,
        R.string.p_badge_list,
        R.string.p_rmd_enable_quiet,
        R.string.p_rmd_quietStart,
        R.string.p_rmd_quietEnd,
        // Task defaults
        R.string.p_add_to_top,
        R.string.p_default_list,
        R.string.p_default_tags,
        R.string.p_default_importance_key,
        R.string.p_default_hideUntil_key,
        R.string.p_default_urgency_key,
        R.string.p_default_recurrence,
        R.string.p_default_recurrence_from,
        R.string.p_default_reminders_key,
        R.string.p_rmd_default_random_hours,
        R.string.p_default_reminders_mode_key,
        R.string.p_default_location,
        R.string.p_default_location_reminder_key,
        // Task list display
        R.string.p_fontSize,
        R.string.p_rowPadding,
        R.string.p_fullTaskTitle,
        R.string.p_show_description,
        R.string.p_show_full_description,
        R.string.p_linkify_task_list,
        R.string.p_chip_appearance,
        R.string.p_subtask_chips,
        R.string.p_start_date_chip,
        R.string.p_place_chips,
        R.string.p_list_chips,
        R.string.p_tag_chips,
        // Task edit
        R.string.p_beast_mode_order,
        R.string.p_linkify_task_edit,
        R.string.p_back_button_saves_task,
        R.string.p_multiline_title,
        R.string.p_show_task_edit_comments,
        R.string.p_show_edit_screen_without_unlock,
        // Date and time
        R.string.p_always_display_full_date,
        R.string.p_date_shortcut_morning,
        R.string.p_date_shortcut_afternoon,
        R.string.p_date_shortcut_evening,
        R.string.p_date_shortcut_night,
        R.string.p_auto_dismiss_datetime_edit_screen,
        R.string.p_auto_dismiss_datetime_list_screen,
        R.string.p_auto_dismiss_datetime_widget,
        // Navigation drawer
        R.string.p_last_viewed_list,
        R.string.p_filters_enabled,
        R.string.p_show_today_filter,
        R.string.p_show_recently_modified_filter,
        R.string.p_tags_enabled,
        R.string.p_tags_hide_unused,
        R.string.p_places_enabled,
        R.string.p_places_hide_unused,
        // Backups
        R.string.p_backups_enabled,
        R.string.p_backups_android_backup_enabled,
        R.string.p_backups_ignore_warnings,
        R.string.p_google_drive_backup,
        // Picker mode
        R.string.p_picker_mode_date,
        R.string.p_picker_mode_time,
        // Sorting
        R.string.p_sort_mode,
        R.string.p_group_mode,
        R.string.p_completed_mode,
        R.string.p_subtask_mode,
        R.string.p_sort_ascending,
        R.string.p_group_ascending,
        R.string.p_completed_ascending,
        R.string.p_subtask_ascending,
        R.string.p_manual_sort,
        R.string.p_astrid_sort,
        R.string.p_show_hidden_tasks,
        R.string.p_show_completed_tasks,
        R.string.p_completed_tasks_at_bottom,
        // Analytics opt-in
        R.string.p_collect_statistics,
        // Advanced
        R.string.p_end_at_deadline,
        R.string.p_astrid_sort_enabled,
    ).mapTo(HashSet()) { context.getString(it) } }

    private val obfuscatedPrefs by lazy { intArrayOf(
        R.string.p_backup_dir,
        R.string.p_attachment_dir,
        R.string.gcal_p_default,
        R.string.p_completion_ringtone,
    ).mapTo(HashSet()) { context.getString(it) } }

    private val prefChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == null || !posthogEnabled) return@OnSharedPreferenceChangeListener
            val properties = when {
                key in trackedPrefs -> {
                    val value = sharedPrefs.all[key]
                    mapOf("key" to key, "value" to (value ?: "null"))
                }
                key in obfuscatedPrefs -> {
                    val value = sharedPrefs.getString(key, null)
                    val state = when {
                        value == null -> "null"
                        value.isEmpty() -> "empty"
                        else -> "non-empty"
                    }
                    mapOf("key" to key, "value" to state)
                }
                key.startsWith("widget-") -> {
                    // strip widget ID suffix: "widget-theme-v2-123" -> "widget-theme-v2"
                    val widgetId = key.substringAfterLast("-").toIntOrNull()
                    val normalized = if (widgetId != null)
                        key.substringBeforeLast("-")
                    else
                        key
                    val value = sharedPrefs.all[key]
                    buildMap<String, Any> {
                        put("key", normalized)
                        put("value", value ?: "null")
                        if (widgetId != null) put("widget_id", widgetId)
                    }
                }
                else -> null
            }
            if (properties != null) {
                PostHog.capture(event = "preference_changed", properties = properties)
            }
        }

    init {
        if (posthogEnabled) {
            preferences.registerOnSharedPreferenceChangeListener(prefChangeListener)
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

    fun reportIabResult(result: String, sku: String, state: String, orderId: String) {
        logEvent(
            R.string.event_purchase_result,
            R.string.param_sku to sku,
            R.string.param_result to result,
            R.string.param_state to state,
            R.string.param_order_id to orderId,
        )
    }

    fun updateRemoteConfig() {
        remoteConfig?.fetchAndActivate()?.addOnSuccessListener {
            Timber.d(it.toString())
        }
    }

    fun addTask(source: String) =
        logEvent(R.string.event_add_task, R.string.param_type to source)

    fun completeTask(source: String) =
        logEvent(R.string.event_complete_task, R.string.param_type to source)

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
