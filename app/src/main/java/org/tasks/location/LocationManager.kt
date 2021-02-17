package org.tasks.location

import android.app.PendingIntent
import android.location.Location

interface LocationManager {
    val lastKnownLocations: List<Location>

    fun addProximityAlert(
            latitude: Double,
            longitude: Double,
            radius: Float,
            intent: PendingIntent
    )

    fun removeProximityAlert(intent: PendingIntent)
}