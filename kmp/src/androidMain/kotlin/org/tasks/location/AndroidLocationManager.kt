package org.tasks.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import co.touchlab.kermit.Logger

open class AndroidLocationManager(
        private val context: Context,
) : LocationManager {

    private val systemLocationManager
        get() = context.getSystemService(android.location.LocationManager::class.java)

    override val lastKnownLocations: List<Location>
        get() = systemLocationManager.allProviders.mapNotNull {
            systemLocationManager.getLastKnownLocationOrNull(it)
        }

    @SuppressLint("MissingPermission")
    override fun addProximityAlert(
            latitude: Double,
            longitude: Double,
            radius: Float,
            intent: PendingIntent
    ) = systemLocationManager.addProximityAlert(latitude, longitude, radius, -1, intent)

    override fun removeProximityAlert(intent: PendingIntent) =
            systemLocationManager.removeProximityAlert(intent)

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates(
            provider: String,
            minTimeMs: Long,
            minDistanceM: Float,
            intent: PendingIntent
    ) {
        systemLocationManager.requestLocationUpdates(provider, minTimeMs, minDistanceM, intent)
    }

    override fun removeLocationUpdates(intent: PendingIntent) {
        systemLocationManager.removeUpdates(intent)
    }

    companion object {
        @SuppressLint("MissingPermission")
        private fun android.location.LocationManager.getLastKnownLocationOrNull(provider: String) =
                try {
                    getLastKnownLocation(provider)
                } catch (e: Exception) {
                    Logger.e("AndroidLocationManager", e) { "Failed to get last known location" }
                    null
                }
    }
}
