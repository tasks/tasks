package org.tasks.location

import android.app.PendingIntent
import android.location.Location

class MockLocationManager : LocationManager {
    private val mockLocations = ArrayList<Location>()

    fun addLocations(vararg locations: Location) {
        mockLocations.addAll(locations)
    }

    fun clearLocations() = mockLocations.clear()

    override val lastKnownLocations: List<Location>
        get() = mockLocations

    override fun addProximityAlert(
            latitude: Double,
            longitude: Double,
            radius: Float,
            intent: PendingIntent
    ) {}

    override fun removeProximityAlert(intent: PendingIntent) {}
}