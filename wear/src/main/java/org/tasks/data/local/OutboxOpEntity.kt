/**
 * OutboxOpEntity.kt — Outbox queue row for watch → phone operations.
 *
 * ## State machine
 * ```
 *  PENDING ──▶ SENDING ──▶ SENT ──▶ ACKED  (happy path)
 *     │            │          │
 *     └────────────┴──────────┘──▶ FAILED  (after max retries)
 * ```
 *
 * [SyncWorker] periodically drains the queue.  Rows in `ACKED` state
 * are eventually cleaned up by [SyncRepository.cleanupAckedOps].
 */
package org.tasks.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a pending sync operation in the outbox.
 * Operations are queued here when the user creates/modifies/deletes tasks on the watch,
 * then sent to the phone via Data Layer and removed once acknowledged.
 */
@Entity(tableName = "outbox_ops")
data class OutboxOpEntity(
    @PrimaryKey(autoGenerate = true)
    val opId: Long = 0,
    val taskId: String,
    val type: OutboxOpType,
    val payload: String,  // JSON-serialized operation data
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val state: OutboxOpState = OutboxOpState.PENDING,
    val lastAttemptAt: Long = 0,
    val errorMessage: String? = null,
)

/**
 * Type of outbox operation.
 */
enum class OutboxOpType {
    CREATE,
    UPDATE,
    DELETE,
    COMPLETE,
}

/**
 * State of an outbox operation.
 */
enum class OutboxOpState {
    PENDING,     // Not yet sent
    SENDING,     // Currently being sent
    SENT,        // Sent, waiting for ack
    ACKED,       // Acknowledged by phone
    FAILED,      // Failed after max retries
}
