/**
 * OutboxOpDao.kt — Room DAO for the `outbox_ops` table.
 *
 * Provides CRUD + state-transition queries for the outbound sync queue.
 * State transitions follow the lifecycle described in [OutboxOpEntity].
 */
package org.tasks.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for OutboxOpEntity.
 * Manages the queue of pending sync operations.
 */
@Dao
interface OutboxOpDao {

    /**
     * Get all pending operations ordered by creation time.
     */
    @Query("SELECT * FROM outbox_ops WHERE state = 'PENDING' OR state = 'SENDING' ORDER BY createdAt ASC")
    fun getPendingOps(): Flow<List<OutboxOpEntity>>

    /**
     * Get pending operations once (non-reactive).
     */
    @Query("SELECT * FROM outbox_ops WHERE state = 'PENDING' OR state = 'SENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOpsOnce(): List<OutboxOpEntity>

    /**
     * Get an operation by ID.
     */
    @Query("SELECT * FROM outbox_ops WHERE opId = :opId LIMIT 1")
    suspend fun getOp(opId: Long): OutboxOpEntity?

    /**
     * Get operations by task ID.
     */
    @Query("SELECT * FROM outbox_ops WHERE taskId = :taskId ORDER BY createdAt DESC")
    suspend fun getOpsByTaskId(taskId: String): List<OutboxOpEntity>

    /**
     * Insert a new operation.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(op: OutboxOpEntity): Long

    /**
     * Update an operation.
     */
    @Update
    suspend fun update(op: OutboxOpEntity)

    /**
     * Mark operation as sending.
     */
    @Query("UPDATE outbox_ops SET state = 'SENDING', lastAttemptAt = :timestamp, attempts = attempts + 1 WHERE opId = :opId")
    suspend fun markSending(opId: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark operation as sent (waiting for ack).
     */
    @Query("UPDATE outbox_ops SET state = 'SENT', lastAttemptAt = :timestamp WHERE opId = :opId")
    suspend fun markSent(opId: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark operation as acknowledged.
     */
    @Query("UPDATE outbox_ops SET state = 'ACKED' WHERE opId = :opId")
    suspend fun markAcked(opId: Long)

    /**
     * Mark operation as failed.
     */
    @Query("UPDATE outbox_ops SET state = 'FAILED', errorMessage = :error WHERE opId = :opId")
    suspend fun markFailed(opId: Long, error: String?)

    /**
     * Reset stuck operations (sending for too long).
     */
    @Query("UPDATE outbox_ops SET state = 'PENDING' WHERE state = 'SENDING' AND lastAttemptAt < :threshold")
    suspend fun resetStuckOps(threshold: Long)

    /**
     * Delete acknowledged operations.
     */
    @Query("DELETE FROM outbox_ops WHERE state = 'ACKED'")
    suspend fun deleteAckedOps()

    /**
     * Delete operation by ID.
     */
    @Query("DELETE FROM outbox_ops WHERE opId = :opId")
    suspend fun delete(opId: Long)

    /**
     * Get count of pending operations.
     */
    @Query("SELECT COUNT(*) FROM outbox_ops WHERE state = 'PENDING' OR state = 'SENDING'")
    suspend fun getPendingCount(): Int

    /**
     * Get operations that need retry (failed less than max attempts).
     */
    @Query("SELECT * FROM outbox_ops WHERE state = 'SENT' AND lastAttemptAt < :threshold ORDER BY createdAt ASC")
    suspend fun getOpsNeedingRetry(threshold: Long): List<OutboxOpEntity>
}
