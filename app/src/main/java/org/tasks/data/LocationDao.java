package org.tasks.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Single;
import java.util.List;

@Dao
public interface LocationDao {

  @Query(
      "SELECT * FROM geofences INNER JOIN places ON geofences.place = places.uid WHERE geofence_id = :id LIMIT 1")
  Location getGeofence(Long id);

  @Query(
      "SELECT * FROM geofences INNER JOIN places ON geofences.place = places.uid WHERE task = :taskId ORDER BY name ASC LIMIT 1")
  Location getGeofences(long taskId);

  @Query(
      "SELECT geofences.*, places.* FROM geofences INNER JOIN places ON geofences.place = places.uid INNER JOIN tasks ON tasks._id = geofences.task WHERE tasks._id = :taskId AND tasks.deleted = 0 AND tasks.completed = 0")
  List<Location> getActiveGeofences(long taskId);

  @Query(
      "SELECT geofences.*, places.* FROM geofences INNER JOIN places ON geofences.place = places.uid INNER JOIN tasks ON tasks._id = geofences.task WHERE tasks.deleted = 0 AND tasks.completed = 0")
  List<Location> getActiveGeofences();

  @Query("SELECT COUNT(*) FROM geofences")
  Single<Integer> geofenceCount();

  @Delete
  void delete(Geofence location);

  @Delete
  void delete(Place place);

  @Insert
  long insert(Geofence location);

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insert(Place place);

  @Update
  void update(Place place);

  @Query("SELECT * FROM places WHERE uid = :uid LIMIT 1")
  Place getByUid(String uid);

  @Query("SELECT * FROM geofences WHERE task = :taskId")
  List<Geofence> getGeofencesForTask(long taskId);

  @Query("SELECT * FROM places")
  List<Place> getPlaces();

  @Query(
      "SELECT places.*, IFNULL(COUNT(geofence_id),0) AS count FROM places LEFT OUTER JOIN geofences ON geofences.place = places.uid GROUP BY uid ORDER BY COUNT(geofence_id) DESC")
  LiveData<List<PlaceUsage>> getPlaceUsage();

  @Query("SELECT * FROM places WHERE latitude = :latitude AND longitude = :longitude LIMIT 1")
  Place findPlace(double latitude, double longitude);
}
