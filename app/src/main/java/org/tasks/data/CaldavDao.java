package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;
import java.util.Set;

@Dao
public interface CaldavDao {

    @Query("SELECT * FROM caldav_account WHERE uuid = :uuid LIMIT 1")
    CaldavAccount getByUuid(String uuid);

    @Query("SELECT * FROM caldav_account ORDER BY UPPER(name) ASC")
    List<CaldavAccount> getAllOrderedByName();

    @Insert
    long insert(CaldavAccount caldavAccount);

    @Update
    void update(CaldavAccount caldavAccount);

    @Insert
    void insert(CaldavTask caldavTask);

    @Update
    void update(CaldavTask caldavTask);

    @Delete
    void delete(CaldavTask caldavTask);

    @Query("SELECT * FROM caldav_tasks WHERE task = :taskId AND deleted > 0 AND account = :account")
    List<CaldavTask> getDeleted(long taskId, String account);

    @Query("SELECT * FROM caldav_tasks WHERE task = :taskId AND deleted = 0 LIMIT 1")
    CaldavTask getTask(long taskId);

    @Query("SELECT * FROM caldav_tasks WHERE account = :account AND object = :object LIMIT 1")
    CaldavTask getTask(String account, String object);

    @Query("DELETE FROM caldav_tasks WHERE task = :taskId")
    void deleteById(long taskId);

    @Query("SELECT * FROM caldav_tasks WHERE task = :taskId")
    List<CaldavTask> getTasks(long taskId);

    @Query("SELECT * FROM caldav_account")
    List<CaldavAccount> getAccounts();

    @Delete
    void delete(CaldavAccount caldavAccount);

    @Query("SELECT * FROM caldav_account WHERE uuid = :uuid LIMIT 1")
    CaldavAccount getAccount(String uuid);

    @Query("DELETE FROM caldav_tasks WHERE account = :uuid")
    void deleteTasksForAccount(String uuid);

    @Query("SELECT object FROM caldav_tasks WHERE account = :account")
    List<String> getObjects(String account);

    @Query("SELECT task FROM caldav_tasks WHERE account = :account AND object IN (:objects)")
    List<Long> getTasks(String account, List<String> objects);

    @Query("DELETE FROM caldav_tasks WHERE account = :account AND object IN (:objects)")
    void deleteObjects(String account, List<String> objects);
}
