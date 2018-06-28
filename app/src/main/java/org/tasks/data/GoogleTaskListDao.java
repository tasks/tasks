package org.tasks.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import java.util.List;

@Dao
public abstract class GoogleTaskListDao {

  @Query("SELECT * FROM google_task_accounts")
  public abstract List<GoogleTaskAccount> getAccounts();

  @Query("SELECT * FROM google_task_accounts WHERE account = :account COLLATE NOCASE LIMIT 1")
  public abstract GoogleTaskAccount getAccount(String account);

  @Query("SELECT * FROM google_task_lists WHERE _id = :id")
  public abstract GoogleTaskList getById(long id);

  @Query("SELECT * FROM google_task_lists WHERE account = :account ORDER BY title ASC")
  public abstract List<GoogleTaskList> getLists(String account);

  @Query("SELECT * FROM google_task_lists WHERE remote_id = :remoteId LIMIT 1")
  public abstract GoogleTaskList getByRemoteId(String remoteId);

  @Query(
      "SELECT * FROM google_task_lists WHERE remote_id = :remoteId AND IFNULL(account, '') = '' LIMIT 1")
  public abstract GoogleTaskList findExistingList(String remoteId);

  @Query("SELECT * FROM google_task_lists")
  public abstract List<GoogleTaskList> getAllLists();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract long insertOrReplace(GoogleTaskList googleTaskList);

  @Insert
  public abstract void insert(GoogleTaskList googleTaskList);

  @Insert
  public abstract void insert(GoogleTaskAccount googleTaskAccount);

  @Update
  public abstract void update(GoogleTaskList googleTaskList);

  @Update
  public abstract void update(GoogleTaskAccount account);
}
