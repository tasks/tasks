package org.tasks.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.todoroo.astrid.api.FilterListItem.NO_ORDER
import org.tasks.filters.GoogleTaskFilters
import org.tasks.time.DateTimeUtils.currentTimeMillis

@Dao
interface GoogleTaskListDao {
    @Query("SELECT COUNT(*) FROM google_task_accounts")
    suspend fun accountCount(): Int

    @Query("SELECT * FROM google_task_accounts")
    suspend fun getAccounts(): List<GoogleTaskAccount>

    @Query("SELECT * FROM google_task_accounts")
    fun watchAccounts(): LiveData<List<GoogleTaskAccount>>

    @Query("SELECT * FROM google_task_accounts WHERE gta_account = :account COLLATE NOCASE LIMIT 1")
    suspend fun getAccount(account: String): GoogleTaskAccount?

    @Query("SELECT * FROM google_task_lists WHERE gtl_id = :id")
    suspend fun getById(id: Long): GoogleTaskList?

    @Query("SELECT * FROM google_task_lists WHERE gtl_account = :account ORDER BY gtl_title ASC")
    suspend fun getLists(account: String): List<GoogleTaskList>

    @Query("SELECT * FROM google_task_lists WHERE gtl_remote_id = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): GoogleTaskList?

    @Query("SELECT * FROM google_task_lists WHERE gtl_remote_id IN (:remoteIds)")
    suspend fun getByRemoteId(remoteIds: List<String>): List<GoogleTaskList>

    @Query("SELECT * FROM google_task_lists")
    fun subscribeToLists(): LiveData<List<GoogleTaskList>>

    @Query("SELECT * FROM google_task_lists WHERE gtl_remote_id = :remoteId AND IFNULL(gtl_account, '') = ''")
    suspend fun findExistingList(remoteId: String): GoogleTaskList?

    @Query("SELECT * FROM google_task_lists")
    suspend fun getAllLists(): List<GoogleTaskList>

    @Query("UPDATE google_task_lists SET gtl_last_sync = 0 WHERE gtl_account = :account")
    suspend fun resetLastSync(account: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(googleTaskList: GoogleTaskList): Long

    @Insert
    suspend fun insert(googleTaskList: GoogleTaskList): Long

    @Insert
    suspend fun insert(googleTaskAccount: GoogleTaskAccount)

    @Update
    suspend fun update(account: GoogleTaskAccount)

    @Update
    suspend fun update(list: GoogleTaskList)

    @Query("SELECT google_task_lists.*, COUNT(tasks._id) AS count"
            + " FROM google_task_lists "
            + " LEFT JOIN google_tasks ON google_tasks.gt_list_id = google_task_lists.gtl_remote_id"
            + " LEFT JOIN tasks ON google_tasks.gt_task = tasks._id AND tasks.deleted = 0 AND tasks.completed = 0 AND tasks.hideUntil < :now AND gt_deleted = 0"
            + " WHERE google_task_lists.gtl_account = :account"
            + " GROUP BY google_task_lists.gtl_remote_id")
    suspend fun getGoogleTaskFilters(account: String, now: Long = currentTimeMillis()): List<GoogleTaskFilters>

    @Query("UPDATE google_task_lists SET gtl_remote_order = $NO_ORDER")
    suspend fun resetOrders()

    @Query("UPDATE google_task_lists SET gtl_remote_order = :order WHERE gtl_id = :id")
    suspend fun setOrder(id: Long, order: Int)
}