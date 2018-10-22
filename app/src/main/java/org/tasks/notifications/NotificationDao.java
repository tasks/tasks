package org.tasks.notifications;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface NotificationDao {

  @Query("SELECT * FROM notification")
  List<Notification> getAll();

  @Query("SELECT * FROM notification ORDER BY timestamp DESC")
  List<Notification> getAllOrdered();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<Notification> notifications);

  @Query("DELETE FROM notification WHERE task = :taskId")
  int delete(long taskId);

  @Query("DELETE FROM notification WHERE task IN(:taskIds)")
  int deleteAll(List<Long> taskIds);

  @Query("SELECT MAX(timestamp) FROM notification")
  long latestTimestamp();
}
