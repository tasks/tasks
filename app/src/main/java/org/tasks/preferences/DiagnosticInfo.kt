package org.tasks.preferences

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.BuildConfig
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.filters.CaldavFilter
import org.tasks.filters.CustomFilter
import org.tasks.filters.Filter
import org.tasks.filters.PlaceFilter
import org.tasks.filters.TagFilter
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class DiagnosticInfo @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionChecker: PermissionChecker,
    private val preferences: Preferences,
    private val caldavDao: CaldavDao,
    private val filterDao: FilterDao,
    private val tagDataDao: TagDataDao,
    private val locationDao: LocationDao,
    private val taskDao: TaskDao,
) {
    private fun isDontKeepActivitiesEnabled(): Boolean? {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ALWAYS_FINISH_ACTIVITIES) == 1
        } catch (e: Exception) {
            Timber.e("failed to fetch ${Settings.Global.ALWAYS_FINISH_ACTIVITIES}: ${e.message}")
            null
        }
    }

    val debugInfo: String
        get() = """
            ----------
            Tasks: ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR} build ${BuildConfig.VERSION_CODE})
            Android: ${Build.VERSION.RELEASE} (${Build.DISPLAY})
            Locale: ${Locale.getDefault()}
            Model: ${Build.MANUFACTURER} ${Build.MODEL}
            Product: ${Build.PRODUCT} (${Build.DEVICE})
            Kernel: ${System.getProperty("os.version")} (${Build.VERSION.INCREMENTAL})
            ----------
            notifications: ${permissionChecker.hasNotificationPermission()}
            reminders: ${permissionChecker.hasAlarmsAndRemindersPermission()}
            background location: ${permissionChecker.canAccessBackgroundLocation()}
            foreground location: ${permissionChecker.canAccessForegroundLocation()}
            calendar: ${permissionChecker.canAccessCalendars()}
            ----------
            dont keep activities: ${isDontKeepActivitiesEnabled()}
            ----------
        """.trimIndent()

    suspend fun getDiagnosticInfo(): String = buildString {
        appendLine("=== Accounts ===")
        val accounts = caldavDao.getAccounts().associateBy { it.uuid }
        accounts.values.forEach { account ->
            appendLine("$account")
        }

        appendLine()
        appendLine("=== Lists ===")
        caldavDao.getCalendars().forEach { calendar ->
            val account = accounts[calendar.account] ?: return@forEach
            val filter = CaldavFilter(calendar, account)
            appendLine("$calendar: ${getStats(filter)}")
        }

        appendLine()
        appendLine("=== Tags ===")
        tagDataDao.getAll().forEach { tag ->
            val filter = TagFilter(tag)
            appendLine("$tag: ${getStats(filter)}")
        }

        appendLine()
        appendLine("=== Places ===")
        locationDao.getPlaces().forEach { place ->
            val filter = PlaceFilter(place)
            appendLine("$place: ${getStats(filter)}")
        }

        appendLine()
        appendLine("=== Filters ===")
        filterDao.getFilters().forEach { filter ->
            appendLine("$filter ${getStats(CustomFilter(filter))}")
        }
    }

    private suspend fun getStats(filter: Filter): String {
        return try {
            val tasks = taskDao.fetchTasks(preferences, filter)
            val size = tasks.size
            val subtasks = tasks.count { it.parent != 0L }
            val indents = tasks.map { it.indent }
            val maxDepth = indents.maxOrNull() ?: 0
            val meanDepth = if (indents.isNotEmpty()) indents.average() else 0.0
            val medianDepth = indents.sorted().let { sorted ->
                if (sorted.isEmpty()) 0.0
                else if (sorted.size % 2 == 1) sorted[sorted.size / 2].toDouble()
                else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            }
            "tasks=$size, subtasks=$subtasks, maxDepth=$maxDepth, meanDepth=${"%.2f".format(meanDepth)}, medianDepth=${"%.2f".format(medianDepth)}"
        } catch (e: Exception) {
            e.toString()
        }
    }
}
