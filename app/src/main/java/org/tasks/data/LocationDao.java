package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface LocationDao {

    @Query("SELECT * FROM locations WHERE _id = :id LIMIT 1")
    Location getGeofence(Long id);

    @Query("SELECT * FROM locations WHERE task = :taskId ORDER BY name ASC")
    List<Location> getGeofences(long taskId);

    @Query("SELECT locations.* FROM locations LEFT JOIN tasks ON tasks._id = locations.task WHERE locations.task = :taskId AND tasks.deleted = 0 AND tasks.completed = 0")
    List<Location> getActiveGeofences(long taskId);

    @Query("SELECT locations.* FROM locations LEFT JOIN tasks ON tasks._id = locations.task WHERE tasks.deleted = 0 AND tasks.completed = 0")
    List<Location> getActiveGeofences();

    @Delete
    void delete(Location location);

    @Insert
    void insert(Location location);

    @Query("DELETE FROM locations WHERE task = :taskId")
    void deleteByTaskId(long taskId);
}
