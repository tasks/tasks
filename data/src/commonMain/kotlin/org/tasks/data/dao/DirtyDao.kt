package org.tasks.data.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.tasks.data.db.SuspendDbUtils.chunkedMap
import org.tasks.data.db.SuspendDbUtils.eachChunk
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPES_NON_LOCAL
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskDirtyVersion

data class TaskDirtyWithTaskId(
    @ColumnInfo(name = "cd_task") val taskId: Long,
    @Embedded val dirty: TaskDirtyVersion,
)

data class TaskToPush(
    @Embedded val task: Task,
    @ColumnInfo(name = "caldav_task_id") val caldavTaskId: Long,
    @ColumnInfo(name = "dirty_version") val dirtyVersion: Long,
    @ColumnInfo(name = "synced_version") val syncedVersion: Long,
)

@Dao
abstract class DirtyDao {

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM task_dirty
            INNER JOIN caldav_tasks ON cd_id = caldav_task_id
            LEFT JOIN caldav_lists ON cdl_uuid = cd_calendar
            LEFT JOIN caldav_accounts ON cda_uuid = cdl_account
            WHERE dirty_version > synced_version
              AND (cdl_access IS NULL OR cdl_access != $ACCESS_READ_ONLY)
              AND (cda_account_type IS NULL OR cda_account_type != $TYPE_LOCAL)
        )
    """)
    abstract fun hasDirtyTasks(): Flow<Boolean>

    @Query("""
        SELECT cd_task FROM task_dirty
        INNER JOIN caldav_tasks ON cd_id = caldav_task_id
        WHERE dirty_version > synced_version AND cd_deleted = 0
    """)
    abstract fun getDirtyTaskIds(): Flow<List<Long>>

    @Transaction
    open suspend fun setDirty(ids: List<Long>, accountTypes: List<Int> = TYPES_NON_LOCAL) =
        ids.eachChunk { upsertDirtyForAccountTypes(it, accountTypes) }

    @Query("""
        INSERT INTO task_dirty (caldav_task_id, dirty_version, synced_version)
        SELECT cd_id, 1, 0 FROM caldav_tasks
        INNER JOIN caldav_lists ON cdl_uuid = cd_calendar
        INNER JOIN caldav_accounts ON cda_uuid = cdl_account
        WHERE cd_task IN (:ids) AND cd_deleted = 0 AND cda_account_type IN (:accountTypes)
        ON CONFLICT(caldav_task_id) DO UPDATE SET dirty_version = dirty_version + 1
    """)
    internal abstract suspend fun upsertDirtyForAccountTypes(ids: List<Long>, accountTypes: List<Int>)

    @Query("SELECT synced_version FROM task_dirty WHERE caldav_task_id = :caldavTaskId")
    abstract suspend fun getSyncedVersion(caldavTaskId: Long): Long?

    @Transaction
    open suspend fun getDirtyStateByTaskIds(taskIds: List<Long>): Map<Long, TaskDirtyVersion> {
        val result = mutableMapOf<Long, TaskDirtyVersion>()
        taskIds.chunkedMap { getDirtyStateByTaskIdBatch(it) }.forEach { row ->
            val current = result[row.taskId]
            if (current == null ||
                row.dirty.dirtyVersion - row.dirty.syncedVersion > current.dirtyVersion - current.syncedVersion
            ) {
                result[row.taskId] = row.dirty
            }
        }
        return result
    }

    @Query("SELECT cd_task, task_dirty.* FROM task_dirty INNER JOIN caldav_tasks ON cd_id = caldav_task_id WHERE cd_task IN (:taskIds) AND cd_deleted = 0")
    internal abstract suspend fun getDirtyStateByTaskIdBatch(taskIds: List<Long>): List<TaskDirtyWithTaskId>

    @Query("SELECT * FROM task_dirty WHERE caldav_task_id = :caldavTaskId")
    abstract suspend fun getDirtyState(caldavTaskId: Long): TaskDirtyVersion?

    @Query("UPDATE task_dirty SET synced_version = MAX(synced_version, :version) WHERE caldav_task_id = :caldavTaskId")
    abstract suspend fun markPushed(caldavTaskId: Long, version: Long)

    suspend fun <T> withDirtyVersion(caldavTaskId: Long, dirtyVersion: Long?, block: suspend () -> T): T {
        val result = block()
        dirtyVersion?.let { markPushed(caldavTaskId, it) }
        return result
    }

    @Query("""
        UPDATE task_dirty
        SET dirty_version = 1, synced_version = 1
        WHERE caldav_task_id = :caldavTaskId
        AND synced_version = 0 AND dirty_version <= 1
    """)
    abstract suspend fun markSynced(caldavTaskId: Long)

    @Query("UPDATE task_dirty SET dirty_version = :dirtyVersion, synced_version = :syncedVersion WHERE caldav_task_id = :caldavTaskId")
    abstract suspend fun setDirtyState(caldavTaskId: Long, dirtyVersion: Long, syncedVersion: Long)

    @Transaction
    open suspend fun setDirtyState(ids: List<Long>, dirtyVersion: Long, syncedVersion: Long) =
        ids.eachChunk { setDirtyStateBatch(it, dirtyVersion, syncedVersion) }

    @Query("UPDATE task_dirty SET dirty_version = :dirtyVersion, synced_version = :syncedVersion WHERE caldav_task_id IN (:ids)")
    internal abstract suspend fun setDirtyStateBatch(ids: List<Long>, dirtyVersion: Long, syncedVersion: Long)

    @Query("SELECT dirty_version > synced_version FROM task_dirty WHERE caldav_task_id = :caldavTaskId")
    abstract suspend fun isDirty(caldavTaskId: Long): Boolean?

    @Query("""
        SELECT tasks.*, task_dirty.caldav_task_id, task_dirty.dirty_version, task_dirty.synced_version
        FROM tasks
                 INNER JOIN caldav_tasks ON tasks._id = caldav_tasks.cd_task
                 INNER JOIN task_dirty ON caldav_tasks.cd_id = task_dirty.caldav_task_id
        WHERE caldav_tasks.cd_calendar = :calendar
          AND cd_deleted = 0
          AND task_dirty.dirty_version > task_dirty.synced_version
        ORDER BY CASE WHEN parent = 0 THEN 0 ELSE 1 END, $ORDER_BY_MANUAL
    """)
    abstract suspend fun getTasksToPush(calendar: String): List<TaskToPush>
}
