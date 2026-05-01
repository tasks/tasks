package org.tasks.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.NO_ORDER
import org.tasks.data.Redacted
import org.tasks.data.UUIDHelper

@CommonParcelize
@Serializable
@Entity(tableName = "tagdata")
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
) : CommonParcelable
