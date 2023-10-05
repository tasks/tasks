package org.tasks.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.todoroo.astrid.api.Filter.Companion.NO_ORDER
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "filters")
data class Filter(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    val id: Long = 0,
    @ColumnInfo(name = "title")
    val title: String? = null,
    @ColumnInfo(name = "sql")
    val sql: String? = null,
    @ColumnInfo(name = "values")
    val values: String? = null,
    @ColumnInfo(name = "criterion")
    val criterion: String? = null,
    @ColumnInfo(name = "f_color")
    val color: Int? = 0,
    @ColumnInfo(name = "f_icon")
    val icon: Int? = -1,
    @ColumnInfo(name = "f_order")
    val order: Int = NO_ORDER,
) : Parcelable
