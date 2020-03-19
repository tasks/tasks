package org.tasks.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.caldav.GeoUtils.toLikeString
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.GeofenceMaker.ARRIVAL
import org.tasks.makers.GeofenceMaker.DEPARTURE
import org.tasks.makers.GeofenceMaker.PLACE
import org.tasks.makers.GeofenceMaker.TASK
import org.tasks.makers.GeofenceMaker.newGeofence
import org.tasks.makers.PlaceMaker.LATITUDE
import org.tasks.makers.PlaceMaker.LONGITUDE
import org.tasks.makers.PlaceMaker.newPlace
import org.tasks.makers.TaskMaker.*
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class LocationDaoTest : InjectingTestCase() {

    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var taskDao: TaskDao

    @Test
    fun getExistingPlace() {
        locationDao.insert(newPlace(with(LATITUDE, 48.067222), with(LONGITUDE, 12.863611)))
        val place = locationDao.findPlace(48.067222.toLikeString(), 12.863611.toLikeString())
        assertEquals(48.067222, place?.latitude)
        assertEquals(12.863611, place?.longitude)
    }

    @Test
    fun getPlaceWithLessPrecision() {
        locationDao.insert(newPlace(with(LATITUDE, 50.7547), with(LONGITUDE, -2.2279)));
        val place = locationDao.findPlace(50.754712.toLikeString(), (-2.227945).toLikeString())
        assertEquals(50.7547, place?.latitude)
        assertEquals(-2.2279, place?.longitude)
    }

    @Test
    fun getPlaceWithMorePrecision() {
        locationDao.insert(newPlace(with(LATITUDE, 36.246944), with(LONGITUDE, -116.816944)))
        locationDao.places.forEach { println(it) }
        val place = locationDao.findPlace(36.2469.toLikeString(), (-116.8169).toLikeString())
        assertEquals(36.246944, place?.latitude)
        assertEquals(-116.816944, place?.longitude)
    }

    @Test
    fun noActiveGeofences() {
        val place = newPlace()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1)))
        locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid)))

        assertNull(locationDao.getGeofencesByPlace(place.uid))
    }

    @Test
    fun activeArrivalGeofence() {
        val place = newPlace()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1)))
        locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid), with(ARRIVAL, true)))

        val geofence = locationDao.getGeofencesByPlace(place.uid)

        assertTrue(geofence.arrival)
        assertFalse(geofence.departure)
    }

    @Test
    fun activeDepartureGeofence() {
        val place = newPlace()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1)))
        locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid), with(DEPARTURE, true)))

        val geofence = locationDao.getGeofencesByPlace(place.uid)

        assertFalse(geofence.arrival)
        assertTrue(geofence.departure)
    }

    @Test
    fun geofenceInactiveForCompletedTask() {
        val place = newPlace()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1), with(COMPLETION_TIME, newDateTime())))
        locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid), with(ARRIVAL, true)))

        assertNull(locationDao.getGeofencesByPlace(place.uid))
    }

    @Test
    fun geofenceInactiveForDeletedTask() {
        val place = newPlace()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1), with(DELETION_TIME, newDateTime())))
        locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid), with(ARRIVAL, true)))

        assertNull(locationDao.getGeofencesByPlace(place.uid))
    }

    override fun inject(component: TestComponent) = component.inject(this)
}

