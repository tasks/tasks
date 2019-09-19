package org.tasks.data;

import static org.tasks.db.DbUtils.batch;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class DeletionDao {

  @Query("SELECT _id FROM tasks WHERE deleted > 0")
  public abstract List<Long> getDeleted();

  @Query("DELETE FROM caldav_tasks WHERE cd_task IN(:ids)")
  abstract void deleteCaldavTasks(List<Long> ids);

  @Query("DELETE FROM google_tasks WHERE gt_task IN(:ids)")
  abstract void deleteGoogleTasks(List<Long> ids);

  @Query("DELETE FROM tags WHERE task IN(:ids)")
  abstract void deleteTags(List<Long> ids);

  @Query("DELETE FROM geofences WHERE task IN(:ids)")
  abstract void deleteGeofences(List<Long> ids);

  @Query("DELETE FROM alarms WHERE task IN(:ids)")
  abstract void deleteAlarms(List<Long> ids);

  @Query("DELETE FROM tasks WHERE _id IN(:ids)")
  abstract void deleteTasks(List<Long> ids);

  @Transaction
  public void delete(List<Long> ids) {
    batch(ids, b -> {
      deleteAlarms(b);
      deleteGeofences(b);
      deleteTags(b);
      deleteGoogleTasks(b);
      deleteCaldavTasks(b);
      deleteTasks(b);
    });
  }

  @Query("UPDATE tasks "
      + "SET modified = datetime('now', 'localtime'), deleted = datetime('now', 'localtime') "
      + "WHERE _id IN(:ids)")
  abstract void markDeletedInternal(List<Long> ids);

  public void markDeleted(Iterable<Long> ids) {
    batch(ids, this::markDeletedInternal);
  }

  @Query("SELECT gt_task FROM google_tasks WHERE gt_deleted = 0 AND gt_list_id = :listId")
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

  @Query("SELECT * FROM google_task_lists WHERE gtl_account = :account ORDER BY gtl_title ASC")
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

  @Query("SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_deleted = 0")
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

  @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account")
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
