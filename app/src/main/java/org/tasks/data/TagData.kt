package org.tasks.data

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.astrid.api.Filter.Companion.NO_ORDER
import com.todoroo.astrid.data.Task
import org.tasks.themes.CustomIcons.LABEL

@Entity(tableName = "tagdata")
class TagData : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    var id: Long? = null

    @ColumnInfo(name = "remoteId")
    var remoteId: String? = Task.NO_UUID

    @ColumnInfo(name = "name")
    var name: String? = ""

    @ColumnInfo(name = "color")
    private var color: Int? = 0

    @ColumnInfo(name = "tagOrdering")
    var tagOrdering: String? = "[]"

    @ColumnInfo(name = "td_icon")
    private var icon: Int? = -1

    @ColumnInfo(name = "td_order")
    var order = NO_ORDER

    @Ignore
    constructor(name: String?) {
        this.name = name
    }

    constructor()

    @SuppressLint("ParcelClassLoader")
    @Ignore
    private constructor(parcel: Parcel) {
        with(parcel) {
            id = readValue(null) as Long?
            remoteId = readString()
            name = readString()
            color = readInt()
            tagOrdering = readString()
            icon = readInt()
            order = readInt()
        }
    }

    @Suppress("RedundantNullableReturnType")
    fun getColor(): Int? = color ?: 0

    fun setColor(color: Int?) {
        this.color = color
    }

    @Suppress("RedundantNullableReturnType")
    fun getIcon(): Int? = icon ?: LABEL

    fun setIcon(icon: Int?) {
        this.icon = icon
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        with(dest) {
            writeValue(id)
            writeString(remoteId)
            writeString(name)
            writeInt(color!!)
            writeString(tagOrdering)
            writeInt(getIcon()!!)
            writeInt(order)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TagData) return false

        if (id != other.id) return false
        if (remoteId != other.remoteId) return false
        if (name != other.name) return false
        if (color != other.color) return false
        if (tagOrdering != other.tagOrdering) return false
        if (icon != other.icon) return false
        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (remoteId?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (color ?: 0)
        result = 31 * result + (tagOrdering?.hashCode() ?: 0)
        result = 31 * result + (icon ?: 0)
        result = 31 * result + order
        return result
    }

    override fun toString(): String =
            "TagData(id=$id, remoteId=$remoteId, name=$name, color=$color, tagOrdering=$tagOrdering, icon=$icon, order=$order)"

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<TagData> = object : Parcelable.Creator<TagData> {
            override fun createFromParcel(source: Parcel): TagData = TagData(source)

            override fun newArray(size: Int): Array<TagData?> = arrayOfNulls(size)
        }
    }
}