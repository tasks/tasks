package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import java.util.List;

@Dao
public interface GoogleTaskListDao {

  @Query("SELECT * FROM google_task_accounts")
  List<GoogleTaskAccount> getAccounts();

  @Query("SELECT * FROM google_task_accounts WHERE account = :account COLLATE NOCASE LIMIT 1")
  GoogleTaskAccount getAccount(String account);

  @Query("SELECT * FROM google_task_lists WHERE _id = :id")
  GoogleTaskList getById(long id);

  @Query("SELECT * FROM google_task_lists WHERE account = :account AND deleted = 0 ORDER BY title ASC")
  List<GoogleTaskList> getActiveLists(String account);

  @Query("SELECT * FROM google_task_lists WHERE remote_id = :remoteId LIMIT 1")
  GoogleTaskList getByRemoteId(String remoteId);

  @Query("SELECT * FROM google_task_lists WHERE remote_id = :remoteId AND IFNULL(account, '') = '' LIMIT 1")
  GoogleTaskList findExistingList(String remoteId);

  @Query("SELECT * FROM google_task_lists")
  List<GoogleTaskList> getAll();

  @Query("SELECT * FROM google_task_lists WHERE deleted = 0")
  List<GoogleTaskList> getAllActiveLists();

  @Query("DELETE FROM google_task_lists WHERE _id = :id")
  void deleteById(long id);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insertOrReplace(GoogleTaskList googleTaskList);

  @Insert
  void insert(GoogleTaskList googleTaskList);

  @Insert
  void insert(GoogleTaskAccount googleTaskAccount);

  @Update
  void update(GoogleTaskList googleTaskList);

  @Update
  void update(GoogleTaskAccount account);

  @Delete
  void delete(GoogleTaskList list);

  @Delete
  void delete(GoogleTaskAccount account);
}
