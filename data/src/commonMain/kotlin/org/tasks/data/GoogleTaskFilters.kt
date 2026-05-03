package org.tasks.data

import androidx.room.Embedded
import org.tasks.data.entity.CaldavCalendar

data class GoogleTaskFilters(
    @Embedded val googleTaskList: CaldavCalendar,
    val count: Int,
)
