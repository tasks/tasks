package org.tasks.data

import androidx.lifecycle.LiveData
import javax.inject.Inject

@Deprecated("use coroutines")
class LocationDaoBlocking @Inject constructor(private val dao: LocationDao) {
    fun getPlacesWithGeofences(): List<Place> = runBlocking {
        dao.getPlacesWithGeofences()
    }

    fun getGeofencesByPlace(uid: String): MergedGeofence? = runBlocking {
        dao.getGeofencesByPlace(uid)
    }

    fun getPlaceForTask(taskId: Long): Place? = runBlocking {
        dao.getPlaceForTask(taskId)
    }

    fun insert(location: Geofence): Long = runBlocking {
        dao.insert(location)
    }

    fun insert(place: Place): Long = runBlocking {
        dao.insert(place)
    }

    fun getGeofencesForTask(taskId: Long): List<Geofence> = runBlocking {
        dao.getGeofencesForTask(taskId)
    }

    fun getPlace(id: Long): Place? = runBlocking {
        dao.getPlace(id)
    }

    fun getPlace(uid: String): Place? = runBlocking {
        dao.getPlace(uid)
    }

    fun getPlaceUsage(): LiveData<List<PlaceUsage>> {
        return dao.getPlaceUsage()
    }

    fun findPlace(latitude: String, longitude: String): Place? = runBlocking {
        dao.findPlace(latitude, longitude)
    }
}