package org.tasks.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Single;
import java.util.List;

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

  @Query("SELECT * FROM caldav_lists ORDER BY cdl_name COLLATE NOCASE")
  List<CaldavCalendar> getCalendars();

  @Query("SELECT * FROM caldav_lists WHERE cdl_account = :account ORDER BY cdl_name COLLATE NOCASE")
  List<CaldavCalendar> getCalendarsByAccount(String account);

  @Query("SELECT * FROM caldav_lists WHERE cdl_uuid = :uuid LIMIT 1")
  CaldavCalendar getCalendar(String uuid);

  @Query(
      "SELECT * FROM caldav_lists WHERE cdl_account = :account AND cdl_name = :name COLLATE NOCASE LIMIT 1")
  CaldavCalendar getCalendar(String account, String name);

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
}
