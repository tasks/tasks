package org.tasks.data

import androidx.room.Embedded

data class LocationFilters(
    @JvmField @Embedded var place: Place,
    @JvmField var count: Int
)
