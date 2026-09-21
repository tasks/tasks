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

@Serializable
@CommonParcelize
@Entity(tableName = "filters")
data class Filter(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    val id: Long = 0,
    @Redacted
    @ColumnInfo(name = "title")
    val title: String? = null,
    @Redacted
    @ColumnInfo(name = "sql")
    val sql: String? = null,
    @Redacted
    @ColumnInfo(name = "values")
    val values: String? = null,
    @Redacted
    @ColumnInfo(name = "criterion")
    val criterion: String? = null,
    @ColumnInfo(name = "f_color")
    val color: Int? = 0,
    @ColumnInfo(name = "f_icon")
    val icon: String? = null,
    @ColumnInfo(name = "f_order")
    val order: Int = NO_ORDER,
) : CommonParcelable
