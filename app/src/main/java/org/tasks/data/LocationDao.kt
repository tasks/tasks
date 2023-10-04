package org.tasks.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.api.Filter.Companion.NO_ORDER
import com.todoroo.astrid.data.Task
import org.tasks.data.Alarm.Companion.TYPE_SNOOZE
import org.tasks.filters.LocationFilters
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils.currentTimeMillis

@Dao
interface LocationDao {
    @Query("SELECT places.*"
            + " FROM places"
            + " INNER JOIN geofences ON geofences.place = places.uid"
            + " INNER JOIN tasks ON geofences.task = tasks._id"
            + " WHERE tasks.completed = 0 AND tasks.deleted = 0"
            + " AND (geofences.arrival > 0 OR geofences.departure > 0)"
            + " GROUP BY places.uid")
    suspend fun getPlacesWithGeofences(): List<Place>

    @Query("SELECT places.*,"
            + " max(geofences.arrival) as arrival,"
            + " max(geofences.departure) as departure"
            + " FROM places"
            + " INNER JOIN geofences ON geofences.place = places.uid"
            + " INNER JOIN tasks ON tasks._id = geofences.task"
            + " WHERE place = :uid AND tasks.completed = 0 AND tasks.deleted = 0"
            + " AND (geofences.arrival > 0 OR geofences.departure > 0)"
            + " GROUP BY places.uid")
    suspend fun getGeofencesByPlace(uid: String): MergedGeofence?

    @Query("DELETE FROM geofences WHERE place = :place")
    suspend fun deleteGeofencesByPlace(place: String)

    @Query("SELECT geofences.* FROM geofences"
            + " INNER JOIN tasks ON tasks._id = geofences.task"
            + " LEFT JOIN alarms ON tasks._id = alarms.task AND alarms.type == $TYPE_SNOOZE"
            + " WHERE place = :place AND arrival = 1 AND tasks.completed = 0"
            + " AND tasks.deleted = 0 AND (alarms._id IS NULL OR alarms.time < :now) AND tasks.hideUntil < :now")
    suspend fun getArrivalGeofences(place: String, now: Long = now()): List<Geofence>

    @Query("SELECT geofences.* FROM geofences"
            + " INNER JOIN tasks ON tasks._id = geofences.task"
            + " LEFT JOIN alarms ON tasks._id = alarms.task AND alarms.type == $TYPE_SNOOZE"
            + " WHERE place = :place AND departure = 1 AND tasks.completed = 0"
            + " AND tasks.deleted = 0 AND (alarms._id IS NULL OR alarms.time < :now) AND tasks.hideUntil < :now")
    suspend fun getDepartureGeofences(place: String, now: Long = now()): List<Geofence>

    @Query("SELECT * FROM geofences"
            + " INNER JOIN places ON geofences.place = places.uid"
            + " WHERE task = :taskId ORDER BY name ASC LIMIT 1")
    suspend fun getGeofences(taskId: Long): Location?

    @Query("SELECT geofences.*, places.* FROM geofences INNER JOIN places ON geofences.place = places.uid INNER JOIN tasks ON tasks._id = geofences.task WHERE tasks._id = :taskId AND tasks.deleted = 0 AND tasks.completed = 0")
    suspend fun getActiveGeofences(taskId: Long): List<Location>

    @Query("SELECT places.*"
            + " FROM places"
            + " INNER JOIN geofences ON geofences.place = places.uid"
            + " WHERE geofences.task = :taskId")
    suspend fun getPlaceForTask(taskId: Long): Place?

    @Query("SELECT geofences.*, places.* FROM geofences INNER JOIN places ON geofences.place = places.uid INNER JOIN tasks ON tasks._id = geofences.task WHERE tasks.deleted = 0 AND tasks.completed = 0")
    suspend fun getActiveGeofences(): List<Location>

    @Query("SELECT COUNT(*) FROM geofences")
    suspend fun geofenceCount(): Int

    @Delete
    suspend fun delete(location: Geofence)

    @Delete
    suspend fun delete(place: Place)

    @Insert
    suspend fun insert(location: Geofence): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(place: Place): Long

    @Update
    suspend fun update(place: Place)

    @Update
    suspend fun update(geofence: Geofence)

    @Query("SELECT * FROM places WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): Place?

    @Query("SELECT * FROM geofences WHERE task = :taskId")
    suspend fun getGeofencesForTask(taskId: Long): List<Geofence>

    @Query("SELECT * FROM places")
    suspend fun getPlaces(): List<Place>

    @Query("SELECT * FROM places WHERE place_id = :id")
    suspend fun getPlace(id: Long): Place?

    @Query("SELECT * FROM places WHERE uid = :uid")
    suspend fun getPlace(uid: String): Place?

    @Query("SELECT places.*, IFNULL(COUNT(geofence_id),0) AS count FROM places LEFT OUTER JOIN geofences ON geofences.place = places.uid GROUP BY uid ORDER BY COUNT(geofence_id) DESC")
    fun getPlaceUsage(): LiveData<List<PlaceUsage>>

    @Query("SELECT * FROM places WHERE latitude LIKE :latitude AND longitude LIKE :longitude")
    suspend fun findPlace(latitude: String, longitude: String): Place?

    @Query("SELECT places.*, COUNT(tasks._id) AS count FROM places "
            + " LEFT JOIN geofences ON geofences.place = places.uid "
            + " LEFT JOIN tasks ON geofences.task = tasks._id AND tasks.completed = 0 AND tasks.deleted = 0 AND tasks.hideUntil < :now"
            + " GROUP BY places.uid"
            + " ORDER BY name COLLATE NOCASE ASC")
    suspend fun getPlaceFilters(now: Long = currentTimeMillis()): List<LocationFilters>

    @Query("UPDATE places SET place_order = $NO_ORDER")
    suspend fun resetOrders()

    @Query("UPDATE places SET place_order = :order WHERE place_id = :id")
    suspend fun setOrder(id: Long, order: Int)

    suspend fun getLocation(task: Task, preferences: Preferences): Location? {
        if (task.isNew) {
            if (task.hasTransitory(Place.KEY)) {
                getPlace(task.getTransitory<String>(Place.KEY)!!)?.let {
                    return Location(Geofence(it.uid, preferences), it)
                }
            }
        } else {
            return getGeofences(task.id)
        }
        return null
    }
}