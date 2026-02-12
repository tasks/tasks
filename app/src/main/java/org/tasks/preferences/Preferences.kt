package org.tasks.preferences

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.activity.BeastModePreferences
import com.todoroo.astrid.core.SortHelper
import kotlinx.serialization.json.Json
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.billing.Purchase
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskAttachment
import org.tasks.extensions.Context.getResourceUri
import org.tasks.kmp.org.tasks.themes.ColorProvider.BLUE_500
import org.tasks.themes.ThemeBase
import org.tasks.time.DateTime
import timber.log.Timber
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit

class Preferences @JvmOverloads constructor(
        private val context: Context,
        name: String? = getSharedPreferencesName(context)
) : QueryPreferences {
    private val listeners = ArrayList<SharedPreferences.OnSharedPreferenceChangeListener>()
    private val proxyListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            listeners.forEach { it.onSharedPreferenceChanged(prefs, key) }
        }
    private val prefs: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE).apply {
        registerOnSharedPreferenceChangeListener(proxyListener)
    }

    fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        listeners.add(listener)
    }

    fun androidBackupServiceEnabled() = getBoolean(R.string.p_backups_android_backup_enabled, true)

    fun showBackupWarnings() = !getBoolean(R.string.p_backups_ignore_warnings, false)

    fun addTasksToTop(): Boolean = getBoolean(R.string.p_add_to_top, true)

    fun backButtonSavesTask(): Boolean = getBoolean(R.string.p_back_button_saves_task, false)

    val isCurrentlyQuietHours: Boolean
        get() {
            if (quietHoursEnabled()) {
                val dateTime = DateTime()
                val start = dateTime.withMillisOfDay(quietHoursStart)
                val end = dateTime.withMillisOfDay(quietHoursEnd)
                return if (start.isAfter(end)) {
                    dateTime.isBefore(end) || dateTime.isAfter(start)
                } else {
                    dateTime.isAfter(start) && dateTime.isBefore(end)
                }
            }
            return false
        }

    fun adjustForQuietHours(time: Long): Long {
        if (quietHoursEnabled()) {
            val dateTime = DateTime(time)
            val start = dateTime.withMillisOfDay(quietHoursStart)
            val end = dateTime.withMillisOfDay(quietHoursEnd)
            if (start.isAfter(end)) {
                if (dateTime.isBefore(end)) {
                    return end.millis
                } else if (dateTime.isAfter(start)) {
                    return end.plusDays(1).millis
                }
            } else {
                if (dateTime.isAfter(start) && dateTime.isBefore(end)) {
                    return end.millis
                }
            }
        }
        return time
    }

    private fun quietHoursEnabled(): Boolean = getBoolean(R.string.p_rmd_enable_quiet, false)

    val isDefaultDueTimeEnabled: Boolean
        get() = getBoolean(R.string.p_rmd_time_enabled, true)

    val defaultDueTime: Int
        get() = getInt(R.string.p_rmd_time, TimeUnit.HOURS.toMillis(18).toInt())

    private val quietHoursStart: Int
        get() = getMillisPerDayPref(R.string.p_rmd_quietStart, R.integer.default_quiet_hours_start)

    private val quietHoursEnd: Int
        get() = getMillisPerDayPref(R.string.p_rmd_quietEnd, R.integer.default_quiet_hours_end)

    val dateShortcutMorning: Int
        get() = getMillisPerDayPref(R.string.p_date_shortcut_morning, R.integer.default_morning)

    val dateShortcutAfternoon: Int
        get() = getMillisPerDayPref(R.string.p_date_shortcut_afternoon, R.integer.default_afternoon)

    val dateShortcutEvening: Int
        get() = getMillisPerDayPref(R.string.p_date_shortcut_evening, R.integer.default_evening)

    val purchases: List<Purchase>
        get() = try {
            getStringSet(R.string.p_purchases).map(::Purchase)
        } catch (e: Exception) {
            Timber.e(e)
            emptyList()
        }

    fun setPurchases(purchases: Collection<Purchase>) {
        setPurchases(purchases.map(Purchase::toJson).toHashSet())
    }

    private fun setPurchases(set: HashSet<String>) {
        try {
            setStringSet(R.string.p_purchases, set)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    val dateShortcutNight: Int
        get() = getMillisPerDayPref(R.string.p_date_shortcut_night, R.integer.default_night)

    private fun getMillisPerDayPref(resId: Int, defResId: Int): Int {
        val setting = getInt(resId, -1)
        return if (setting < 0 || setting > DateTime.MAX_MILLIS_PER_DAY) {
            context.resources.getInteger(defResId)
        } else setting
    }

    val isDefaultCalendarSet: Boolean
        get() {
            val defaultCalendar = defaultCalendar
            return defaultCalendar != null && defaultCalendar != "-1" && defaultCalendar != "0"
        }

    val completionSound: Uri?
        get() = getRingtone(
            R.string.p_completion_ringtone,
            context.getResourceUri(R.raw.long_rising_tone)
        )

    private fun getRingtone(pref: Int, default: Uri): Uri? {
        val ringtone = getStringValue(pref)
        return when {
            ringtone == null -> default
            ringtone.isNotBlank() -> ringtone.toUri()
            else -> null
        }
    }

    val isTrackingEnabled: Boolean
        get() = getBoolean(R.string.p_collect_statistics, true)

    val defaultCalendar: String?
        get() = getStringValue(R.string.gcal_p_default)

    val showEditScreenWithoutUnlock: Boolean
        get() = getBoolean(R.string.p_show_edit_screen_without_unlock, false)

    @SuppressLint("ApplySharedPref")
    fun clear() {
        prefs.edit().clear().commit()
    }

    fun setDefaults() {
        prefs.unregisterOnSharedPreferenceChangeListener(proxyListener)
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true)
        PreferenceManager.setDefaultValues(context, R.xml.preferences_look_and_feel, true)
        PreferenceManager.setDefaultValues(context, R.xml.preferences_notifications, true)
        PreferenceManager.setDefaultValues(context, R.xml.preferences_task_defaults, true)
        PreferenceManager.setDefaultValues(context, R.xml.preferences_date_and_time, true)
        PreferenceManager.setDefaultValues(context, R.xml.preferences_navigation_drawer, true)
        PreferenceManager.setDefaultValues(context, R.xml.preferences_backups, true)
        PreferenceManager.setDefaultValues(context, R.xml.preferences_advanced, true)
        PreferenceManager.setDefaultValues(context, R.xml.help_and_feedback, true)
        BeastModePreferences.setDefaultOrder(this, context)
        prefs.registerOnSharedPreferenceChangeListener(proxyListener)
    }

    fun reset() {
        prefs.unregisterOnSharedPreferenceChangeListener(proxyListener)
        clear()
        setDefaults()
    }

    fun getStringValue(keyResource: Int): String? = getStringValue(context.getString(keyResource))

    fun getStringValue(key: String?): String? = try {
        prefs.getString(key, null)
    } catch (e: Exception) {
        Timber.e(e)
        null
    }

    val defaultAlarms: List<Alarm>
        get() = getStringSet(R.string.p_default_alarms, DEFAULT_ALARMS)
            .mapNotNull {
                try {
                    Json.decodeFromString<Alarm>(it)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
            .sortedWith(compareBy({ it.type }, { it.time }))

    fun setDefaultAlarms(alarms: List<Alarm>) {
        setStringSet(
            R.string.p_default_alarms,
            alarms.map { Json.encodeToString(it) }.toHashSet()
        )
    }

    val defaultRingMode: Int
        get() = getIntegerFromString(R.string.p_default_reminders_mode_key, 0)

    val fontSize: Int
        get() = getInt(R.string.p_fontSize, 16)

    fun getIntegerFromString(keyResource: Int, defaultValue: Int): Int =
        getIntegerFromString(context.getString(keyResource), defaultValue)

    fun getIntegerFromString(keyResource: String?, defaultValue: Int): Int =
        getStringValue(keyResource)?.toIntOrNull() ?: defaultValue

    private fun getUri(key: Int): Uri? {
        val uri = getStringValue(key)
        return if (isNullOrEmpty(uri)) null else Uri.parse(uri)
    }

    fun setUri(key: Int, uri: URI) {
        setString(key, uri.toString())
    }

    fun setUri(key: Int, uri: Uri) {
        setString(key, uri.toString())
    }

    fun setString(key: Int, newValue: String?) {
        setString(context.getString(key), newValue)
    }

    fun setString(key: String?, newValue: String?) {
        Timber.d("Setting $key -> ${newValue?.let { if (BuildConfig.DEBUG) it else if (it.isBlank()) "" else "<redacted>" }}")
        val editor = prefs.edit()
        editor.putString(key, newValue)
        editor.apply()
    }

    fun setStringSet(key: Int, newValue: Set<String>) =
        setStringSet(context.getString(key), newValue)

    fun setStringSet(key: String, newValue: Set<String>) {
        Timber.d("Setting $key -> ${newValue.size} items")
        val editor = prefs.edit()
        editor.putStringSet(key, newValue)
        editor.apply()
    }

    internal fun getStringSet(key: Int, defaultValue: Set<String> = emptySet()) =
        getStringSet(context.getString(key), defaultValue)

    private fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> =
        prefs.getStringSet(key, defaultValue)!!

    fun setStringFromInteger(keyResource: Int, newValue: Int) {
        val editor = prefs.edit()
        val key = context.getString(keyResource)
        Timber.d("Setting $key -> $newValue")
        editor.putString(key, newValue.toString())
        editor.apply()
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean = try {
        prefs.getBoolean(key, defValue)
    } catch (e: ClassCastException) {
        Timber.w(e)
        defValue
    }

    fun getBoolean(keyResources: Int, defValue: Boolean): Boolean =
            getBoolean(context.getString(keyResources), defValue)

    fun setBoolean(keyResource: Int, value: Boolean) {
        setBoolean(context.getString(keyResource), value)
    }

    fun setBoolean(key: String?, value: Boolean) {
        Timber.d("Setting $key -> $value")
        val editor = prefs.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getInt(resourceId: Int, defValue: Int): Int =
            getInt(context.getString(resourceId), defValue)

    fun getInt(key: String?, defValue: Int): Int = prefs.getInt(key, defValue)

    fun setInt(resourceId: Int, value: Int) {
        setInt(context.getString(resourceId), value)
    }

    fun setInt(key: String?, value: Int) {
        Timber.d("Setting $key -> $value")
        val editor = prefs.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun getLong(resourceId: Int, defValue: Long): Long =
            getLong(context.getString(resourceId), defValue)

    fun getLong(key: String?, defValue: Long): Long = prefs.getLong(key, defValue)

    fun setLong(resourceId: Int, value: Long) {
        setLong(context.getString(resourceId), value)
    }

    fun setLong(key: String?, value: Long) {
        Timber.d("Setting $key -> $value")
        val editor = prefs.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun clear(key: String?) {
        Timber.d("Clearing $key")
        val editor = prefs.edit()
        editor.remove(key)
        editor.apply()
    }

    val lastSetVersion: Int
        get() = getInt(R.string.p_current_version, 0)

    fun setCurrentVersion(version: Int) {
        setInt(R.string.p_current_version, version)
    }

    var installVersion: Int
        get() = getInt(R.string.p_install_version, 0)
        set(value) = setInt(R.string.p_install_version, value)

    var installDate: Long
        get() = getLong(R.string.p_install_date, 0L)
        set(value) = setLong(R.string.p_install_date, value)

    var deviceInstallVersion: Int
        get() = getInt(R.string.p_device_install_version, 0)
        set(value) = setInt(R.string.p_device_install_version, value)

    override var sortMode: Int
        get() = getInt(R.string.p_sort_mode, SortHelper.SORT_DUE)
        set(value) { setInt(R.string.p_sort_mode, value) }

    override var groupMode: Int
        get() = getInt(R.string.p_group_mode, SortHelper.SORT_DUE)
        set(value) { setInt(R.string.p_group_mode, value) }

    override var completedMode: Int
        get() = getInt(R.string.p_completed_mode, SortHelper.SORT_COMPLETED)
        set(value) { setInt(R.string.p_completed_mode, value) }

    override var subtaskMode: Int
        get() = getInt(R.string.p_subtask_mode, SortHelper.SORT_MANUAL)
        set(value) { setInt(R.string.p_subtask_mode, value) }

    override var showHidden: Boolean
        get() = getBoolean(R.string.p_show_hidden_tasks, true)
        set(value) { setBoolean(R.string.p_show_hidden_tasks, value) }

    override var showCompleted: Boolean
        get() = getBoolean(R.string.p_show_completed_tasks, true)
        set(value) { setBoolean(R.string.p_show_completed_tasks, value) }

    override var alwaysDisplayFullDate: Boolean
        get() = getBoolean(R.string.p_always_display_full_date, false)
        set(value) { setBoolean(R.string.p_always_display_full_date, value)}

    override var completedTasksAtBottom: Boolean
        get() = getBoolean(R.string.p_completed_tasks_at_bottom, true)
        set(value) { setBoolean(R.string.p_completed_tasks_at_bottom, value) }

    val backupDirectory: Uri?
        get() = getDirectory(R.string.p_backup_dir, "backups")

    val appPrivateStorage: Uri
        get() = root.uri

    val attachmentsDirectory: Uri?
        get() = getDirectory(R.string.p_attachment_dir, TaskAttachment.FILES_DIRECTORY_DEFAULT)

    private fun getDirectory(pref: Int, name: String): Uri? {
        val uri = getUri(pref)
        if (uri != null) {
            when (uri.scheme) {
                ContentResolver.SCHEME_FILE -> {
                    val file = File(uri.path)
                    try {
                        if (file.canWrite()) {
                            return uri
                        }
                    } catch (ignored: SecurityException) {
                    }
                }
                ContentResolver.SCHEME_CONTENT -> if (hasWritePermission(context, uri)) {
                    return uri
                }
            }
        }
        return getDefaultDirectory(name)
    }

    private fun getDefaultDirectory(name: String): Uri? =
            root
                    .createDirectory(name)
                    ?.uri
                    ?: getDefaultFileLocation(name)?.let { Uri.fromFile(it) }

    private val root: DocumentFile
        get() = DocumentFile.fromFile(context.getExternalFilesDir(null) ?: context.filesDir)

    private fun getDefaultFileLocation(type: String): File? {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val path = File(baseDir, type)
        return if (path.isDirectory || path.mkdirs()) path else null
    }

    private fun hasWritePermission(context: Context, uri: Uri): Boolean =
            (PackageManager.PERMISSION_GRANTED
                    == context.checkUriPermission(
                    uri,
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))

    fun remove(resId: Int) {
        val editor = prefs.edit()
        editor.remove(context.getString(resId))
        editor.apply()
    }

    fun bundleNotifications(): Boolean = getBoolean(R.string.p_bundle_notifications, true)

    fun usePersistentReminders(): Boolean =
            AndroidUtilities.preUpsideDownCake() && getBoolean(R.string.p_rmd_persistent, true)

    fun useSwipeToSnooze(): Boolean =
            getBoolean(R.string.p_rmd_swipe_to_snooze_enabled, false)

    fun swipeToSnoozeIntervalMS(): Long =
        TimeUnit.MINUTES.toMillis(
            getIntegerFromString(R.string.p_rmd_swipe_to_snooze_time_minutes, 0).toLong()
        )

    var lastSync: Long
        get() = getLong(R.string.p_last_sync, 0L)
        set(value) {
            setLong(R.string.p_last_sync, value)
        }

    fun <T> getPrefs(c: Class<T>): Map<String, T> =
        prefs.all.filter { (_, value) -> c.isInstance(value) } as Map<String, T>

    val isPerListSortEnabled: Boolean
        get() = getBoolean(R.string.p_per_list_sort, false)

    override var isManualSort: Boolean
        get() = getBoolean(R.string.p_manual_sort, false)
        set(value) { setBoolean(R.string.p_manual_sort, value) }

    val isAstridSortEnabled: Boolean
        get() = getBoolean(R.string.p_astrid_sort_enabled, false)

    override var isAstridSort: Boolean
        get() = isAstridSortEnabled && getBoolean(R.string.p_astrid_sort, false)
        set(value) {
            setBoolean(R.string.p_astrid_sort, value)
        }

    override var sortAscending: Boolean
        get() = getBoolean(R.string.p_sort_ascending, true)
        set(value) { setBoolean(R.string.p_sort_ascending, value) }

    override var groupAscending: Boolean
        get() = getBoolean(R.string.p_group_ascending, true)
        set(value) { setBoolean(R.string.p_group_ascending, value) }

    override var completedAscending: Boolean
        get() = getBoolean(R.string.p_completed_ascending, false)
        set(value) { setBoolean(R.string.p_completed_ascending, value) }

    override var subtaskAscending: Boolean
        get() = getBoolean(R.string.p_subtask_ascending, false)
        set(value) { setBoolean(R.string.p_subtask_ascending, value) }

    val defaultPriority: Int
        get() = getIntegerFromString(R.string.p_default_importance_key, Task.Priority.LOW)

    val themeBase: Int
        get() = getInt(R.string.p_theme, ThemeBase.DEFAULT_BASE_THEME)

    val showSubtaskChip: Boolean
        get() = getBoolean(R.string.p_subtask_chips, true)

    val showStartDateChip: Boolean
        get() = getBoolean(R.string.p_start_date_chip, true)

    val showPlaceChip: Boolean
        get() = getBoolean(R.string.p_place_chips, true)

    val showListChip: Boolean
        get() = getBoolean(R.string.p_list_chips, true)

    val showTagChip: Boolean
        get() = getBoolean(R.string.p_tag_chips, true)

    fun alreadyNotified(account: String?, scope: String?): Boolean =
            getBoolean(context.getString(R.string.p_notified_oauth_error, account, scope), false)

    fun setAlreadyNotified(account: String?, scope: String?, value: Boolean) {
        setBoolean(context.getString(R.string.p_notified_oauth_error, account, scope), value)
    }

    val defaultThemeColor: Int
        get() = getInt(R.string.p_theme_color, BLUE_500)

    val dynamicColor: Boolean
        get() = getBoolean(R.string.p_dynamic_color, false)

    val markdown: Boolean
        get() = getBoolean(R.string.p_markdown, false)

    var lastReviewRequest: Long
        get() = getLong(R.string.p_last_review_request, 0L)
        set(value) = setLong(R.string.p_last_review_request, value)

    var warnNotificationsDisabled: Boolean
        get() = getBoolean(R.string.p_warn_notifications_disabled, true)
        set(value) = setBoolean(R.string.p_warn_notifications_disabled, value)

    var warnAlarmsDisabled: Boolean
        get() = getBoolean(R.string.p_warn_alarms_disabled, true)
        set(value) = setBoolean(R.string.p_warn_alarms_disabled, value)

    var warnQuietHoursDisabled: Boolean
        get() = getBoolean(R.string.p_warn_quiet_hours_enabled, true)
        set(value) = setBoolean(R.string.p_warn_quiet_hours_enabled, value)

    var warnMicrosoft: Boolean
        get() = getBoolean(R.string.p_warn_microsoft, true)
        set(value) = setBoolean(R.string.p_warn_microsoft, value)

    var warnGoogleTasks: Boolean
        get() = getBoolean(R.string.p_warn_google_tasks, true)
        set(value) = setBoolean(R.string.p_warn_google_tasks, value)

    var lastSubscribeRequest: Long
        get() = getLong(R.string.p_last_subscribe_request, 0L)
        set(value) = setLong(R.string.p_last_subscribe_request, value)

    var shownBeastModeHint: Boolean
        get() = getBoolean(R.string.p_shown_beast_mode_hint, false)
        set(value) = setBoolean(R.string.p_shown_beast_mode_hint, value)

    val linkify: Boolean
        get() = getBoolean(R.string.p_linkify_task_edit, false)

    val multilineTitle: Boolean
        get() = getBoolean(R.string.p_multiline_title, false)

    @OptIn(ExperimentalMaterial3Api::class)
    var calendarDisplayMode: DisplayMode
        get() = if (getIntegerFromString(R.string.p_picker_mode_date, 0) == 1)
            DisplayMode.Input else DisplayMode.Picker
        set(mode) {
            setStringFromInteger(R.string.p_picker_mode_date, if (mode == DisplayMode.Input) 1 else 0)
        }

    @OptIn(ExperimentalMaterial3Api::class)
    var timeDisplayMode: DisplayMode
        get() = if (getIntegerFromString(R.string.p_picker_mode_time, 0) == 1)
            DisplayMode.Input else DisplayMode.Picker
        set(mode) {
            setStringFromInteger(R.string.p_picker_mode_time, if (mode == DisplayMode.Input) 1 else 0)
        }

    companion object {
        private fun getSharedPreferencesName(context: Context): String =
                context.packageName + "_preferences"

        private val DEFAULT_ALARMS: Set<String> = setOf(
            Json.encodeToString(Alarm(time = 0, type = Alarm.TYPE_REL_START)),
            Json.encodeToString(Alarm(time = 0, type = Alarm.TYPE_REL_END)),
            Json.encodeToString(Alarm.whenOverdue(0)),
        )
    }
}
