package org.tasks.filters

import androidx.room.Embedded
import com.todoroo.astrid.api.GtasksFilter
import org.tasks.data.CaldavCalendar

data class GoogleTaskFilters(
    @JvmField @Embedded val googleTaskList: CaldavCalendar,
    @JvmField val count: Int,
) {
    fun toGtasksFilter(): GtasksFilter {
        val filter = GtasksFilter(googleTaskList)
        filter.count = count
        return filter
    }
}
