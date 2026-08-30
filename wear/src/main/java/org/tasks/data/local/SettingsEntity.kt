/**
 * SettingsEntity.kt — Singleton settings row for the Wear app.
 *
 * The table always has exactly one row (`id = 1`).
 * [SettingsRepository.ensureSettingsExist] creates the default row
 * on first launch.
 *
 * Settings are written locally first (setting `dirty = true`) and
 * synced to the phone when connectivity is available. Incoming
 * settings from the phone are only applied when the local row is
 * *not* dirty, so local edits are never silently overwritten.
 */
package org.tasks.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing local settings for the Wear app.
 * These settings are stored locally and synced with the phone when connected.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,  // Always use id=1 for singleton settings
    val showHidden: Boolean = false,
    val showCompleted: Boolean = false,
    @ColumnInfo(name = "filter_value")
    val filter: String = "",
    val collapsedGroups: String = "",  // Comma-separated list of collapsed group IDs
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = false,  // True if local changes not yet synced
    val syncedAt: Long = 0L,  // Last successful sync timestamp
)
