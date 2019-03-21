package org.tasks.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Single;
import java.util.List;

@Dao
public interface LocationDao {

  @Query(
      "SELECT * FROM geofences INNER JOIN places ON geofences.place = places.uid WHERE geofence_id = :id LIMIT 1")
  Location getGeofence(Long id);

  @Query("SELECT * FROM geofences INNER JOIN places ON geofences.place = places.uid WHERE task = :taskId ORDER BY name ASC")
  List<Location> getGeofences(long taskId);

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

  @Insert
  void insert(Geofence location);

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insert(Place place);

  @Query("SELECT * FROM places WHERE uid = :uid LIMIT 1")
  Place getByUid(String uid);

  @Query("SELECT * FROM geofences WHERE task = :taskId")
  List<Geofence> getGeofencesForTask(long taskId);

  @Query("SELECT * FROM places")
  List<Place> getPlaces();
}
