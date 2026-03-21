package org.tasks.data

import androidx.room.Embedded
import org.tasks.data.entity.Place

data class LocationFilters(
    @Embedded var place: Place,
    var count: Int
)
