package org.tasks.data

import androidx.room3.Embedded
import org.tasks.data.entity.CaldavCalendar

data class CaldavFilters(
    @Embedded val caldavCalendar: CaldavCalendar,
    val count: Int,
    val principals: Int,
)
