package org.tasks.filters

import androidx.room.Embedded
import org.tasks.data.Place

data class LocationFilters(
    @JvmField @Embedded var place: Place,
    @JvmField var count: Int
) {
    fun toLocationFilter(): PlaceFilter {
        val filter = PlaceFilter(place)
        filter.count = count
        return filter
    }
}
