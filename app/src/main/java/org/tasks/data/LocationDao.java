package org.tasks.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import io.reactivex.Single;
import java.util.List;

@Dao
public interface LocationDao {

  @Query("SELECT * FROM locations WHERE _id = :id LIMIT 1")
  Location getGeofence(Long id);

  @Query("SELECT * FROM locations WHERE task = :taskId ORDER BY name ASC")
  List<Location> getGeofences(long taskId);

  @Query(
      "SELECT locations.* FROM locations INNER JOIN tasks ON tasks._id = locations.task WHERE tasks._id = :taskId AND tasks.deleted = 0 AND tasks.completed = 0")
  List<Location> getActiveGeofences(long taskId);

  @Query(
      "SELECT locations.* FROM locations INNER JOIN tasks ON tasks._id = locations.task WHERE tasks.deleted = 0 AND tasks.completed = 0")
  List<Location> getActiveGeofences();

  @Query("SELECT COUNT(*) FROM locations")
  Single<Integer> geofenceCount();

  @Delete
  void delete(Location location);

  @Insert
  void insert(Location location);
}
