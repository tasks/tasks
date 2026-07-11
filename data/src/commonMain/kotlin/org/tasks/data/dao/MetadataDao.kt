package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.tasks.data.entity.MetadataSyncState
import org.tasks.data.entity.MetadataTombstone

@Dao
abstract class MetadataDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun ensureStates(states: List<MetadataSyncState>)

    @Query("UPDATE metadata_sync_state SET dirty_version = dirty_version + 1, reaped = 0 WHERE category = :category AND local_id IN (:ids)")
    internal abstract suspend fun bumpDirty(category: String, ids: List<Long>)

    suspend fun markDirty(category: String, ids: List<Long>) {
        if (ids.isEmpty()) return
        ensureStates(ids.map { MetadataSyncState(category, it) })
        bumpDirty(category, ids)
    }

    @Query("UPDATE metadata_sync_state SET dirty_version = dirty_version + 1 WHERE category = :category AND reaped = 0 AND local_id IN (:ids)")
    internal abstract suspend fun bumpDirtyIfLive(category: String, ids: List<Long>)

    suspend fun healDirty(category: String, ids: List<Long>) {
        if (ids.isEmpty()) return
        ensureStates(ids.map { MetadataSyncState(category, it) })
        bumpDirtyIfLive(category, ids)
    }

    @Query("UPDATE metadata_sync_state SET reaped = 0, dirty_version = dirty_version + 1 WHERE category = :category AND reaped = 1 AND local_id IN (:ids)")
    abstract suspend fun unreap(category: String, ids: List<Long>)

    @Query("UPDATE metadata_sync_state SET synced_version = :version WHERE category = :category AND local_id = :id AND synced_version < :version")
    abstract suspend fun markSynced(category: String, id: Long, version: Long)

    @Query("UPDATE metadata_sync_state SET synced_version = dirty_version WHERE category = :category AND local_id IN (:ids)")
    abstract suspend fun clearDirty(category: String, ids: List<Long>)

    @Query("UPDATE metadata_sync_state SET synced_version = dirty_version WHERE category = :category AND dirty_version > synced_version")
    abstract suspend fun clearAllDirty(category: String)

    @Query("UPDATE metadata_sync_state SET reaped = :reaped WHERE category = :category AND local_id IN (:ids)")
    internal abstract suspend fun setReapedFlag(category: String, ids: List<Long>, reaped: Boolean)

    suspend fun setReaped(category: String, ids: List<Long>, reaped: Boolean) {
        if (ids.isEmpty()) return
        if (reaped) ensureStates(ids.map { MetadataSyncState(category, it) })
        setReapedFlag(category, ids, reaped)
    }

    @Query("UPDATE metadata_sync_state SET reaped = 0 WHERE category = :category AND reaped = 1")
    abstract suspend fun clearAllReaped(category: String)

    @Query("SELECT EXISTS(SELECT 1 FROM metadata_sync_state WHERE category = :category AND reaped = 1)")
    abstract suspend fun hasReaped(category: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM metadata_sync_state WHERE category = :category AND dirty_version > synced_version)")
    abstract suspend fun hasDirty(category: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTombstone(tombstone: MetadataTombstone)

    @Query("SELECT * FROM metadata_tombstone WHERE category = :category")
    abstract suspend fun getTombstones(category: String): List<MetadataTombstone>

    @Query("SELECT entity_key FROM metadata_tombstone WHERE category = :category")
    abstract suspend fun getTombstoneKeys(category: String): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM metadata_tombstone WHERE category = :category)")
    abstract suspend fun hasTombstones(category: String): Boolean

    @Query("DELETE FROM metadata_tombstone WHERE category = :category AND entity_key IN (:keys)")
    abstract suspend fun deleteTombstones(category: String, keys: List<String>)

    @Query("DELETE FROM metadata_tombstone WHERE category = :category")
    abstract suspend fun clearAllTombstones(category: String)
}
