package org.tasks.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.api.FilterListItem.NO_ORDER
import org.tasks.themes.CustomIcons.LIST

@Entity(tableName = "google_task_lists")
class GoogleTaskList : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "gtl_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "gtl_account")
    var account: String? = null

    @ColumnInfo(name = "gtl_remote_id")
    var remoteId: String? = null

    @ColumnInfo(name = "gtl_title")
    var title: String? = null

    @ColumnInfo(name = "gtl_remote_order")
    var order = NO_ORDER

    @ColumnInfo(name = "gtl_last_sync")
    var lastSync: Long = 0

    @ColumnInfo(name = "gtl_color")
    private var color: Int? = null

    @ColumnInfo(name = "gtl_icon")
    private var icon: Int? = -1

    constructor()

    @Ignore
    constructor(parcel: Parcel) {
        id = parcel.readLong()
        account = parcel.readString()
        remoteId = parcel.readString()
        title = parcel.readString()
        order = parcel.readInt()
        lastSync = parcel.readLong()
        color = parcel.readInt()
        icon = parcel.readInt()
    }

    @Suppress("RedundantNullableReturnType")
    fun getColor(): Int? = color ?: 0

    fun setColor(color: Int?) {
        this.color = color
    }

    @Suppress("RedundantNullableReturnType")
    fun getIcon(): Int? = icon ?: LIST

    fun setIcon(icon: Int?) {
        this.icon = icon
    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, i: Int) {
        with(parcel) {
            writeLong(id)
            writeString(account)
            writeString(remoteId)
            writeString(title)
            writeInt(order)
            writeLong(lastSync)
            writeInt(getColor()!!)
            writeInt(getIcon()!!)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoogleTaskList) return false

        if (id != other.id) return false
        if (account != other.account) return false
        if (remoteId != other.remoteId) return false
        if (title != other.title) return false
        if (order != other.order) return false
        if (lastSync != other.lastSync) return false
        if (color != other.color) return false
        if (icon != other.icon) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (account?.hashCode() ?: 0)
        result = 31 * result + (remoteId?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + order
        result = 31 * result + lastSync.hashCode()
        result = 31 * result + (color ?: 0)
        result = 31 * result + (icon ?: 0)
        return result
    }

    override fun toString(): String =
            "GoogleTaskList(id=$id, account=$account, remoteId=$remoteId, title=$title, remoteOrder=$order, lastSync=$lastSync, color=$color, icon=$icon)"

    companion object {
        @JvmField val TABLE = Table("google_task_lists")
        val ACCOUNT = TABLE.column("gtl_account")
        @JvmField val REMOTE_ID = TABLE.column("gtl_remote_id")
        @JvmField val NAME = TABLE.column("gtl_title")
        @JvmField val CREATOR: Parcelable.Creator<GoogleTaskList> = object : Parcelable.Creator<GoogleTaskList> {
            override fun createFromParcel(parcel: Parcel): GoogleTaskList = GoogleTaskList(parcel)

            override fun newArray(size: Int): Array<GoogleTaskList?> = arrayOfNulls(size)
        }
    }
}