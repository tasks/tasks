package org.tasks.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Single;
import java.util.List;
import org.tasks.filters.GoogleTaskFilters;

@Dao
public interface GoogleTaskListDao {

  @Query("SELECT COUNT(*) FROM google_task_accounts")
  Single<Integer> accountCount();

  @Query("SELECT * FROM google_task_accounts")
  List<GoogleTaskAccount> getAccounts();

  @Query("SELECT * FROM google_task_accounts WHERE gta_account = :account COLLATE NOCASE LIMIT 1")
  GoogleTaskAccount getAccount(String account);

  @Query("SELECT * FROM google_task_lists WHERE gtl_id = :id")
  GoogleTaskList getById(long id);

  @Query("SELECT * FROM google_task_lists WHERE gtl_account = :account ORDER BY gtl_title ASC")
  List<GoogleTaskList> getLists(String account);

  @Query("SELECT * FROM google_task_lists WHERE gtl_remote_id = :remoteId LIMIT 1")
  GoogleTaskList getByRemoteId(String remoteId);

  @Query("SELECT * FROM google_task_lists WHERE gtl_remote_id IN (:remoteIds)")
  List<GoogleTaskList> getByRemoteId(List<String> remoteIds);

  @Query("SELECT * FROM google_task_lists")
  LiveData<List<GoogleTaskList>> subscribeToLists();

  @Query(
      "SELECT * FROM google_task_lists WHERE gtl_remote_id = :remoteId AND IFNULL(gtl_account, '') = ''")
  GoogleTaskList findExistingList(String remoteId);

  @Query("SELECT * FROM google_task_lists")
  List<GoogleTaskList> getAllLists();

  @Query("UPDATE google_task_lists SET gtl_last_sync = 0 WHERE gtl_account = :account")
  void resetLastSync(String account);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insertOrReplace(GoogleTaskList googleTaskList);

  @Insert
  long insert(GoogleTaskList googleTaskList);

  @Insert
  void insert(GoogleTaskAccount googleTaskAccount);

  @Update
  void update(GoogleTaskAccount account);

  @Update
  void update(GoogleTaskList list);

  @Query(
      "SELECT google_task_lists.*, COUNT(tasks._id) AS count"
          + " FROM google_task_lists "
          + " LEFT JOIN google_tasks ON google_tasks.gt_list_id = google_task_lists.gtl_remote_id"
          + " LEFT JOIN tasks ON google_tasks.gt_task = tasks._id AND tasks.deleted = 0 AND tasks.completed = 0 AND tasks.hideUntil < :now AND gt_deleted = 0"
          + " WHERE google_task_lists.gtl_account = :account"
          + " GROUP BY google_task_lists.gtl_remote_id")
  List<GoogleTaskFilters> getGoogleTaskFilters(String account, long now);
}
