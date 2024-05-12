package org.tasks.data

import androidx.room.Embedded
import org.tasks.data.entity.Place

data class LocationFilters(
    @JvmField @Embedded var place: Place,
    @JvmField var count: Int
)
