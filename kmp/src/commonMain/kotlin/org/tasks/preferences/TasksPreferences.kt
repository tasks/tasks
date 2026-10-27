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

    suspend fun setIfGreater(key: Preferences.Key<Long>, value: Long, default: Long = 0L): Boolean {
        var updated = false
        dataStore.edit {
            if ((it[key] ?: default) < value) {
                it[key] = value
                updated = true
            }
        }
        return updated
    }

    /**
     * Atomically writes [value] for [key] only if no value is stored yet, so a
     * caller can seed a platform-specific default exactly once without a
     * read-then-write race and without clobbering a value the user (or a
     * later seed) already set. Returns true if it seeded.
     */
    suspend fun <T> ifUnset(key: Preferences.Key<T>, value: T): Boolean {
        var seeded = false
        dataStore.edit {
            if (it[key] == null) {
                it[key] = value
                seeded = true
            }
        }
        return seeded
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
        val mcpServerEnabled = booleanPreferencesKey("mcp_server_enabled")
        val mcpServerPort = intPreferencesKey("mcp_server_port")
        val mcpServerReadWrite = booleanPreferencesKey("mcp_server_read_write")
        val mcpServerAccessMode = stringPreferencesKey("mcp_server_access_mode")
        val mcpServerEnabledTools = stringSetPreferencesKey("mcp_server_enabled_tools")
        val mcpServerToken = stringPreferencesKey("mcp_server_token")
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

        // Edit screen options pane (desktop settings; Android uses its own SharedPreferences).
        // Every read path keeps the native default (false); the desktop seeds true on first
        // launch so its historical Escape-to-save back behaviour remains the default.
        val taskEditBackButtonSaves = booleanPreferencesKey("task_edit_back_button_saves")

        // Customize edit screen: JSON array of {"id","enabled"} entries in the user's
        // display order (hidden rows stay in the list so re-enabling restores their spot).
        // A null/absent value means the canonical order with every section enabled.
        val editScreenSections = stringPreferencesKey("edit_screen_sections")

        val sidebarWidth = intPreferencesKey("sidebar_width")
        val taskListPaneWidth = intPreferencesKey("task_list_pane_width")
        val localListBannerDismissed = booleanPreferencesKey("local_list_banner_dismissed")
        val needsCloudOnboarding = booleanPreferencesKey("needs_cloud_onboarding")
        val apnsToken = stringPreferencesKey("apns_token")
        val pushFallbackToken = stringPreferencesKey("push_fallback_token")
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
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val screenClearedAtShutdown = booleanPreferencesKey("screen_cleared_at_shutdown")
        val notificationSession = stringPreferencesKey("notification_session")
        val defaultRemindersEnabled = booleanPreferencesKey("default_reminders_enabled")
        val defaultReminderTime = intPreferencesKey("default_reminder_time")
        val quietHoursEnabled = booleanPreferencesKey("quiet_hours_enabled")
        val quietHoursStart = intPreferencesKey("quiet_hours_start")
        val quietHoursEnd = intPreferencesKey("quiet_hours_end")
        val addTasksToTop = booleanPreferencesKey("add_tasks_to_top")
        val defaultList = stringPreferencesKey("default_list")
        val defaultTags = stringPreferencesKey("default_tags")
        val defaultPriority = intPreferencesKey("default_priority")
        val defaultHideUntil = intPreferencesKey("default_hide_until")
        val defaultDueDate = intPreferencesKey("default_due_date")
        val defaultCalendar = stringPreferencesKey("default_calendar")
        val defaultRecurrence = stringPreferencesKey("default_recurrence")
        val defaultRecurrenceFrom = intPreferencesKey("default_recurrence_from")
        val defaultAlarms = stringSetPreferencesKey("default_alarms")
        val defaultRingMode = intPreferencesKey("default_ring_mode")
        val defaultLocation = stringPreferencesKey("default_location")
        val defaultLocationReminder = intPreferencesKey("default_location_reminder")
        val locationUpdateInterval = intPreferencesKey("location_update_interval")
        val filtersEnabled = booleanPreferencesKey("drawer_filters_enabled")
        val showTodayFilter = booleanPreferencesKey("p_show_today_f")
        val showRecentlyModifiedFilter = booleanPreferencesKey("p_show_recently_modified_f")
        val tagsEnabled = booleanPreferencesKey("drawer_tags_enabled")
        val tagsHideUnused = booleanPreferencesKey("drawer_tags_hide_unused")
        val placesEnabled = booleanPreferencesKey("drawer_places_enabled")
        val placesHideUnused = booleanPreferencesKey("drawer_places_hide_unused")
        val theme = intPreferencesKey("theme")
        val themeColor = intPreferencesKey("theme_color")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val markdown = booleanPreferencesKey("markdown")
        val openLastViewedList = booleanPreferencesKey("open_last_viewed_list")
        val defaultOpenFilter = stringPreferencesKey("default_open_filter")
        val lastViewedList = stringPreferencesKey("last_viewed_list")
        val languageTag = stringPreferencesKey("language_tag")
    }
}
