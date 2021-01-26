package org.tasks.tasklist

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.core.SortHelper.*
import org.tasks.R
import org.tasks.date.DateTimeUtils.toDateTime
import java.time.format.FormatStyle
import java.util.*

data class AdapterSection(
        var firstPosition: Int,
        val value: Long,
        var sectionedPosition: Int = 0,
        var collapsed: Boolean = false
) {
    fun headerColor(context: Context, sortMode: Int) =
            ContextCompat.getColor(context, if ((sortMode == SORT_DUE || sortMode == SORT_START)
                    && value > 0
                    && value.toDateTime().plusDays(1).startOfDay().isBeforeNow) {
                R.color.overdue
            } else {
                R.color.text_secondary
            })

    fun headerString(
            context: Context,
            locale: Locale,
            sortMode: Int,
            alwaysDisplayFullDate: Boolean,
            style: FormatStyle = FormatStyle.FULL,
            compact: Boolean = false
    ): String =
            when {
                sortMode == SORT_IMPORTANCE -> context.getString(priorityToString())
                value == 0L -> context.getString(when (sortMode) {
                    SORT_DUE -> R.string.no_due_date
                    SORT_START -> R.string.no_start_date
                    else -> R.string.no_date
                })
                else -> {
                    val dateString = DateUtilities.getRelativeDay(
                            context, value, locale, style, alwaysDisplayFullDate, !compact
                    )
                    when {
                        compact -> dateString
                        sortMode == SORT_DUE ->
                            context.getString(R.string.sort_due_group, dateString)
                        sortMode == SORT_START ->
                            context.getString(R.string.sort_start_group, dateString)
                        sortMode == SORT_CREATED ->
                            context.getString(R.string.sort_created_group, dateString)
                        sortMode == SORT_MODIFIED ->
                            context.getString(R.string.sort_modified_group, dateString)
                        else -> throw IllegalArgumentException()
                    }
                }
            }

    @StringRes
    private fun priorityToString() = when (value) {
        0L -> R.string.filter_high_priority
        1L -> R.string.filter_medium_priority
        2L -> R.string.filter_low_priority
        else -> R.string.filter_no_priority
    }
}