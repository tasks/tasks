package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "metadata_sync_state", primaryKeys = ["category", "local_id"])
data class MetadataSyncState(
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "local_id") val localId: Long,
    @ColumnInfo(name = "dirty_version", defaultValue = "0") val dirtyVersion: Long = 0,
    @ColumnInfo(name = "synced_version", defaultValue = "0") val syncedVersion: Long = 0,
    @ColumnInfo(name = "reaped", defaultValue = "0") val reaped: Boolean = false,
)
