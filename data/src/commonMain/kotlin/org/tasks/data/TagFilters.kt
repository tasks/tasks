package org.tasks.data

import androidx.room3.Embedded
import org.tasks.data.entity.TagData

data class TagFilters(
    @Embedded var tagData: TagData,
    var count: Int,
)
