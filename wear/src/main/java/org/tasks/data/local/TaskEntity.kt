/**
 * TaskEntity.kt — Room entity for the `tasks` table on Wear OS.
 *
 * Each row is a local copy of a task. The phone app is the ultimate
 * source of truth; this table enables full offline operation on the
 * watch.
 *
 * ## Per-field conflict resolution
 * [titleUpdatedAt], [notesUpdatedAt] and [completedUpdatedAt] track
 * the last-write timestamp per field. When an incoming update arrives
 * from the phone, [SyncRepository] compares timestamps and keeps
 * whichever value was written more recently ("last writer wins").
 *
 * ## Phone ID mapping
 * [phoneTaskId] stores the phone's numeric task PK. This prevents
 * duplicate creation when the same task arrives in both a snapshot
 * and an incremental update.
 */
package org.tasks.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a task in the local Wear database.
 * This entity maps to the "tasks" table.
 *
 * Sync-related fields:
 * - dirty: true if this task has local changes not yet synced to phone
 * - deleted: tombstone flag for soft-delete (needed for sync)
 * - titleUpdatedAt: timestamp of last title change (for per-field conflict resolution)
 * - notesUpdatedAt: timestamp of last notes change (for per-field conflict resolution)
 * - completedUpdatedAt: timestamp of last completed change (for per-field conflict resolution)
 * - syncedAt: timestamp of last successful sync with phone
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val completed: Boolean = false,
    val priority: Int = 0,
    val timestamp: String? = null,
    val repeating: Boolean = false,
    // Date/time and notification fields
    val dueDate: Long? = null,  // Due date timestamp (null = no due date)
    val dueTime: Long? = null,  // Due time timestamp (null = no specific time)
    val reminder: Boolean = false,  // Whether to send a reminder notification
    val reminderTime: Long? = null,  // When to send reminder (null = at due date/time)
    // Sync-related fields
    val dirty: Boolean = false,  // True if local changes not yet synced
    val titleUpdatedAt: Long = System.currentTimeMillis(),  // Per-field timestamp for conflict resolution
    val notesUpdatedAt: Long = System.currentTimeMillis(),  // Per-field timestamp for conflict resolution
    val completedUpdatedAt: Long = System.currentTimeMillis(),  // Per-field timestamp for conflict resolution
    val syncedAt: Long = 0L,  // Last successful sync timestamp
    val phoneTaskId: Long? = null,  // Original phone task ID for mapping
)
