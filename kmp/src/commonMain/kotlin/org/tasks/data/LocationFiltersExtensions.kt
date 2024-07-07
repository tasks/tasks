package org.tasks.data

import org.tasks.filters.PlaceFilter

fun LocationFilters.toLocationFilter(): PlaceFilter = PlaceFilter(
    place = place,
    count = count,
)
