package org.tasks.data

import androidx.room.Embedded
import org.tasks.data.entity.TagData

data class TagFilters(
    @JvmField @Embedded var tagData: TagData,
    @JvmField var count: Int,
)
