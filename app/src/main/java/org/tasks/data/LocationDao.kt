package org.tasks.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.todoroo.astrid.api.FilterListItem.NO_ORDER
import io.reactivex.Single
import org.tasks.filters.LocationFilters
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
    fun getPlacesWithGeofences(): List<Place>

    @Query("SELECT places.*,"
            + " max(geofences.arrival) as arrival,"
            + " max(geofences.departure) as departure,"
            + " min(geofences.radius) as radius"
            + " FROM places"
            + " INNER JOIN geofences ON geofences.place = places.uid"
            + " INNER JOIN tasks ON tasks._id = geofences.task"
            + " WHERE place = :uid AND tasks.completed = 0 AND tasks.deleted = 0"
            + " AND (geofences.arrival > 0 OR geofences.departure > 0)"
            + " GROUP BY places.uid")
    fun getGeofencesByPlace(uid: String): MergedGeofence?

    @Query("DELETE FROM geofences WHERE place = :place")
    fun deleteGeofencesByPlace(place: String)

    @Query("SELECT geofences.* FROM geofences"
            + " INNER JOIN tasks ON tasks._id = geofences.task"
            + " WHERE place = :place AND arrival = 1 AND tasks.completed = 0"
            + " AND tasks.deleted = 0 AND tasks.snoozeTime < :now AND tasks.hideUntil < :now")
    fun getArrivalGeofences(place: String, now: Long): List<Geofence>

    @Query("SELECT geofences.* FROM geofences"
            + " INNER JOIN tasks ON tasks._id = geofences.task"
            + " WHERE place = :place AND departure = 1 AND tasks.completed = 0"
            + " AND tasks.deleted = 0 AND tasks.snoozeTime < :now AND tasks.hideUntil < :now")
    fun getDepartureGeofences(place: String, now: Long): List<Geofence>

    @Query("SELECT * FROM geofences"
            + " INNER JOIN places ON geofences.place = places.uid"
            + " WHERE task = :taskId ORDER BY name ASC LIMIT 1")
    fun getGeofences(taskId: Long): Location?

    @Query("SELECT geofences.*, places.* FROM geofences INNER JOIN places ON geofences.place = places.uid INNER JOIN tasks ON tasks._id = geofences.task WHERE tasks._id = :taskId AND tasks.deleted = 0 AND tasks.completed = 0")
    fun getActiveGeofences(taskId: Long): List<Location>

    @Query("SELECT places.*"
            + " FROM places"
            + " INNER JOIN geofences ON geofences.place = places.uid"
            + " WHERE geofences.task = :taskId")
    fun getPlaceForTask(taskId: Long): Place?

    @Query("SELECT geofences.*, places.* FROM geofences INNER JOIN places ON geofences.place = places.uid INNER JOIN tasks ON tasks._id = geofences.task WHERE tasks.deleted = 0 AND tasks.completed = 0")
    fun getActiveGeofences(): List<Location>

    @Query("SELECT COUNT(*) FROM geofences")
    fun geofenceCount(): Single<Int>

    @Delete
    fun delete(location: Geofence)

    @Delete
    fun delete(place: Place)

    @Insert
    fun insert(location: Geofence): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(place: Place): Long

    @Update
    fun update(place: Place)

    @Update
    fun update(geofence: Geofence)

    @Query("SELECT * FROM places WHERE uid = :uid LIMIT 1")
    fun getByUid(uid: String): Place?

    @Query("SELECT * FROM geofences WHERE task = :taskId")
    fun getGeofencesForTask(taskId: Long): List<Geofence>

    @Query("SELECT * FROM places")
    fun getPlaces(): List<Place>

    @Query("SELECT * FROM places WHERE place_id = :id")
    fun getPlace(id: Long): Place?

    @Query("SELECT * FROM places WHERE uid = :uid")
    fun getPlace(uid: String): Place?

    @Query("SELECT places.*, IFNULL(COUNT(geofence_id),0) AS count FROM places LEFT OUTER JOIN geofences ON geofences.place = places.uid GROUP BY uid ORDER BY COUNT(geofence_id) DESC")
    fun getPlaceUsage(): LiveData<List<PlaceUsage>>

    @Query("SELECT * FROM places WHERE latitude LIKE :latitude AND longitude LIKE :longitude")
    fun findPlace(latitude: String, longitude: String): Place?

    @Query("SELECT places.*, COUNT(tasks._id) AS count FROM places "
            + " LEFT JOIN geofences ON geofences.place = places.uid "
            + " LEFT JOIN tasks ON geofences.task = tasks._id AND tasks.completed = 0 AND tasks.deleted = 0 AND tasks.hideUntil < :now"
            + " GROUP BY places.uid"
            + " ORDER BY name COLLATE NOCASE ASC")
    fun getPlaceFilters(now: Long = currentTimeMillis()): List<LocationFilters>

    @Query("UPDATE places SET place_order = $NO_ORDER")
    fun resetOrders()

    @Query("UPDATE places SET place_order = :order WHERE place_id = :id")
    fun setOrder(id: Long, order: Int)
}