package org.tasks.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.todoroo.astrid.api.FilterListItem.NO_ORDER
import io.reactivex.Single
import org.tasks.filters.GoogleTaskFilters
import org.tasks.time.DateTimeUtils.currentTimeMillis

@Dao
interface GoogleTaskListDao {
    @Query("SELECT COUNT(*) FROM google_task_accounts")
    fun accountCount(): Single<Int>

    @Query("SELECT * FROM google_task_accounts")
    fun getAccounts(): List<GoogleTaskAccount>

    @Query("SELECT * FROM google_task_accounts WHERE gta_account = :account COLLATE NOCASE LIMIT 1")
    fun getAccount(account: String): GoogleTaskAccount?

    @Query("SELECT * FROM google_task_lists WHERE gtl_id = :id")
    fun getById(id: Long): GoogleTaskList?

    @Query("SELECT * FROM google_task_lists WHERE gtl_account = :account ORDER BY gtl_title ASC")
    fun getLists(account: String): List<GoogleTaskList>

    @Query("SELECT * FROM google_task_lists WHERE gtl_remote_id = :remoteId LIMIT 1")
    fun getByRemoteId(remoteId: String): GoogleTaskList?

    @Query("SELECT * FROM google_task_lists WHERE gtl_remote_id IN (:remoteIds)")
    fun getByRemoteId(remoteIds: List<String>): List<GoogleTaskList>

    @Query("SELECT * FROM google_task_lists")
    fun subscribeToLists(): LiveData<List<GoogleTaskList>>

    @Query("SELECT * FROM google_task_lists WHERE gtl_remote_id = :remoteId AND IFNULL(gtl_account, '') = ''")
    fun findExistingList(remoteId: String): GoogleTaskList?

    @Query("SELECT * FROM google_task_lists")
    fun getAllLists(): List<GoogleTaskList>

    @Query("UPDATE google_task_lists SET gtl_last_sync = 0 WHERE gtl_account = :account")
    fun resetLastSync(account: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(googleTaskList: GoogleTaskList): Long

    @Insert
    fun insert(googleTaskList: GoogleTaskList): Long

    @Insert
    fun insert(googleTaskAccount: GoogleTaskAccount)

    @Update
    fun update(account: GoogleTaskAccount)

    @Update
    fun update(list: GoogleTaskList)

    @Query("SELECT google_task_lists.*, COUNT(tasks._id) AS count"
            + " FROM google_task_lists "
            + " LEFT JOIN google_tasks ON google_tasks.gt_list_id = google_task_lists.gtl_remote_id"
            + " LEFT JOIN tasks ON google_tasks.gt_task = tasks._id AND tasks.deleted = 0 AND tasks.completed = 0 AND tasks.hideUntil < :now AND gt_deleted = 0"
            + " WHERE google_task_lists.gtl_account = :account"
            + " GROUP BY google_task_lists.gtl_remote_id")
    fun getGoogleTaskFilters(account: String, now: Long = currentTimeMillis()): List<GoogleTaskFilters>

    @Query("UPDATE google_task_lists SET gtl_remote_order = $NO_ORDER")
    fun resetOrders()

    @Query("UPDATE google_task_lists SET gtl_remote_order = :order WHERE gtl_id = :id")
    fun setOrder(id: Long, order: Int)
}