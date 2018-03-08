package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface CaldavDao {

    @Query("SELECT * FROM caldav_account WHERE name = :name COLLATE NOCASE LIMIT 1")
    CaldavAccount getAccountByName(String name);

    @Query("SELECT * FROM caldav_account WHERE uuid = :uuid LIMIT 1")
    CaldavAccount getByUuid(String uuid);

    @Query("SELECT * FROM caldav_account WHERE deleted = 0 ORDER BY UPPER(name) ASC")
    List<CaldavAccount> getAllOrderedByName();

    @Insert
    void insert(CaldavAccount caldavAccount);

    @Update
    void update(CaldavAccount caldavAccount);

    @Query("UPDATE caldav_account SET deleted = (strftime('%s','now')*1000) WHERE uuid = :uuid")
    void markAccountDeleted(String uuid);

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

    @Query("SELECT * FROM caldav_tasks WHERE remote_id = :remoteId LIMIT 1")
    CaldavTask getTask(String remoteId);

    @Query("DELETE FROM caldav_tasks WHERE task = :taskId")
    void deleteById(long taskId);

    @Query("SELECT * FROM caldav_tasks WHERE task = :taskId")
    List<CaldavTask> getTasks(long taskId);

    @Query("SELECT * FROM caldav_account")
    List<CaldavAccount> getAccounts();
}
