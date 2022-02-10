package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.tasks.SuspendFreeze.Companion.freezeAt
import org.tasks.caldav.GeoUtils.toLikeString
import org.tasks.data.Alarm.Companion.TYPE_SNOOZE
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.GeofenceMaker.ARRIVAL
import org.tasks.makers.GeofenceMaker.DEPARTURE
import org.tasks.makers.GeofenceMaker.PLACE
import org.tasks.makers.GeofenceMaker.TASK
import org.tasks.makers.GeofenceMaker.newGeofence
import org.tasks.makers.PlaceMaker.LATITUDE
import org.tasks.makers.PlaceMaker.LONGITUDE
import org.tasks.makers.PlaceMaker.newPlace
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.DELETION_TIME
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.HIDE_TYPE
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class LocationDaoTest : InjectingTestCase() {
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var alarmDao: AlarmDao

    @Test
    fun getExistingPlace() = runBlocking {
        locationDao.insert(newPlace(with(LATITUDE, 48.067222), with(LONGITUDE, 12.863611)))
        val place = locationDao.findPlace(48.067222.toLikeString(), 12.863611.toLikeString())
        assertEquals(48.067222, place?.latitude)
        assertEquals(12.863611, place?.longitude)
    }

    @Test
    fun getPlaceWithLessPrecision() = runBlocking {
        locationDao.insert(newPlace(with(LATITUDE, 50.7547), with(LONGITUDE, -2.2279)))
        val place = locationDao.findPlace(50.754712.toLikeString(), (-2.227945).toLikeString())
        assertEquals(50.7547, place?.latitude)
        assertEquals(-2.2279, place?.longitude)
    }

    @Test
    fun getPlaceWithMorePrecision() = runBlocking {
        locationDao.insert(newPlace(with(LATITUDE, 36.246944), with(LONGITUDE, -116.816944)))
        locationDao.getPlaces().forEach { println(it) }
        val place = locationDao.findPlace(36.2469.toLikeString(), (-116.8169).toLikeString())
        assertEquals(36.246944, place?.latitude)
        assertEquals(-116.816944, place?.longitude)
    }

    @Test
    fun noActiveGeofences() = runBlocking {
        val place = newPlace()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1)))
        locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid)))

        assertNull(locationDao.getGeofencesByPlace(place.uid!!))
    }

    @Test
    fun activeArrivalGeofence() = runBlocking {
        val place = newPlace()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1)))
        locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid), with(ARRIVAL, true)))

        val geofence = locationDao.getGeofencesByPlace(place.uid!!)

        assertTrue(geofence!!.arrival)
        assertFalse(geofence.departure)
    }

    @Test
    fun activeDepartureGeofence() = runBlocking {
        val place = newPlace()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1)))
        locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid), with(DEPARTURE, true)))

        val geofence = locationDao.getGeofencesByPlace(place.uid!!)

        assertFalse(geofence!!.arrival)
        assertTrue(geofence.departure)
    }

    @Test
    fun geofenceInactiveForCompletedTask() = runBlocking {
        val place = newPlace()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1), with(COMPLETION_TIME, newDateTime())))
        locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid), with(ARRIVAL, true)))

        assertNull(locationDao.getGeofencesByPlace(place.uid!!))
    }

    @Test
    fun geofenceInactiveForDeletedTask() = runBlocking {
        val place = newPlace()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1), with(DELETION_TIME, newDateTime())))
        locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid), with(ARRIVAL, true)))

        assertNull(locationDao.getGeofencesByPlace(place.uid!!))
    }

    @Test
    fun ignoreArrivalForSnoozedTask() = runBlocking {
        freezeAt(now()).thawAfter {
            val place = newPlace()
            locationDao.insert(place)
            val task = taskDao.createNew(newTask())
            alarmDao.insert(Alarm(task, newDateTime().plusMinutes(15).millis, TYPE_SNOOZE))
            locationDao.insert(newGeofence(with(TASK, task), with(PLACE, place.uid), with(ARRIVAL, true)))

            assertTrue(locationDao.getArrivalGeofences(place.uid!!, now()).isEmpty())
        }
    }

    @Test
    fun ignoreDepartureForSnoozedTask() = runBlocking {
        freezeAt(now()).thawAfter {
            val place = newPlace()
            locationDao.insert(place)
            val task = taskDao.createNew(newTask())
            alarmDao.insert(Alarm(task, newDateTime().plusMinutes(15).millis, TYPE_SNOOZE))
            locationDao.insert(newGeofence(with(TASK, task), with(PLACE, place.uid), with(DEPARTURE, true)))

            assertTrue(locationDao.getDepartureGeofences(place.uid!!, now()).isEmpty())
        }
    }

    @Test
    fun getArrivalWithElapsedSnooze() = runBlocking {
        freezeAt(now()).thawAfter {
            val place = newPlace()
            locationDao.insert(place)
            val task = taskDao.createNew(newTask())
            alarmDao.insert(Alarm(task, newDateTime().minusMinutes(15).millis, TYPE_SNOOZE))
            val geofence = newGeofence(with(TASK, task), with(PLACE, place.uid), with(ARRIVAL, true))
            geofence.id = locationDao.insert(geofence)

            assertEquals(listOf(geofence), locationDao.getArrivalGeofences(place.uid!!, now()))
        }
    }

    @Test
    fun getDepartureWithElapsedSnooze() = runBlocking {
        freezeAt(now()).thawAfter {
            val place = newPlace()
            locationDao.insert(place)
            val task = taskDao.createNew(newTask())
            alarmDao.insert(Alarm(task, newDateTime().minusMinutes(15).millis, TYPE_SNOOZE))
            val geofence = newGeofence(with(TASK, task), with(PLACE, place.uid), with(DEPARTURE, true))
            geofence.id = locationDao.insert(geofence)

            assertEquals(listOf(geofence), locationDao.getDepartureGeofences(place.uid!!, now()))
        }
    }

    @Test
    fun ignoreArrivalForHiddenTask() = runBlocking {
        freezeAt(now()).thawAfter {
            val place = newPlace()
            locationDao.insert(place)
            taskDao.createNew(newTask(
                    with(ID, 1),
                    with(DUE_TIME, newDateTime().plusMinutes(15)),
                    with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME)))
            locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid), with(ARRIVAL, true)))

            assertTrue(locationDao.getArrivalGeofences(place.uid!!, now()).isEmpty())
        }
    }

    @Test
    fun ignoreDepartureForHiddenTask() = runBlocking {
        freezeAt(now()).thawAfter {
            val place = newPlace()
            locationDao.insert(place)
            taskDao.createNew(newTask(
                    with(ID, 1),
                    with(DUE_TIME, newDateTime().plusMinutes(15)),
                    with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME)))
            locationDao.insert(newGeofence(with(TASK, 1), with(PLACE, place.uid), with(DEPARTURE, true)))

            assertTrue(locationDao.getDepartureGeofences(place.uid!!, now()).isEmpty())
        }
    }

    @Test
    fun getArrivalWithElapsedHideUntil() = runBlocking {
        freezeAt(now()).thawAfter {
            val place = newPlace()
            locationDao.insert(place)
            taskDao.createNew(newTask(
                    with(ID, 1),
                    with(DUE_TIME, newDateTime().minusMinutes(15)),
                    with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME)))
            val geofence = newGeofence(with(TASK, 1), with(PLACE, place.uid), with(ARRIVAL, true))
            geofence.id = locationDao.insert(geofence)

            assertEquals(listOf(geofence), locationDao.getArrivalGeofences(place.uid!!, now()))
        }
    }

    @Test
    fun getDepartureWithElapsedHideUntil() = runBlocking {
        freezeAt(now()).thawAfter {
            val place = newPlace()
            locationDao.insert(place)
            taskDao.createNew(newTask(
                    with(ID, 1),
                    with(DUE_TIME, newDateTime().minusMinutes(15)),
                    with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME)))
            val geofence = newGeofence(with(TASK, 1), with(PLACE, place.uid), with(DEPARTURE, true))
            geofence.id = locationDao.insert(geofence)

            assertEquals(listOf(geofence), locationDao.getDepartureGeofences(place.uid!!, now()))
        }
    }
}

