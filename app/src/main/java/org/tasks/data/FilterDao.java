package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import java.util.List;

@Dao
public interface FilterDao {

  @Update
  void update(Filter filter);

  @Query("DELETE FROM filters WHERE _id = :id")
  void delete(long id);

  @Query("SELECT * FROM filters WHERE title = :title COLLATE NOCASE LIMIT 1")
  Filter getByName(String title);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insertOrUpdate(Filter storeObject);

  @Insert
  void insert(Filter filter);

  @Query("SELECT * FROM filters ORDER BY title ASC")
  List<Filter> getFilters();

  @Query("SELECT * FROM filters WHERE _id = :id LIMIT 1")
  Filter getById(long id);

  @Query("SELECT * FROM filters")
  List<Filter> getAll();
}
