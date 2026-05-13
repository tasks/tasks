package org.tasks.tasklist

import com.todoroo.astrid.core.SortHelper
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.tasks.data.dao.CaldavDao
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDay
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
) {
    private val listCache = HashMap<Long, String?>()

    fun headerStringBlocking(
        value: Long,
        groupMode: Int,
        alwaysDisplayFullDate: Boolean = false,
        style: DateStyle = DateStyle.FULL,
        compact: Boolean = false,
    ) = runBlocking {
        headerString(value, groupMode, alwaysDisplayFullDate, style, compact)
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
                listCache.getOrPut(value) {
                    caldavDao.getCalendarById(value)?.name
                } ?: "list: $value"
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
