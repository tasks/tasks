package org.tasks.location

import android.location.Location
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.injection.InjectingTestCase
import org.tasks.time.DateTime
import javax.inject.Inject

@HiltAndroidTest
class LocationServiceAndroidTest : InjectingTestCase() {
    @Inject lateinit var service: LocationServiceAndroid
    @Inject lateinit var locationManager: MockLocationManager

    @Test
    fun sortByAccuracy() = runBlocking {
        newLocation(NETWORK_PROVIDER, 45.0, 46.0, 50f, DateTime(2021, 2, 4, 13, 35, 45, 121))
        newLocation(GPS_PROVIDER, 45.1, 46.1, 30f, DateTime(2021, 2, 4, 13, 33, 45, 121))

        assertEquals(MapPosition(45.1, 46.1), service.currentLocation())
    }

    @Test
    fun sortWithStaleLocation() = runBlocking {
        newLocation(GPS_PROVIDER, 45.1, 46.1, 30f, DateTime(2021, 2, 4, 13, 33, 44, 121))
        newLocation(NETWORK_PROVIDER, 45.0, 46.0, 50f, DateTime(2021, 2, 4, 13, 35, 45, 121))

        assertEquals(MapPosition(45.0, 46.0), service.currentLocation())
    }

    @Test
    fun useNewerUpdateWhenAccuracySame() = runBlocking {
        newLocation(GPS_PROVIDER, 45.1, 46.1, 50f, DateTime(2021, 2, 4, 13, 35, 45, 100))
        newLocation(NETWORK_PROVIDER, 45.0, 46.0, 50f, DateTime(2021, 2, 4, 13, 35, 45, 121))

        assertEquals(MapPosition(45.0, 46.0), service.currentLocation())
    }

    @Test
    fun returnCachedLocation() = runBlocking {
        newLocation(GPS_PROVIDER, 45.1, 46.1, 50f, DateTime(2021, 2, 4, 13, 35, 45, 100))

        service.currentLocation()

        locationManager.clearLocations()

        assertEquals(MapPosition(45.1, 46.1), service.currentLocation())
    }

    @Test
    fun nullWhenNoPosition() = runBlocking {
        assertNull(service.currentLocation())
    }

    private fun newLocation(
            provider: String,
            latitude: Double,
            longitude: Double,
            accuracy: Float,
            time: DateTime) {
        locationManager.addLocations(Location(provider).apply {
            this.latitude = latitude
            this.longitude = longitude
            this.accuracy = accuracy
            this.time = time.millis
        })
    }
}