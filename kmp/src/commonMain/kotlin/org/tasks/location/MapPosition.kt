package org.tasks.location

import org.tasks.CommonParcelable
import org.tasks.CommonParcelize

@CommonParcelize
data class MapPosition(
    val latitude: Double,
    val longitude: Double,
    val zoom: Float = 15.0f,
) : CommonParcelable
