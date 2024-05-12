package org.tasks.data

import androidx.room.Embedded
import org.tasks.data.entity.CaldavCalendar

data class GoogleTaskFilters(
    @JvmField @Embedded val googleTaskList: CaldavCalendar,
    @JvmField val count: Int,
)
