package org.tasks.tasklist

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.todoroo.astrid.core.SortHelper.SORT_START
import org.tasks.R
import org.tasks.date.DateTimeUtils.toDateTime

fun AdapterSection.headerColor(
    context: Context,
    groupMode: Int,
    textColor: Int = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
) =
    if (groupMode == SORT_START
        && value > 0
        && value.toDateTime().plusDays(1).startOfDay().isBeforeNow
    ) {
        ContextCompat.getColor(context, R.color.overdue)
    } else {
        textColor
    }
