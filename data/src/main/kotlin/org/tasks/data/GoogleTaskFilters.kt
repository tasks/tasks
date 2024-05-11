package org.tasks.data

import androidx.room.Embedded

data class GoogleTaskFilters(
    @JvmField @Embedded val googleTaskList: CaldavCalendar,
    @JvmField val count: Int,
)
