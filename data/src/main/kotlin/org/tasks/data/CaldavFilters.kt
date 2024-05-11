package org.tasks.data

import androidx.room.Embedded

data class CaldavFilters(
    @JvmField @Embedded val caldavCalendar: CaldavCalendar,
    @JvmField val count: Int,
    @JvmField val principals: Int,
)
