package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.SuspendFreeze.Companion.freezeAt
import org.tasks.caldav.GeoUtils.toLikeString
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.DELETION_TIME
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.HIDE_TYPE
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class LocationDaoTest : InjectingTestCase() {
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var alarmDao: AlarmDao

    @Test
    fun getExistingPlace() = runBlocking {
        locationDao.insert(Place(latitude = 48.067222, longitude = 12.863611))
        val place = locationDao.findPlace(48.067222.toLikeString(), 12.863611.toLikeString())
        assertEquals(48.067222, place?.latitude)
        assertEquals(12.863611, place?.longitude)
    }

    @Test
    fun getPlaceWithLessPrecision() = runBlocking {
        locationDao.insert(Place(latitude = 50.7547, longitude = -2.2279))
        val place = locationDao.findPlace(50.754712.toLikeString(), (-2.227945).toLikeString())
        assertEquals(50.7547, place?.latitude)
        assertEquals(-2.2279, place?.longitude)
    }

    @Test
    fun getPlaceWithMorePrecision() = runBlocking {
        locationDao.insert(Place(latitude = 36.246944, longitude = -116.816944))
        locationDao.getPlaces().forEach { println(it) }
        val place = locationDao.findPlace(36.2469.toLikeString(), (-116.8169).toLikeString())
        assertEquals(36.246944, place?.latitude)
        assertEquals(-116.816944, place?.longitude)
    }

    @Test
    fun noActiveGeofences() = runBlocking {
        val place = Place()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1)))
        locationDao.insert(Geofence(task = 1, place = place.uid))

        assertNull(locationDao.getGeofencesByPlace(place.uid!!))
    }

    @Test
    fun activeArrivalGeofence() = runBlocking {
        val place = Place()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1)))
        locationDao.insert(Geofence(task = 1, place = place.uid, isArrival = true))

        val geofence = locationDao.getGeofencesByPlace(place.uid!!)

        assertTrue(geofence!!.arrival)
        assertFalse(geofence.departure)
    }

    @Test
    fun activeDepartureGeofence() = runBlocking {
        val place = Place()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1)))
        locationDao.insert(Geofence(task = 1, place = place.uid, isDeparture = true))

        val geofence = locationDao.getGeofencesByPlace(place.uid!!)

        assertFalse(geofence!!.arrival)
        assertTrue(geofence.departure)
    }

    @Test
    fun geofenceInactiveForCompletedTask() = runBlocking {
        val place = Place()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1), with(COMPLETION_TIME, newDateTime())))
        locationDao.insert(Geofence(task = 1, place = place.uid, isArrival = true))

        assertNull(locationDao.getGeofencesByPlace(place.uid!!))
    }

    @Test
    fun geofenceInactiveForDeletedTask() = runBlocking {
        val place = Place()
        locationDao.insert(place)
        taskDao.createNew(newTask(with(ID, 1), with(DELETION_TIME, newDateTime())))
        locationDao.insert(Geofence(task = 1, place = place.uid, isArrival = true))

        assertNull(locationDao.getGeofencesByPlace(place.uid!!))
    }

    @Test
    fun ignoreArrivalForSnoozedTask() = runBlocking {
        freezeAt(currentTimeMillis()).thawAfter {
            val place = Place()
            locationDao.insert(place)
            val task = taskDao.createNew(newTask())
            alarmDao.insert(
                Alarm(
                    task = task,
                    time = newDateTime().plusMinutes(15).millis,
                    type = TYPE_SNOOZE
                )
            )
            locationDao.insert(Geofence(task = task, place = place.uid, isArrival = true))

            assertTrue(locationDao.getArrivalGeofences(place.uid!!, currentTimeMillis()).isEmpty())
        }
    }

    @Test
    fun ignoreDepartureForSnoozedTask() = runBlocking {
        freezeAt(currentTimeMillis()).thawAfter {
            val place = Place()
            locationDao.insert(place)
            val task = taskDao.createNew(newTask())
            alarmDao.insert(
                Alarm(
                    task = task,
                    time = newDateTime().plusMinutes(15).millis,
                    type = TYPE_SNOOZE
                )
            )
            locationDao.insert(Geofence(task = task, place = place.uid, isDeparture = true))

            assertTrue(locationDao.getDepartureGeofences(place.uid!!, currentTimeMillis()).isEmpty())
        }
    }

    @Test
    fun getArrivalWithElapsedSnooze() = runBlocking {
        freezeAt(currentTimeMillis()).thawAfter {
            val place = Place()
            locationDao.insert(place)
            val task = taskDao.createNew(newTask())
            alarmDao.insert(
                Alarm(
                    task = task,
                    time = newDateTime().minusMinutes(15).millis,
                    type = TYPE_SNOOZE
                )
            )
            val geofence = Geofence(task = task, place = place.uid, isArrival = true)
                .let { it.copy(id = locationDao.insert(it)) }

            assertEquals(listOf(geofence), locationDao.getArrivalGeofences(place.uid!!,
                currentTimeMillis()
            ))
        }
    }

    @Test
    fun getDepartureWithElapsedSnooze() = runBlocking {
        freezeAt(currentTimeMillis()).thawAfter {
            val place = Place()
            locationDao.insert(place)
            val task = taskDao.createNew(newTask())
            alarmDao.insert(
                Alarm(
                    task = task,
                    time = newDateTime().minusMinutes(15).millis,
                    type = TYPE_SNOOZE
                )
            )
            val geofence = Geofence(task = task, place = place.uid, isDeparture = true)
                .let { it.copy(id = locationDao.insert(it)) }

            assertEquals(listOf(geofence), locationDao.getDepartureGeofences(place.uid!!,
                currentTimeMillis()
            ))
        }
    }

    @Test
    fun ignoreArrivalForHiddenTask() = runBlocking {
        freezeAt(currentTimeMillis()).thawAfter {
            val place = Place()
            locationDao.insert(place)
            taskDao.createNew(newTask(
                    with(ID, 1),
                    with(DUE_TIME, newDateTime().plusMinutes(15)),
                    with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME)))
            locationDao.insert(Geofence(task = 1, place = place.uid, isArrival = true))

            assertTrue(locationDao.getArrivalGeofences(place.uid!!, currentTimeMillis()).isEmpty())
        }
    }

    @Test
    fun ignoreDepartureForHiddenTask() = runBlocking {
        freezeAt(currentTimeMillis()).thawAfter {
            val place = Place()
            locationDao.insert(place)
            taskDao.createNew(newTask(
                    with(ID, 1),
                    with(DUE_TIME, newDateTime().plusMinutes(15)),
                    with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME)))
            locationDao.insert(Geofence(task = 1, place = place.uid, isDeparture = true))

            assertTrue(locationDao.getDepartureGeofences(place.uid!!, currentTimeMillis()).isEmpty())
        }
    }

    @Test
    fun getArrivalWithElapsedHideUntil() = runBlocking {
        freezeAt(currentTimeMillis()).thawAfter {
            val place = Place()
            locationDao.insert(place)
            taskDao.createNew(newTask(
                    with(ID, 1),
                    with(DUE_TIME, newDateTime().minusMinutes(15)),
                    with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME)))
            val geofence = Geofence(task = 1, place = place.uid, isArrival = true)
                .let {
                    it.copy(id = locationDao.insert(it))
                }

            assertEquals(listOf(geofence), locationDao.getArrivalGeofences(place.uid!!,
                currentTimeMillis()
            ))
        }
    }

    @Test
    fun getDepartureWithElapsedHideUntil() = runBlocking {
        freezeAt(currentTimeMillis()).thawAfter {
            val place = Place()
            locationDao.insert(place)
            taskDao.createNew(newTask(
                    with(ID, 1),
                    with(DUE_TIME, newDateTime().minusMinutes(15)),
                    with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME)))
            val geofence = Geofence(task = 1, place = place.uid, isDeparture = true)
                .let { it.copy(id = locationDao.insert(it)) }

            assertEquals(listOf(geofence), locationDao.getDepartureGeofences(place.uid!!,
                currentTimeMillis()
            ))
        }
    }
}

