package org.tasks.data

import androidx.room.Embedded

data class TagFilters(
    @JvmField @Embedded var tagData: TagData,
    @JvmField var count: Int,
)
