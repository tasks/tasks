package org.tasks.data

import androidx.room.*
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UserActivityDao {
    @Insert
    abstract suspend fun insert(userActivity: UserActivity)

    @Update
    abstract suspend fun update(userActivity: UserActivity)

    @Delete
    abstract suspend fun delete(userActivity: UserActivity)

    @Query("SELECT * FROM userActivity WHERE target_id = :taskUuid ORDER BY created_at DESC ")
    abstract fun watchComments(taskUuid: String): Flow<List<UserActivity>>

    @Query("SELECT userActivity.* FROM userActivity INNER JOIN tasks ON tasks._id = :task WHERE target_id = tasks.remoteId")
    abstract suspend fun getComments(task: Long): List<UserActivity>

    @Query("SELECT * FROM userActivity")
    abstract suspend fun getComments(): List<UserActivity>

    suspend fun createNew(item: UserActivity) {
        if (item.created == null || item.created == 0L) {
            item.created = DateUtilities.now()
        }
        if (Task.isUuidEmpty(item.remoteId)) {
            item.remoteId = UUIDHelper.newUUID()
        }
        insert(item)
    }
}