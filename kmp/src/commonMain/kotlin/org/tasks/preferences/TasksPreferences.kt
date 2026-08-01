package org.tasks.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class PreferencesSnapshot(private val preferences: Preferences) {
    fun <T> get(key: Preferences.Key<T>, defaultValue: T): T = preferences[key] ?: defaultValue
}

class TasksPreferences(private val dataStore: DataStore<Preferences>) {

    suspend fun <T> get(key: Preferences.Key<T>, defaultValue: T): T =
        dataStore.data.map { it[key] }.firstOrNull() ?: defaultValue

    suspend fun snapshot(): PreferencesSnapshot = PreferencesSnapshot(dataStore.data.first())

    fun <T> flow(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        dataStore.data.map { it[key] ?: defaultValue }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    suspend fun removeByPrefix(prefix: String) = removeByPrefixes(listOf(prefix))

    suspend fun removeByPrefixes(prefixes: Collection<String>) {
        if (prefixes.isEmpty()) return
        dataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { key -> prefixes.any { key.name.startsWith(it) } }
                .forEach { prefs.remove(it) }
        }
    }

    suspend fun <T> getAndSet(key: Preferences.Key<T>, value: T): T? {
        var previous: T? = null
        dataStore.edit {
            previous = it[key]
            it[key] = value
        }
        return previous
    }

    companion object {
        val collapseFilters = booleanPreferencesKey("drawer_collapse_filters")
        val collapseTags = booleanPreferencesKey("drawer_collapse_tags")
        val showDebugFilters = booleanPreferencesKey("show_debug_filters")
        val debugPro = booleanPreferencesKey("debug_pro")
        val collapseDebug = booleanPreferencesKey("drawer_collapse_debug")
        val collapsePlaces = booleanPreferencesKey("drawer_collapse_places")
        val acceptedTosVersion = intPreferencesKey("accepted_tos_version")
        val hasLoggedOnboardingComplete = booleanPreferencesKey("has_logged_onboarding_complete")
        val subscriptionDismissedAccounts = stringSetPreferencesKey("subscription_dismissed_accounts")
        val syncSource = stringPreferencesKey("sync_source")
        val cachedAccountData = stringPreferencesKey("cached_account_data")
        val serverEnvironment = stringPreferencesKey("server_environment")
        val syncOngoing = booleanPreferencesKey("sync_ongoing")
        val syncOngoingAndroid = booleanPreferencesKey("sync_ongoing_android")
        val windowWidth = intPreferencesKey("window_width")
        val windowHeight = intPreferencesKey("window_height")
        val windowX = intPreferencesKey("window_x")
        val windowY = intPreferencesKey("window_y")
        val installVersion = intPreferencesKey("install_version")
        val installDate = longPreferencesKey("install_date")
        val deviceInstallVersion = intPreferencesKey("device_install_version")
        val currentVersion = intPreferencesKey("current_version")
        val blogLastChecked = longPreferencesKey("blog_last_checked")
        val blogFeedMode = intPreferencesKey("blog_feed_mode")
        val blogPendingPost = stringPreferencesKey("blog_pending_post")
        val blogDismissedPostId = stringPreferencesKey("blog_dismissed_post_id")
        val collectStatistics = booleanPreferencesKey("collect_statistics")
        val sidebarExpanded = booleanPreferencesKey("sidebar_expanded")
        val sidebarWidth = intPreferencesKey("sidebar_width")
        val taskListPaneWidth = intPreferencesKey("task_list_pane_width")
        val localListBannerDismissed = booleanPreferencesKey("local_list_banner_dismissed")
        val needsCloudOnboarding = booleanPreferencesKey("needs_cloud_onboarding")
        val metadataPrimaryAccount = longPreferencesKey("metadata_primary_account")
        val metadataStoreAccount = longPreferencesKey("metadata_store_account")
        val metadataRev = stringPreferencesKey("metadata_rev")
        val metadataOrderDirty = booleanPreferencesKey("metadata_order_dirty")
        val datePickerInputMode = booleanPreferencesKey("date_picker_input_mode")
        val timePickerInputMode = booleanPreferencesKey("time_picker_input_mode")
        val persistentNotifications = booleanPreferencesKey("persistent_notifications")
        val wearableNotifications = booleanPreferencesKey("wearable_notifications")
        val bundleNotifications = booleanPreferencesKey("bundle_notifications")
        val voiceReminders = booleanPreferencesKey("voice_reminders")
        val swipeToSnoozeEnabled = booleanPreferencesKey("swipe_to_snooze_enabled")
        val swipeToSnoozeMinutes = intPreferencesKey("swipe_to_snooze_minutes")
        val defaultRemindersEnabled = booleanPreferencesKey("default_reminders_enabled")
        val defaultReminderTime = intPreferencesKey("default_reminder_time")
        val quietHoursEnabled = booleanPreferencesKey("quiet_hours_enabled")
        val quietHoursStart = intPreferencesKey("quiet_hours_start")
        val quietHoursEnd = intPreferencesKey("quiet_hours_end")
    }
}
