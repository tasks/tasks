package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.CommonIgnoredOnParcel
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.NO_ORDER
import org.tasks.data.Redacted
import org.tasks.data.UUIDHelper

@CommonParcelize
@Serializable
@Entity(
    tableName = "tagdata",
    indices = [Index(value = ["normalized_name"], unique = true)],
)
data class TagData(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    val id: Long? = null,
    @ColumnInfo(name = "remoteId")
    val remoteId: String? = UUIDHelper.newUUID(),
    @Redacted
    @ColumnInfo(name = "name")
    val name: String? = "",
    @ColumnInfo(name = "color")
    val color: Int? = 0,
    @ColumnInfo(name = "tagOrdering")
    val tagOrdering: String? = "[]",
    @ColumnInfo(name = "td_icon")
    val icon: String? = null,
    @ColumnInfo(name = "td_order")
    val order: Int = NO_ORDER,
) : CommonParcelable {
    @Transient
    @CommonIgnoredOnParcel
    @ColumnInfo(name = "normalized_name", defaultValue = "")
    var normalizedName: String = normalize(name)
        internal set

    companion object {
        /** Locale-invariant fold used everywhere the normalized name is computed. */
        fun normalize(name: String?): String = (name ?: "").trim().lowercase()
    }
}

fun TagData.isSyncable(): Boolean = id != null && remoteId != null && !name.isNullOrBlank()

fun normalizeColor(color: Int?): Int? = color?.takeIf { it != 0 }

fun normalizeIcon(icon: String?): String? = icon?.takeIf { it.isNotBlank() }
