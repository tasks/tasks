package org.tasks.filters

import androidx.room.Embedded
import com.todoroo.astrid.api.CaldavFilter
import org.tasks.data.CaldavCalendar

data class CaldavFilters(
    @JvmField @Embedded val caldavCalendar: CaldavCalendar,
    @JvmField val count: Int,
    @JvmField val principals: Int,
) {
    fun toCaldavFilter(): CaldavFilter = CaldavFilter(
        calendar = caldavCalendar,
        principals = principals,
        count = count,
    )
}
