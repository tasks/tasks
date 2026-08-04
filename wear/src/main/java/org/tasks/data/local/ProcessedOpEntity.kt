/**
 * ProcessedOpEntity.kt — Idempotency guard for phone → watch operations.
 *
 * Stores the `opId` of every incoming phone operation that has been
 * applied. Before applying a new operation, [SyncRepository] checks
 * this table to skip duplicates.
 *
 * Old entries are purged by [SyncRepository.cleanupOldProcessedOps]
 * (default: 7 days).
 */
package org.tasks.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to track processed operations from phone to avoid duplicates (idempotency).
 * When we receive an operation from the phone, we store its ID here to prevent
 * processing it again if it arrives multiple times.
 */
@Entity(tableName = "processed_ops")
data class ProcessedOpEntity(
    @PrimaryKey
    val opId: String,  // Unique operation ID from phone
    val processedAt: Long = System.currentTimeMillis(),
)
