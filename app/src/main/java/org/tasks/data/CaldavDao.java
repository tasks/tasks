package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import java.util.List;

@Dao
public interface CaldavDao {

  @Query("SELECT * FROM caldav_calendar WHERE uuid = :uuid LIMIT 1")
  CaldavCalendar getCalendarByUuid(String uuid);

  @Query("SELECT * FROM caldav_account WHERE uuid = :uuid LIMIT 1")
  CaldavAccount getAccountByUuid(String uuid);

  @Query("SELECT * FROM caldav_account ORDER BY UPPER(name) ASC")
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

  @Update
  void update(CaldavTask caldavTask);

  @Delete
  void delete(CaldavTask caldavTask);

  @Query("SELECT * FROM caldav_tasks WHERE task = :taskId AND deleted > 0 AND calendar = :calendar")
  List<CaldavTask> getDeleted(long taskId, String calendar);

  @Query("SELECT * FROM caldav_tasks WHERE task = :taskId AND deleted = 0 LIMIT 1")
  CaldavTask getTask(long taskId);

  @Query("SELECT * FROM caldav_tasks WHERE calendar = :calendar AND object = :object LIMIT 1")
  CaldavTask getTask(String calendar, String object);

  @Query("SELECT * FROM caldav_tasks WHERE task = :taskId")
  List<CaldavTask> getTasks(long taskId);

  @Query("SELECT * FROM caldav_calendar ORDER BY name COLLATE NOCASE")
  List<CaldavCalendar> getCalendars();

  @Query("SELECT * FROM caldav_calendar WHERE account = :account ORDER BY name COLLATE NOCASE")
  List<CaldavCalendar> getCalendarsByAccount(String account);

  @Query("SELECT * FROM caldav_calendar WHERE uuid = :uuid LIMIT 1")
  CaldavCalendar getCalendar(String uuid);

  @Query(
      "SELECT * FROM caldav_calendar WHERE account = :account AND name = :name COLLATE NOCASE LIMIT 1")
  CaldavCalendar getCalendar(String account, String name);

  @Query("SELECT object FROM caldav_tasks WHERE calendar = :calendar")
  List<String> getObjects(String calendar);

  @Query("SELECT task FROM caldav_tasks WHERE calendar = :calendar AND object IN (:objects)")
  List<Long> getTasks(String calendar, List<String> objects);

  @Query("SELECT * FROM caldav_calendar WHERE account = :account AND url NOT IN (:urls)")
  List<CaldavCalendar> findDeletedCalendars(String account, List<String> urls);

  @Query("SELECT * FROM caldav_calendar WHERE account = :account AND url = :url LIMIT 1")
  CaldavCalendar getCalendarByUrl(String account, String url);

  @Query("SELECT * FROM caldav_account WHERE name = :name COLLATE NOCASE LIMIT 1")
  CaldavAccount getAccountByName(String name);

  @Query("SELECT DISTINCT calendar FROM caldav_tasks WHERE deleted = 0 AND task IN (:tasks)")
  List<String> getCalendars(List<Long> tasks);
}
