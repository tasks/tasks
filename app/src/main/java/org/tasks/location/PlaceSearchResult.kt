package org.tasks.location

import org.tasks.data.entity.Place

data class PlaceSearchResult(
    val id: String,
    val name: String?,
    val address: String?,
    val place: Place? = null,
)
