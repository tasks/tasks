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

class Preferences @JvmOverloads constructor(private val context: Context, name: String? = getSharedPreferencesName(context)) {
    private val prefs: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val publicPrefs: SharedPreferences = context.getSharedPreferences(AstridApiConstants.PUBLIC_PREFS, Context.MODE_PRIVATE)

    fun addTasksToTop(): Boolean {
        return getBoolean(R.string.p_add_to_top, true)
    }

    fun backButtonSavesTask(): Boolean {
        return getBoolean(R.string.p_back_button_saves_task, false)
    }

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

    private fun quietHoursEnabled(): Boolean {
        return getBoolean(R.string.p_rmd_enable_quiet, false)
    }

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
        try {
            val editor = prefs.edit()
            editor.putStringSet(
                    context.getString(R.string.p_purchases),
                    purchases.map(Purchase::toJson).toHashSet())
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

    fun getStringValue(key: String?): String? {
        return prefs.getString(key, null)
    }

    fun getStringValue(keyResource: Int): String? {
        return prefs.getString(context.getString(keyResource), null)
    }

    val defaultReminders: Int
        get() = getIntegerFromString(
                R.string.p_default_reminders_key, Task.NOTIFY_AT_DEADLINE or Task.NOTIFY_AFTER_DEADLINE)

    val defaultRingMode: Int
        get() = getIntegerFromString(R.string.p_default_reminders_mode_key, 0)

    val fontSize: Int
        get() = getInt(R.string.p_fontSize, 16)

    fun getIntegerFromString(keyResource: Int, defaultValue: Int): Int {
        return getIntegerFromString(context.getString(keyResource), defaultValue)
    }

    fun getIntegerFromString(keyResource: String?, defaultValue: Int): Int {
        val value = prefs.getString(keyResource, null) ?: return defaultValue
        return try {
            value.toInt()
        } catch (e: Exception) {
            Timber.e(e)
            defaultValue
        }
    }

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

    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return try {
            prefs.getBoolean(key, defValue)
        } catch (e: ClassCastException) {
            Timber.e(e)
            defValue
        }
    }

    fun getBoolean(keyResources: Int, defValue: Boolean): Boolean {
        return getBoolean(context.getString(keyResources), defValue)
    }

    fun setBoolean(keyResource: Int, value: Boolean) {
        setBoolean(context.getString(keyResource), value)
    }

    fun setBoolean(key: String?, value: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getInt(resourceId: Int, defValue: Int): Int {
        return getInt(context.getString(resourceId), defValue)
    }

    fun getInt(key: String?, defValue: Int): Int {
        return prefs.getInt(key, defValue)
    }

    fun setInt(resourceId: Int, value: Int) {
        setInt(context.getString(resourceId), value)
    }

    fun setInt(key: String?, value: Int) {
        val editor = prefs.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun getLong(resourceId: Int, defValue: Long): Long {
        return getLong(context.getString(resourceId), defValue)
    }

    fun getLong(key: String?, defValue: Long): Long {
        return prefs.getLong(key, defValue)
    }

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
        get() = getInt(P_CURRENT_VERSION, 0)

    fun setCurrentVersion(version: Int) {
        setInt(P_CURRENT_VERSION, version)
    }

    var sortMode: Int
        get() = publicPrefs.getInt(PREF_SORT_SORT, SortHelper.SORT_AUTO)
        set(value) {
            setPublicPref(PREF_SORT_SORT, value)
        }

    private fun setPublicPref(key: String, value: Int) {
        val edit = publicPrefs.edit()
        edit?.putInt(key, value)?.apply()
    }

    val backupDirectory: Uri?
        get() = getDirectory(R.string.p_backup_dir, "backups")

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
        val documentFile = DocumentFile.fromFile(context.getExternalFilesDir(null)!!).createDirectory(name)
        if (documentFile != null) {
            return documentFile.uri
        }
        val file = getDefaultFileLocation(name)
        return if (file != null) {
            Uri.fromFile(file)
        } else null
    }

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

    private fun hasWritePermission(context: Context, uri: Uri): Boolean {
        return (PackageManager.PERMISSION_GRANTED
                == context.checkUriPermission(
                uri,
                Binder.getCallingPid(),
                Binder.getCallingUid(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
    }

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

    fun bundleNotifications(): Boolean {
        return getBoolean(R.string.p_bundle_notifications, true)
    }

    fun usePersistentReminders(): Boolean {
        return getBoolean(R.string.p_rmd_persistent, true)
    }

    var isSyncOngoing: Boolean
        get() = getBoolean(R.string.p_sync_ongoing, false)
        set(value) {
            setBoolean(R.string.p_sync_ongoing, value)
        }

    fun useGoogleMaps(): Boolean {
        return getInt(R.string.p_map_provider, 0) == 1
    }

    fun useGooglePlaces(): Boolean {
        return getInt(R.string.p_place_provider, 0) == 1
    }

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

    val isManualSort: Boolean
        get() = getBoolean(R.string.p_manual_sort, false)

    val isAstridSort: Boolean
        get() = getBoolean(R.string.p_astrid_sort_enabled, false) && getBoolean(R.string.p_astrid_sort, false)

    val isReverseSort: Boolean
        get() = getBoolean(R.string.p_reverse_sort, false)

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

    fun alreadyNotified(account: String?, scope: String?): Boolean {
        return getBoolean(context.getString(R.string.p_notified_oauth_error, account, scope), false)
    }

    fun setAlreadyNotified(account: String?, scope: String?, value: Boolean) {
        setBoolean(context.getString(R.string.p_notified_oauth_error, account, scope), value)
    }

    val defaultThemeColor: Int
        get() = getInt(R.string.p_theme_color, ColorProvider.BLUE_500)

    fun usePagedQueries(): Boolean {
        return getBoolean(R.string.p_use_paged_queries, false)
    }

    fun showGroupHeaders(): Boolean {
        return !usePagedQueries() && !getBoolean(R.string.p_disable_sort_groups, false)
    }

    companion object {
        const val P_CURRENT_VERSION = "cv" // $NON-NLS-1$
        private const val PREF_SORT_SORT = "sort_sort" // $NON-NLS-1$
        private fun getSharedPreferencesName(context: Context): String {
            return context.packageName + "_preferences"
        }
    }
}