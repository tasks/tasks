package org.tasks.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class LocationDaoBlocking @Inject constructor(private val dao: LocationDao) {
    fun insert(place: Place): Long = runBlocking {
        dao.insert(place)
    }

    fun getPlaceUsage(): LiveData<List<PlaceUsage>> {
        return dao.getPlaceUsage()
    }

    fun findPlace(latitude: String, longitude: String): Place? = runBlocking {
        dao.findPlace(latitude, longitude)
    }
}