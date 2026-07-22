/**
 * SettingsRepository.kt — Read / write access for user preferences on Wear.
 *
 * Wraps [SettingsDao] and adds business logic:
 * - [ensureSettingsExist] seeds the default singleton row.
 * - Mutators ([setShowHidden], [setShowCompleted], etc.) implicitly
 *   mark the row as `dirty = true`.
 * - [applyFromPhone] merges incoming phone settings **only when the
 *   local row is not dirty**, so user changes made while offline are
 *   never silently overwritten.
 *
 * ## Singleton
 * Accessed via [SettingsRepository.getInstance].
 */
package org.tasks.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Repository that provides access to local settings.
 * Works offline and syncs with phone when connected.
 */
class SettingsRepository(context: Context) {

    private val database = WearDatabase.getInstance(context)
    private val settingsDao = database.settingsDao()

    /**
     * Get settings as a Flow for reactive updates.
     * Returns default settings if none exist.
     */
    fun getSettings(): Flow<SettingsEntity> {
        return settingsDao.getSettings().map { it ?: SettingsEntity() }
    }

    /**
     * Get settings once (non-reactive).
     */
    suspend fun getSettingsOnce(): SettingsEntity {
        return settingsDao.getSettingsOnce() ?: SettingsEntity()
    }

    /**
     * Ensure settings exist in the database.
     */
    suspend fun ensureSettingsExist() {
        if (!settingsDao.hasSettings()) {
            settingsDao.insertOrUpdate(SettingsEntity())
            Timber.d("Created default settings")
        }
    }

    /**
     * Update show hidden setting.
     */
    suspend fun setShowHidden(showHidden: Boolean) {
        ensureSettingsExist()
        settingsDao.setShowHidden(showHidden)
        Timber.d("Set showHidden=$showHidden")
    }

    /**
     * Update show completed setting.
     */
    suspend fun setShowCompleted(showCompleted: Boolean) {
        ensureSettingsExist()
        settingsDao.setShowCompleted(showCompleted)
        Timber.d("Set showCompleted=$showCompleted")
    }

    /**
     * Update filter.
     */
    suspend fun setFilter(filter: String, collapsedGroups: String = "") {
        ensureSettingsExist()
        settingsDao.setFilter(filter, collapsedGroups)
        Timber.d("Set filter=$filter")
    }

    /**
     * Add or remove a group from collapsed list.
     */
    suspend fun toggleGroupCollapsed(groupId: Long, collapsed: Boolean) {
        ensureSettingsExist()
        val settings = getSettingsOnce()
        val collapsedList = settings.collapsedGroups
            .split(",")
            .filter { it.isNotBlank() }
            .map { it.toLong() }
            .toMutableSet()

        if (collapsed) {
            collapsedList.add(groupId)
        } else {
            collapsedList.remove(groupId)
        }

        settingsDao.setCollapsedGroups(collapsedList.joinToString(","))
        Timber.d("Toggled group $groupId collapsed=$collapsed")
    }

    /**
     * Get collapsed group IDs as a set.
     */
    suspend fun getCollapsedGroups(): Set<Long> {
        val settings = getSettingsOnce()
        return settings.collapsedGroups
            .split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    /**
     * Apply settings from phone (sync).
     */
    suspend fun applyFromPhone(
        showHidden: Boolean,
        showCompleted: Boolean,
        filter: String,
        collapsedGroups: Set<Long>,
    ) {
        ensureSettingsExist()
        val current = getSettingsOnce()

        // Only apply if not dirty (local changes take priority)
        if (!current.dirty) {
            settingsDao.insertOrUpdate(
                SettingsEntity(
                    showHidden = showHidden,
                    showCompleted = showCompleted,
                    filter = filter,
                    collapsedGroups = collapsedGroups.joinToString(","),
                    dirty = false,
                    syncedAt = System.currentTimeMillis(),
                )
            )
            Timber.d("Applied settings from phone")
        } else {
            Timber.d("Skipped applying settings from phone (local changes pending)")
        }
    }

    /**
     * Mark settings as synced.
     */
    suspend fun markSynced() {
        settingsDao.markSynced()
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
        }
    }
}
