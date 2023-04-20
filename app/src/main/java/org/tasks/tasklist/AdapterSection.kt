package org.tasks.tasklist

import android.content.Context
import androidx.core.content.ContextCompat
import com.todoroo.astrid.core.SortHelper.SORT_START
import org.tasks.R
import org.tasks.date.DateTimeUtils.toDateTime

data class AdapterSection(
        var firstPosition: Int,
        val value: Long,
        var sectionedPosition: Int = 0,
        var collapsed: Boolean = false
) {
    fun headerColor(context: Context, sortMode: Int, textColor: Int = R.color.text_secondary) =
            ContextCompat.getColor(context, if (sortMode == SORT_START
                    && value > 0
                    && value.toDateTime().plusDays(1).startOfDay().isBeforeNow) {
                R.color.overdue
            } else {
                textColor
            })
}