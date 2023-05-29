package org.tasks.preferences

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Binder
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.activity.BeastModePreferences
import com.todoroo.astrid.core.SortHelper
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.NOTIFY_AFTER_DEADLINE
import com.todoroo.astrid.data.Task.Companion.NOTIFY_AT_DEADLINE
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.billing.Purchase
import org.tasks.data.TaskAttachment
import org.tasks.extensions.Context.getResourceUri
import org.tasks.themes.ColorProvider
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
    private val prefs: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

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

    val ringtone: Uri?
        get() = getRingtone(
            R.string.p_rmd_ringtone,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        )

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

    val firstDayOfWeek: Int
        get() {
            val firstDayOfWeek = getIntegerFromString(R.string.p_start_of_week, 0)
            return if (firstDayOfWeek < 1 || firstDayOfWeek > 7) 0 else firstDayOfWeek
        }

    @SuppressLint("ApplySharedPref")
    fun clear() {
        prefs.edit().clear().commit()
    }

    fun setDefaults() {
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
    }

    fun reset() {
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

    val defaultRemindersSet: Set<String>
        get() = getStringSet(
            R.string.p_default_reminders_key,
            hashSetOf(NOTIFY_AT_DEADLINE.toString(), NOTIFY_AFTER_DEADLINE.toString())
        )

    val defaultReminders: Int
        get() = defaultRemindersSet
            .mapNotNull { it.toIntOrNull() }
            .sum()

    val defaultRingMode: Int
        get() = getIntegerFromString(R.string.p_default_reminders_mode_key, 0)

    val fontSize: Int
        get() = getInt(R.string.p_fontSize, 16)

    val mapTheme: Int
        get() = getIntegerFromString(R.string.p_map_theme, 0)

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
        val editor = prefs.edit()
        editor.putString(key, newValue)
        editor.apply()
    }

    fun setStringSet(key: Int, newValue: Set<String>) =
        setStringSet(context.getString(key), newValue)

    fun setStringSet(key: String, newValue: Set<String>) {
        val editor = prefs.edit()
        editor.putStringSet(key, newValue)
        editor.apply()
    }

    private fun getStringSet(key: Int, defaultValue: Set<String> = emptySet()) =
        getStringSet(context.getString(key), defaultValue)

    private fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> =
        prefs.getStringSet(key, defaultValue)!!

    fun setStringFromInteger(keyResource: Int, newValue: Int) {
        val editor = prefs.edit()
        editor.putString(context.getString(keyResource), newValue.toString())
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
        val editor = prefs.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun clear(key: String?) {
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

    override var sortMode: Int
        get() = getInt(R.string.p_sort_mode, SortHelper.SORT_AUTO)
        set(value) { setInt(R.string.p_sort_mode, value) }

    override var groupMode: Int
        get() = getInt(R.string.p_group_mode, SortHelper.GROUP_NONE)
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

    val externalStorage: Uri
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
        get() = DocumentFile.fromFile(context.getExternalFilesDir(null)!!)

    private fun getDefaultFileLocation(type: String): File? {
        val externalFilesDir = context.getExternalFilesDir(null) ?: return null
        val path = String.format("%s/%s", externalFilesDir.absolutePath, type)
        val file = File(path)
        return if (file.isDirectory || file.mkdirs()) file else null
    }

    val cacheDirectory: Uri
        get() {
            var cacheDir = context.externalCacheDir
            if (cacheDir == null) {
                cacheDir = context.cacheDir
            }
            return DocumentFile.fromFile(cacheDir!!).uri
        }

    private fun hasWritePermission(context: Context, uri: Uri): Boolean =
            (PackageManager.PERMISSION_GRANTED
                    == context.checkUriPermission(
                    uri,
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))

    val notificationDefaults: Int
        get() {
            var result = 0
            if (getBoolean(R.string.p_rmd_vibrate, true)) {
                result = result or NotificationCompat.DEFAULT_VIBRATE
            }
            if (getBoolean(R.string.p_led_notification, true)) {
                result = result or NotificationCompat.DEFAULT_LIGHTS
            }
            return result
        }

    fun remove(resId: Int) {
        val editor = prefs.edit()
        editor.remove(context.getString(resId))
        editor.apply()
    }

    fun bundleNotifications(): Boolean = getBoolean(R.string.p_bundle_notifications, true)

    fun usePersistentReminders(): Boolean =
        AndroidUtilities.preUpsideDownCake() && getBoolean(R.string.p_rmd_persistent, true)

    var isSyncOngoing: Boolean
        get() = syncFlags.any { getBoolean(it, false) }
        set(value) {
            syncFlags.forEach { setBoolean(it, value) }
        }

    var lastSync: Long
        get() = getLong(R.string.p_last_sync, 0L)
        set(value) {
            setLong(R.string.p_last_sync, value)
        }

    fun <T> getPrefs(c: Class<T>): Map<String, T> =
        prefs.all.filter { (_, value) -> c.isInstance(value) } as Map<String, T>

    val isFlipperEnabled: Boolean
        get() = BuildConfig.DEBUG && getBoolean(R.string.p_flipper, false)

    var isPositionHackEnabled: Boolean
        get() = getLong(R.string.p_google_tasks_position_hack, 0) > now() - DateUtilities.ONE_WEEK
        set(value) { setLong(R.string.p_google_tasks_position_hack, if (value) now() else 0) }

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
        get() = getBoolean(R.string.p_group_ascending, false)
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
        get() = getInt(R.string.p_theme_color, ColorProvider.BLUE_500)

    val markdown: Boolean
        get() = getBoolean(R.string.p_markdown, false)

    val isTopAppBar: Boolean
        get() = getIntegerFromString(R.string.p_app_bar_position, 1) == 0

    var lastReviewRequest: Long
        get() = getLong(R.string.p_last_review_request, 0L)
        set(value) = setLong(R.string.p_last_review_request, value)

    var lastSubscribeRequest: Long
        get() = getLong(R.string.p_last_subscribe_request, 0L)
        set(value) = setLong(R.string.p_last_subscribe_request, value)

    var shownBeastModeHint: Boolean
        get() = getBoolean(R.string.p_shown_beast_mode_hint, false)
        set(value) = setBoolean(R.string.p_shown_beast_mode_hint, value)

    val desaturateDarkMode: Boolean
        get() = getBoolean(R.string.p_desaturate_colors, true)

    val linkify: Boolean
        get() = getBoolean(R.string.p_linkify_task_edit, false)

    companion object {
        private fun getSharedPreferencesName(context: Context): String =
                context.packageName + "_preferences"

        private val syncFlags = listOf(
                R.string.p_sync_ongoing,
                R.string.p_sync_ongoing_android,
        )
    }
}