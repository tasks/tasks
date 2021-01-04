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
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.todoroo.astrid.activity.BeastModePreferences
import com.todoroo.astrid.api.AstridApiConstants
import com.todoroo.astrid.core.SortHelper
import com.todoroo.astrid.data.Task
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.billing.Purchase
import org.tasks.data.TaskAttachment
import org.tasks.themes.ColorProvider
import org.tasks.themes.ThemeBase
import org.tasks.time.DateTime
import timber.log.Timber
import java.io.File
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

class Preferences @JvmOverloads constructor(
        private val context: Context,
        name: String? = getSharedPreferencesName(context)
) : QueryPreferences {
    private val prefs: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val publicPrefs: SharedPreferences = context.getSharedPreferences(AstridApiConstants.PUBLIC_PREFS, Context.MODE_PRIVATE)

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
            prefs.getStringSet(context.getString(R.string.p_purchases), emptySet())!!.map(::Purchase)
        } catch (e: Exception) {
            Timber.e(e)
            emptyList()
        }

    fun setPurchases(purchases: Collection<Purchase>) {
        setPurchases(purchases.mapNotNull(Purchase::toJson).toHashSet())
    }

    fun setPurchases(set: HashSet<String>) {
        try {
            val editor = prefs.edit()
            editor.putStringSet(context.getString(R.string.p_purchases), set)
            editor.apply()
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
        get() {
            val ringtone = getStringValue(R.string.p_rmd_ringtone)
                    ?: return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            return if ("" == ringtone) {
                null
            } else Uri.parse(ringtone)
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
        publicPrefs.edit().clear().commit()
        prefs.edit().clear().commit()
    }

    fun setDefaults() {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true)
        PreferenceManager.setDefaultValues(context, R.xml.preferences_look_and_feel, true)
        PreferenceManager.setDefaultValues(context, R.xml.preferences_notifications, true)
        PreferenceManager.setDefaultValues(context, R.xml.preferences_synchronization, true)
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

    val defaultReminders: Int
        get() = getIntegerFromString(
                R.string.p_default_reminders_key, Task.NOTIFY_AT_DEADLINE or Task.NOTIFY_AFTER_DEADLINE)

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
        val editor = prefs.edit()
        editor.putString(key, newValue)
        editor.apply()
    }

    fun setStringFromInteger(keyResource: Int, newValue: Int) {
        val editor = prefs.edit()
        editor.putString(context.getString(keyResource), newValue.toString())
        editor.apply()
    }

    fun getBoolean(key: String?, defValue: Boolean): Boolean = try {
        prefs.getBoolean(key, defValue)
    } catch (e: ClassCastException) {
        Timber.e(e)
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

    override var sortMode: Int
        get() = publicPrefs.getInt(PREF_SORT_SORT, SortHelper.SORT_AUTO)
        set(value) {
            setPublicPref(PREF_SORT_SORT, value)
        }

    override var showHidden: Boolean
        get() = getBoolean(R.string.p_show_hidden_tasks, false)
        set(value) { setBoolean(R.string.p_show_hidden_tasks, value) }

    override var showCompleted: Boolean
        get() = getBoolean(R.string.p_show_completed_tasks, false)
        set(value) { setBoolean(R.string.p_show_completed_tasks, value) }

    override val showCompletedTemporarily: Boolean
        get() = getBoolean(R.string.p_temporarily_show_completed_tasks, false)

    override var alwaysDisplayFullDate: Boolean
        get() = getBoolean(R.string.p_always_display_full_date, false)
        set(value) { setBoolean(R.string.p_always_display_full_date, value)}

    private fun setPublicPref(key: String, value: Int) {
        val edit = publicPrefs.edit()
        edit?.putInt(key, value)?.apply()
    }

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

    fun usePersistentReminders(): Boolean = getBoolean(R.string.p_rmd_persistent, true)

    var isSyncOngoing: Boolean
        get() = syncFlags.any { getBoolean(it, false) }
        set(value) {
            syncFlags.forEach { setBoolean(it, value) }
        }

    fun useGooglePlaces(): Boolean = getInt(R.string.p_place_provider, 0) == 1

    fun <T> getPrefs(c: Class<T>): Map<String, T> {
        val result: MutableMap<String, T> = HashMap()
        val entries: Iterable<Map.Entry<String, *>> = prefs.all.entries.filter { e: Map.Entry<String?, Any?> -> c.isInstance(e.value) }
        for ((key, value) in entries) {
            result[key] = value as T
        }
        return result
    }

    val isFlipperEnabled: Boolean
        get() = BuildConfig.DEBUG && getBoolean(R.string.p_flipper, false)

    val isPositionHackEnabled: Boolean
        get() = getLong(R.string.p_google_tasks_position_hack, 0) > 0

    override var isManualSort: Boolean
        get() = getBoolean(R.string.p_manual_sort, false)
        set(value) { setBoolean(R.string.p_manual_sort, value) }

    override var isAstridSort: Boolean
        get() = getBoolean(R.string.p_astrid_sort_enabled, false) && getBoolean(R.string.p_astrid_sort, false)
        set(value) { setBoolean(R.string.p_astrid_sort, value) }

    override var isReverseSort: Boolean
        get() = getBoolean(R.string.p_reverse_sort, false)
        set(value) { setBoolean(R.string.p_reverse_sort, value) }

    val defaultPriority: Int
        get() = getIntegerFromString(R.string.p_default_importance_key, Task.Priority.LOW)

    val themeBase: Int
        get() = getInt(R.string.p_theme, ThemeBase.DEFAULT_BASE_THEME)

    var showSubtaskChip: Boolean
        get() = getBoolean(R.string.p_subtask_chips, true)
        set(value) = setBoolean(R.string.p_subtask_chips, value)

    var showPlaceChip: Boolean
        get() = getBoolean(R.string.p_place_chips, true)
        set(value) = setBoolean(R.string.p_place_chips, value)

    var showListChip: Boolean
        get() = getBoolean(R.string.p_list_chips, true)
        set(value) = setBoolean(R.string.p_list_chips, value)

    var showTagChip: Boolean
        get() = getBoolean(R.string.p_tag_chips, true)
        set(value) = setBoolean(R.string.p_tag_chips, value)

    fun alreadyNotified(account: String?, scope: String?): Boolean =
            getBoolean(context.getString(R.string.p_notified_oauth_error, account, scope), false)

    fun setAlreadyNotified(account: String?, scope: String?, value: Boolean) {
        setBoolean(context.getString(R.string.p_notified_oauth_error, account, scope), value)
    }

    val defaultThemeColor: Int
        get() = getInt(R.string.p_theme_color, ColorProvider.BLUE_500)

    override fun usePagedQueries(): Boolean = getBoolean(R.string.p_use_paged_queries, false)

    fun showGroupHeaders(): Boolean =
            !usePagedQueries() && !getBoolean(R.string.p_disable_sort_groups, false)

    companion object {
        private const val PREF_SORT_SORT = "sort_sort" // $NON-NLS-1$

        private fun getSharedPreferencesName(context: Context): String =
                context.packageName + "_preferences"

        private val syncFlags = listOf(
                R.string.p_sync_ongoing_google_tasks,
                R.string.p_sync_ongoing_caldav,
                R.string.p_sync_ongoing_etesync,
                R.string.p_sync_ongoing_android)
    }
}