/**
 * SettingsDao.kt — Room DAO for the singleton `settings` table.
 *
 * Every write method sets `dirty = 1` so the sync layer knows
 * there are local changes to push to the phone.
 */
package org.tasks.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SettingsEntity.
 * Provides methods for accessing local settings.
 */
@Dao
interface SettingsDao {

    /**
     * Get settings as a Flow for reactive updates.
     */
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<SettingsEntity?>

    /**
     * Get settings once (non-reactive).
     */
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): SettingsEntity?

    /**
     * Insert or replace settings.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: SettingsEntity)

    /**
     * Update show hidden setting.
     */
    @Query("UPDATE settings SET showHidden = :showHidden, updatedAt = :timestamp, dirty = 1 WHERE id = 1")
    suspend fun setShowHidden(showHidden: Boolean, timestamp: Long = System.currentTimeMillis())

    /**
     * Update show completed setting.
     */
    @Query("UPDATE settings SET showCompleted = :showCompleted, updatedAt = :timestamp, dirty = 1 WHERE id = 1")
    suspend fun setShowCompleted(showCompleted: Boolean, timestamp: Long = System.currentTimeMillis())

    /**
     * Update filter.
     */
    @Query("UPDATE settings SET filter_value = :filter, collapsedGroups = :collapsedGroups, updatedAt = :timestamp, dirty = 1 WHERE id = 1")
    suspend fun setFilter(filter: String, collapsedGroups: String = "", timestamp: Long = System.currentTimeMillis())

    /**
     * Update collapsed groups.
     */
    @Query("UPDATE settings SET collapsedGroups = :collapsedGroups, updatedAt = :timestamp, dirty = 1 WHERE id = 1")
    suspend fun setCollapsedGroups(collapsedGroups: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark settings as synced.
     */
    @Query("UPDATE settings SET dirty = 0, syncedAt = :timestamp WHERE id = 1")
    suspend fun markSynced(timestamp: Long = System.currentTimeMillis())

    /**
     * Check if settings exist.
     */
    @Query("SELECT COUNT(*) > 0 FROM settings WHERE id = 1")
    suspend fun hasSettings(): Boolean
}
