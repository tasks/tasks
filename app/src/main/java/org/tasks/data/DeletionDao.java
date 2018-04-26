package org.tasks.data;

import static com.google.common.collect.Iterables.partition;
import static com.todoroo.andlib.utility.DateUtilities.now;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class DeletionDao {
  @Query("SELECT _id FROM tasks WHERE deleted > 0")
  public abstract List<Long> getDeleted();

  @Query("DELETE FROM caldav_tasks WHERE task IN(:ids)")
  abstract void deleteCaldavTasks(List<Long> ids);

  @Query("DELETE FROM google_tasks WHERE task IN(:ids)")
  abstract void deleteGoogleTasks(List<Long> ids);

  @Query("DELETE FROM tags WHERE task IN(:ids)")
  abstract void deleteTags(List<Long> ids);

  @Query("DELETE FROM locations WHERE task IN(:ids)")
  abstract void deleteGeofences(List<Long> ids);

  @Query("DELETE FROM alarms WHERE task IN(:ids)")
  abstract void deleteAlarms(List<Long> ids);

  @Query("DELETE FROM tasks WHERE _id IN(:ids)")
  abstract void deleteTasks(List<Long> ids);

  @Transaction
  public void delete(List<Long> ids) {
    for (List<Long> partition : partition(ids, 999)) {
      deleteAlarms(partition);
      deleteGeofences(partition);
      deleteTags(partition);
      deleteGoogleTasks(partition);
      deleteCaldavTasks(partition);
      deleteTasks(partition);
    }
  }

  @Query("UPDATE tasks SET modified = :timestamp, deleted = :timestamp WHERE _id IN(:ids)")
  abstract void markDeleted(long timestamp, List<Long> ids);

  public void markDeleted(List<Long> ids) {
    long now = now();
    for (List<Long> partition : partition(ids, 997)) {
      markDeleted(now, partition);
    }
  }

  @Query("SELECT task FROM google_tasks WHERE deleted = 0 AND list_id = :listId")
  abstract List<Long> getActiveGoogleTasks(String listId);

  @Delete
  abstract void deleteGoogleTaskList(GoogleTaskList googleTaskList);

  @Transaction
  public List<Long> delete(GoogleTaskList googleTaskList) {
    List<Long> tasks = getActiveGoogleTasks(googleTaskList.getRemoteId());
    delete(tasks);
    deleteGoogleTaskList(googleTaskList);
    return tasks;
  }

  @Delete
  abstract void deleteGoogleTaskAccount(GoogleTaskAccount googleTaskAccount);

  @Query("SELECT * FROM google_task_lists WHERE account = :account ORDER BY title ASC")
  abstract List<GoogleTaskList> getLists(String account);

  @Transaction
  public List<Long> delete(GoogleTaskAccount googleTaskAccount) {
    List<Long> deleted = new ArrayList<>();
    for (GoogleTaskList list : getLists(googleTaskAccount.getAccount())) {
      deleted.addAll(delete(list));
    }
    deleteGoogleTaskAccount(googleTaskAccount);
    return deleted;
  }

  @Query("SELECT task FROM caldav_tasks WHERE calendar = :calendar AND deleted = 0")
  abstract List<Long> getActiveCaldavTasks(String calendar);

  @Delete
  abstract void deleteCaldavCalendar(CaldavCalendar caldavCalendar);

  @Transaction
  public List<Long> delete(CaldavCalendar caldavCalendar) {
    List<Long> tasks = getActiveCaldavTasks(caldavCalendar.getUuid());
    delete(tasks);
    deleteCaldavCalendar(caldavCalendar);
    return tasks;
  }

  @Query("SELECT * FROM caldav_calendar WHERE account = :account")
  abstract List<CaldavCalendar> getCalendars(String account);

  @Delete
  abstract void deleteCaldavAccount(CaldavAccount caldavAccount);

  @Transaction
  public List<Long> delete(CaldavAccount caldavAccount) {
    List<Long> deleted = new ArrayList<>();
    for (CaldavCalendar calendar : getCalendars(caldavAccount.getUuid())) {
      deleted.addAll(delete(calendar));
    }
    deleteCaldavAccount(caldavAccount);
    return deleted;
  }
}
