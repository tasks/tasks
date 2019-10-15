package org.tasks.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Single;
import java.util.List;
import org.tasks.filters.CaldavFilters;

@Dao
public interface CaldavDao {

  @Query("SELECT * FROM caldav_lists")
  LiveData<List<CaldavCalendar>> subscribeToCalendars();

  @Query("SELECT * FROM caldav_lists WHERE cdl_uuid = :uuid LIMIT 1")
  CaldavCalendar getCalendarByUuid(String uuid);

  @Query("SELECT * FROM caldav_accounts WHERE cda_uuid = :uuid LIMIT 1")
  CaldavAccount getAccountByUuid(String uuid);

  @Query("SELECT COUNT(*) FROM caldav_accounts")
  Single<Integer> accountCount();

  @Query("SELECT * FROM caldav_accounts ORDER BY UPPER(cda_name) ASC")
  List<CaldavAccount> getAccounts();

  @Insert
  long insert(CaldavAccount caldavAccount);

  @Update
  void update(CaldavAccount caldavAccount);

  @Insert
  long insert(CaldavCalendar caldavCalendar);

  @Update
  void update(CaldavCalendar caldavCalendar);

  @Insert
  long insert(CaldavTask caldavTask);

  @Insert
  void insert(Iterable<CaldavTask> tasks);

  @Update
  void update(CaldavTask caldavTask);

  @Delete
  void delete(CaldavTask caldavTask);

  @Query("SELECT * FROM caldav_tasks WHERE cd_deleted > 0 AND cd_calendar = :calendar")
  List<CaldavTask> getDeleted(String calendar);

  @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId AND cd_deleted = 0 LIMIT 1")
  CaldavTask getTask(long taskId);

  @Query("SELECT * FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_object = :object LIMIT 1")
  CaldavTask getTask(String calendar, String object);

  @Query("SELECT * FROM caldav_tasks WHERE cd_task = :taskId")
  List<CaldavTask> getTasks(long taskId);

  @Query(
      "SELECT task.*, caldav_task.* FROM tasks AS task "
          + "INNER JOIN caldav_tasks AS caldav_task ON _id = cd_task "
          + "WHERE cd_deleted = 0 AND cd_vtodo IS NOT NULL AND cd_vtodo != ''")
  List<CaldavTaskContainer> getTasks();

  @Query("SELECT * FROM caldav_lists ORDER BY cdl_name COLLATE NOCASE")
  List<CaldavCalendar> getCalendars();

  @Query("SELECT * FROM caldav_lists WHERE cdl_uuid = :uuid LIMIT 1")
  CaldavCalendar getCalendar(String uuid);

  @Query("SELECT cd_object FROM caldav_tasks WHERE cd_calendar = :calendar")
  List<String> getObjects(String calendar);

  @Query("SELECT cd_task FROM caldav_tasks WHERE cd_calendar = :calendar AND cd_object IN (:objects)")
  List<Long> getTasks(String calendar, List<String> objects);

  @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account AND cdl_url NOT IN (:urls)")
  List<CaldavCalendar> findDeletedCalendars(String account, List<String> urls);

  @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account AND cdl_url = :url LIMIT 1")
  CaldavCalendar getCalendarByUrl(String account, String url);

  @Query("SELECT * FROM caldav_accounts WHERE cda_name = :name COLLATE NOCASE LIMIT 1")
  CaldavAccount getAccountByName(String name);

  @Query("SELECT DISTINCT cd_calendar FROM caldav_tasks WHERE cd_deleted = 0 AND cd_task IN (:tasks)")
  List<String> getCalendars(List<Long> tasks);

  @Query(
      "SELECT caldav_lists.*, caldav_accounts.*, COUNT(tasks._id) AS count"
          + " FROM caldav_accounts"
          + " LEFT JOIN caldav_lists ON caldav_lists.cdl_account = caldav_accounts.cda_uuid"
          + " LEFT JOIN caldav_tasks ON caldav_tasks.cd_calendar = caldav_lists.cdl_uuid"
          + " LEFT JOIN tasks ON caldav_tasks.cd_task = tasks._id AND tasks.deleted = 0 AND tasks.completed = 0 AND tasks.hideUntil < :now"
          + " GROUP BY caldav_lists.cdl_uuid"
          + " ORDER BY caldav_accounts.cda_name COLLATE NOCASE, caldav_lists.cdl_name COLLATE NOCASE")
  List<CaldavFilters> getCaldavFilters(long now);

  @Query(
      "SELECT tasks._id FROM tasks "
          + "INNER JOIN tags ON tags.task = tasks._id "
          + "INNER JOIN caldav_tasks ON cd_task = tasks._id "
          + "GROUP BY tasks._id")
  List<Long> getTasksWithTags();

  @Query("WITH RECURSIVE "
          + " recursive_caldav (cd_task) AS ( "
          + " SELECT cd_task "
          + " FROM tasks "
          + " INNER JOIN caldav_tasks "
          + "  ON _id = cd_task "
          + " WHERE cd_parent IN (:ids) "
          + " AND tasks.deleted = 0 "
          + "UNION ALL "
          + " SELECT caldav_tasks.cd_task "
          + " FROM tasks "
          + " INNER JOIN caldav_tasks "
          + "  ON _id = caldav_tasks.cd_task "
          + " INNER JOIN recursive_caldav "
          + "  ON recursive_caldav.cd_task = caldav_tasks.cd_parent "
          + " WHERE tasks.deleted = 0 "
          + " ) "
          + "SELECT cd_task FROM recursive_caldav")
  List<Long> getChildren(List<Long> ids);
}
