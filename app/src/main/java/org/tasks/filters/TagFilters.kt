package org.tasks.filters

import androidx.room.Embedded
import com.todoroo.astrid.api.TagFilter
import org.tasks.data.TagData

data class TagFilters(
    @JvmField @Embedded var tagData: TagData,
    @JvmField var count: Int,
) {
    fun toTagFilter(): TagFilter {
        val filter = TagFilter(tagData)
        filter.count = count
        return filter
    }
}
