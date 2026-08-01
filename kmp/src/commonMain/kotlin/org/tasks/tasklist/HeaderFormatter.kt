package org.tasks.tasklist

import com.todoroo.astrid.core.SortHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.tasks.data.dao.CaldavDao
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDay
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.Volatile
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.completed
import tasks.kmp.generated.resources.filter_high_priority
import tasks.kmp.generated.resources.filter_low_priority
import tasks.kmp.generated.resources.filter_medium_priority
import tasks.kmp.generated.resources.filter_no_priority
import tasks.kmp.generated.resources.filter_overdue
import tasks.kmp.generated.resources.no_date
import tasks.kmp.generated.resources.no_due_date
import tasks.kmp.generated.resources.no_start_date
import tasks.kmp.generated.resources.sort_created_group
import tasks.kmp.generated.resources.sort_due_group
import tasks.kmp.generated.resources.sort_modified_group
import tasks.kmp.generated.resources.sort_start_group

class HeaderFormatter(
    private val caldavDao: CaldavDao,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    @Volatile
    private var cacheDay = Long.MIN_VALUE
    private val headerCache = ConcurrentHashMap<String, String>()

    @Volatile
    private var listNames: Map<Long, String?> = emptyMap()

    init {
        caldavDao
            .subscribeToCalendars()
            .onEach { calendars ->
                listNames = calendars.associate { it.id to it.name }
                // A rename changes header text, so anything already resolved is stale.
                headerCache.clear()
            }
            .launchIn(scope)
    }

    /**
     * Called from view binding on the main thread, and resolving a header means a string resource
     * load. Headers repeat constantly while scrolling, so results are memoized. Relative dates
     * ("today", "tomorrow") are only valid for the current day, so the cache is dropped when the
     * day rolls over, and again whenever a list is renamed.
     */
    fun headerStringBlocking(
        value: Long,
        groupMode: Int,
        alwaysDisplayFullDate: Boolean = false,
        style: DateStyle = DateStyle.FULL,
        compact: Boolean = false,
    ): String {
        val today = currentTimeMillis().startOfDay()
        if (today != cacheDay) {
            cacheDay = today
            headerCache.clear()
        }
        val key = "$value|$groupMode|$alwaysDisplayFullDate|$style|$compact"
        headerCache[key]?.let { return it }
        return runBlocking {
            headerString(value, groupMode, alwaysDisplayFullDate, style, compact)
        }.also { headerCache[key] = it }
    }

    suspend fun headerString(
        value: Long,
        groupMode: Int,
        alwaysDisplayFullDate: Boolean = false,
        style: DateStyle = DateStyle.FULL,
        compact: Boolean = false,
    ): String =
        when {
            value == SectionedDataSource.HEADER_COMPLETED ->
                getString(Res.string.completed)
            groupMode == SortHelper.SORT_IMPORTANCE ->
                getString(priorityToString(value))
            groupMode == SortHelper.SORT_LIST ->
                // Falls back to a query only before the first emission from subscribeToCalendars.
                (if (listNames.containsKey(value)) {
                    listNames[value]
                } else {
                    caldavDao.getCalendarById(value)?.name
                }) ?: "list: $value"
            value == SectionedDataSource.HEADER_OVERDUE ->
                getString(Res.string.filter_overdue)
            value == 0L -> getString(
                when (groupMode) {
                    SortHelper.SORT_DUE -> Res.string.no_due_date
                    SortHelper.SORT_START -> Res.string.no_start_date
                    else -> Res.string.no_date
                }
            )
            else -> {
                val dateString = getRelativeDay(
                    value,
                    style,
                    alwaysDisplayFullDate = alwaysDisplayFullDate,
                    lowercase = !compact,
                )
                when {
                    compact -> dateString
                    groupMode == SortHelper.SORT_DUE ->
                        getString(Res.string.sort_due_group, dateString)
                    groupMode == SortHelper.SORT_START ->
                        getString(Res.string.sort_start_group, dateString)
                    groupMode == SortHelper.SORT_CREATED ->
                        getString(Res.string.sort_created_group, dateString)
                    groupMode == SortHelper.SORT_MODIFIED ->
                        getString(Res.string.sort_modified_group, dateString)
                    else -> throw IllegalArgumentException()
                }
            }
        }

    private fun priorityToString(value: Long) = when (value) {
        0L -> Res.string.filter_high_priority
        1L -> Res.string.filter_medium_priority
        2L -> Res.string.filter_low_priority
        else -> Res.string.filter_no_priority
    }
}
