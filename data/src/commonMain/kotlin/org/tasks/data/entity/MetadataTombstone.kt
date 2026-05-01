package org.tasks.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity(tableName = "metadata_tombstone", primaryKeys = ["category", "entity_key"])
data class MetadataTombstone(
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "entity_key") val key: String,
    @ColumnInfo(name = "ts") val ts: Long,
)
