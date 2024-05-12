package org.tasks.tasklist

import android.content.Context
import androidx.annotation.StringRes
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.core.SortHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.data.dao.CaldavDao
import org.tasks.preferences.Preferences
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

class HeaderFormatter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val locale: Locale,
    private val caldavDao: CaldavDao,
) {
    private val listCache = HashMap<Long, String?>()

    fun headerStringBlocking(
        value: Long,
        groupMode: Int = preferences.groupMode,
        alwaysDisplayFullDate: Boolean = preferences.alwaysDisplayFullDate,
        style: FormatStyle = FormatStyle.FULL,
        compact: Boolean = false,
    ) = runBlocking {
        headerString(value, groupMode, alwaysDisplayFullDate, style, compact)
    }

    private suspend fun headerString(
        value: Long,
        groupMode: Int = preferences.groupMode,
        alwaysDisplayFullDate: Boolean = preferences.alwaysDisplayFullDate,
        style: FormatStyle = FormatStyle.FULL,
        compact: Boolean = false
    ): String =
        when {
            value == SectionedDataSource.HEADER_COMPLETED -> context.getString(R.string.completed)
            groupMode == SortHelper.SORT_IMPORTANCE -> context.getString(priorityToString(value))
            groupMode == SortHelper.SORT_LIST ->
                listCache.getOrPut(value) { caldavDao.getCalendarById(value)?.name }?: "list: $value"
            value == SectionedDataSource.HEADER_OVERDUE -> context.getString(R.string.filter_overdue)
            value == 0L -> context.getString(when (groupMode) {
                SortHelper.SORT_DUE -> R.string.no_due_date
                SortHelper.SORT_START -> R.string.no_start_date
                else -> R.string.no_date
            })
            else -> {
                val dateString = DateUtilities.getRelativeDay(
                    context, value, locale, style, alwaysDisplayFullDate, !compact
                )
                when {
                    compact -> dateString
                    groupMode == SortHelper.SORT_DUE ->
                        context.getString(R.string.sort_due_group, dateString)
                    groupMode == SortHelper.SORT_START ->
                        context.getString(R.string.sort_start_group, dateString)
                    groupMode == SortHelper.SORT_CREATED ->
                        context.getString(R.string.sort_created_group, dateString)
                    groupMode == SortHelper.SORT_MODIFIED ->
                        context.getString(R.string.sort_modified_group, dateString)
                    else -> throw IllegalArgumentException()
                }
            }
        }
    @StringRes
    private fun priorityToString(value: Long) = when (value) {
        0L -> R.string.filter_high_priority
        1L -> R.string.filter_medium_priority
        2L -> R.string.filter_low_priority
        else -> R.string.filter_no_priority
    }
}
