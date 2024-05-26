package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.LABEL
import org.tasks.data.NO_ORDER
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
    @ColumnInfo(name = "name")
    val name: String? = "",
    @ColumnInfo(name = "color")
    val color: Int? = 0,
    @ColumnInfo(name = "tagOrdering")
    val tagOrdering: String? = "[]",
    @ColumnInfo(name = "td_icon")
    private val icon: Int? = -1,
    @ColumnInfo(name = "td_order")
    val order: Int = NO_ORDER,
) : CommonParcelable {
    @Suppress("RedundantNullableReturnType")
    fun getIcon(): Int? = icon ?: LABEL
}
