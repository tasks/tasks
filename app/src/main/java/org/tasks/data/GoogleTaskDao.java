package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import java.util.List;

@Dao
public interface GoogleTaskDao {

  @Insert
  void insert(GoogleTask task);

  @Query("SELECT * FROM google_tasks WHERE task = :taskId AND deleted = 0 LIMIT 1")
  GoogleTask getByTaskId(long taskId);

  @Update
  void update(GoogleTask googleTask);

  @Query(
      "SELECT * FROM google_tasks WHERE list_id = :listId AND parent = :parent ORDER BY remote_order ASC")
  List<GoogleTask> byRemoteOrder(String listId, long parent);

  @Query(
      "SELECT * FROM google_tasks WHERE list_id = :listId AND `order` > :startAtOrder - 1 ORDER BY `order` ASC ")
  List<GoogleTask> getTasksFrom(String listId, long startAtOrder);

  @Query(
      "SELECT * FROM google_tasks WHERE list_id = :listId AND `order` < :startAtOrder ORDER BY `order` DESC")
  List<GoogleTask> getTasksFromReverse(String listId, long startAtOrder);

  @Delete
  void delete(GoogleTask deleted);

  @Query("SELECT * FROM google_tasks WHERE remote_id = :remoteId LIMIT 1")
  GoogleTask getByRemoteId(String remoteId);

  @Query("SELECT * FROM google_tasks WHERE task = :taskId AND deleted > 0")
  List<GoogleTask> getDeletedByTaskId(long taskId);

  @Query("SELECT * FROM google_tasks WHERE task = :taskId")
  List<GoogleTask> getAllByTaskId(long taskId);

  @Query("SELECT DISTINCT list_id FROM google_tasks WHERE deleted = 0 AND task IN (:tasks)")
  List<String> getLists(List<Long> tasks);

  @Query("SELECT task FROM google_tasks WHERE deleted = 0 AND list_id = :listId")
  List<Long> getActiveTasks(String listId);
}
