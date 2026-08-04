/**
 * ProcessedOpDao.kt — Room DAO for the `processed_ops` idempotency table.
 *
 * Provides three simple operations:
 * - [isProcessed]   — O(1) check before applying an incoming phone operation.
 * - [markProcessed] — Insert after successful application.
 * - [cleanupOld]    — Purge entries older than a configurable threshold.
 */
package org.tasks.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for tracking processed operations from phone.
 */
@Dao
interface ProcessedOpDao {

    /**
     * Check if an operation has already been processed.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM processed_ops WHERE opId = :opId)")
    suspend fun isProcessed(opId: String): Boolean

    /**
     * Mark an operation as processed.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markProcessed(op: ProcessedOpEntity)

    /**
     * Clean up old processed operations (older than threshold).
     */
    @Query("DELETE FROM processed_ops WHERE processedAt < :threshold")
    suspend fun cleanupOld(threshold: Long)
}
