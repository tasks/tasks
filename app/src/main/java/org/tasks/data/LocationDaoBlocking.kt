package org.tasks.data

import androidx.lifecycle.LiveData
import org.tasks.filters.LocationFilters
import org.tasks.time.DateTimeUtils.currentTimeMillis
import javax.inject.Inject

@Deprecated("use coroutines")
class LocationDaoBlocking @Inject constructor(private val dao: LocationDao) {
    fun getPlacesWithGeofences(): List<Place> = runBlocking {
        dao.getPlacesWithGeofences()
    }

    fun getGeofencesByPlace(uid: String): MergedGeofence? = runBlocking {
        dao.getGeofencesByPlace(uid)
    }

    fun deleteGeofencesByPlace(place: String) = runBlocking {
        dao.deleteGeofencesByPlace(place)
    }

    fun getArrivalGeofences(place: String, now: Long): List<Geofence> = runBlocking {
        dao.getArrivalGeofences(place, now)
    }

    fun getDepartureGeofences(place: String, now: Long): List<Geofence> = runBlocking {
        dao.getDepartureGeofences(place, now)
    }

    fun getGeofences(taskId: Long): Location? = runBlocking {
        dao.getGeofences(taskId)
    }

    fun getActiveGeofences(taskId: Long): List<Location> = runBlocking {
        dao.getActiveGeofences(taskId)
    }

    fun getPlaceForTask(taskId: Long): Place? = runBlocking {
        dao.getPlaceForTask(taskId)
    }

    fun getActiveGeofences(): List<Location> = runBlocking {
        dao.getActiveGeofences()
    }

    suspend fun geofenceCount(): Int {
        return dao.geofenceCount()
    }

    fun delete(location: Geofence) = runBlocking {
        dao.delete(location)
    }

    fun delete(place: Place) = runBlocking {
        dao.delete(place)
    }

    fun insert(location: Geofence): Long = runBlocking {
        dao.insert(location)
    }

    fun insert(place: Place): Long = runBlocking {
        dao.insert(place)
    }

    fun update(place: Place) = runBlocking {
        dao.update(place)
    }

    fun update(geofence: Geofence) = runBlocking {
        dao.update(geofence)
    }

    fun getByUid(uid: String): Place? = runBlocking {
        dao.getByUid(uid)
    }

    fun getGeofencesForTask(taskId: Long): List<Geofence> = runBlocking {
        dao.getGeofencesForTask(taskId)
    }

    fun getPlaces(): List<Place> = runBlocking {
        dao.getPlaces()
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

    fun getPlaceFilters(now: Long = currentTimeMillis()): List<LocationFilters> = runBlocking {
        dao.getPlaceFilters(now)
    }

    fun resetOrders() = runBlocking {
        dao.resetOrders()
    }

    fun setOrder(id: Long, order: Int) = runBlocking {
        dao.setOrder(id, order)
    }
}