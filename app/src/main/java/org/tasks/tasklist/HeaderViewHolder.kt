package org.tasks.tasklist

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.core.SortHelper
import org.tasks.R
import org.tasks.date.DateTimeUtils.newDateTime
import java.time.format.FormatStyle
import java.util.*

class HeaderViewHolder(
        private val context: Context,
        private val locale: Locale,
        view: View,
        callback: (Long) -> Unit) : RecyclerView.ViewHolder(view) {

    private val header: TextView = view.findViewById(R.id.header)
    private var sortGroup = -1L

    fun bind(filter: Filter, sortMode: Int, alwaysDisplayFullDate: Boolean, section: AdapterSection) {
        sortGroup = section.value
        val header: String? = if (filter.supportsSorting()) getHeader(sortMode, alwaysDisplayFullDate, sortGroup) else null

        if (header == null) {
            this.header.visibility = View.GONE
        } else {
            this.header.visibility = View.VISIBLE
            this.header.text = header
            this.header.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, if (section.collapsed) R.drawable.ic_keyboard_arrow_down_black_18dp else R.drawable.ic_keyboard_arrow_up_black_18dp, 0)
            this.header.setTextColor(
                    context.getColor(
                            if (sortMode == SortHelper.SORT_DUE && sortGroup > 0 && newDateTime(sortGroup).plusDays(1).startOfDay().isBeforeNow) R.color.overdue else R.color.text_secondary))
        }
    }

    private fun getHeader(sortMode: Int, alwaysDisplayFullDate: Boolean, group: Long): String =
            when {
                sortMode == SortHelper.SORT_IMPORTANCE -> context.getString(priorityToString(group.toInt()))
                group == 0L -> context.getString(if (sortMode == SortHelper.SORT_DUE) {
                    R.string.no_due_date
                } else {
                    R.string.no_date
                })
                sortMode == SortHelper.SORT_CREATED ->
                    context.getString(R.string.sort_created_group, getDateString(group, alwaysDisplayFullDate))
                sortMode == SortHelper.SORT_MODIFIED ->
                    context.getString(R.string.sort_modified_group, getDateString(group, alwaysDisplayFullDate))
                else -> getDateString(group, alwaysDisplayFullDate, false)
            }

    private fun getDateString(value: Long, alwaysDisplayFullDate: Boolean, lowercase: Boolean = true) =
            DateUtilities.getRelativeDay(context, value, locale, FormatStyle.FULL, alwaysDisplayFullDate, lowercase)

    @StringRes
    private fun priorityToString(priority: Int) = when (priority) {
        0 -> R.string.filter_high_priority
        1 -> R.string.filter_medium_priority
        2 -> R.string.filter_low_priority
        else -> R.string.filter_no_priority
    }

    init {
        header.setOnClickListener {
            callback.invoke(sortGroup)
        }
    }
}