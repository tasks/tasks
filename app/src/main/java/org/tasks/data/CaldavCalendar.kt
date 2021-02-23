package org.tasks.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.api.FilterListItem.NO_ORDER
import com.todoroo.astrid.data.Task
import org.tasks.themes.CustomIcons.LIST

@Entity(tableName = "caldav_lists")
class CaldavCalendar : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "cdl_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "cdl_account")
    var account: String? = Task.NO_UUID

    @ColumnInfo(name = "cdl_uuid")
    var uuid: String? = Task.NO_UUID

    @ColumnInfo(name = "cdl_name")
    var name: String? = ""

    @ColumnInfo(name = "cdl_color")
    var color = 0

    @ColumnInfo(name = "cdl_ctag")
    var ctag: String? = null

    @ColumnInfo(name = "cdl_url")
    var url: String? = ""

    @ColumnInfo(name = "cdl_icon")
    private var icon: Int? = -1

    @ColumnInfo(name = "cdl_order")
    var order = NO_ORDER

    @ColumnInfo(name = "cdl_access")
    var access = 0

    constructor()

    @Ignore
    constructor(name: String?, uuid: String?) {
        this.name = name
        this.uuid = uuid
    }

    @Ignore
    constructor(source: Parcel) {
        id = source.readLong()
        account = source.readString()
        uuid = source.readString()
        name = source.readString()
        color = source.readInt()
        ctag = source.readString()
        url = source.readString()
        icon = source.readInt()
        order = source.readInt()
        access = source.readInt()
    }

    fun getIcon(): Int? {
        return (if (icon == null) LIST else icon!!)
    }

    fun setIcon(icon: Int?) {
        this.icon = icon
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        with(dest) {
            writeLong(id)
            writeString(account)
            writeString(uuid)
            writeString(name)
            writeInt(color)
            writeString(ctag)
            writeString(url)
            writeInt(getIcon()!!)
            writeInt(order)
            writeInt(access)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CaldavCalendar) return false

        if (id != other.id) return false
        if (account != other.account) return false
        if (uuid != other.uuid) return false
        if (name != other.name) return false
        if (color != other.color) return false
        if (ctag != other.ctag) return false
        if (url != other.url) return false
        if (icon != other.icon) return false
        if (order != other.order) return false
        if (access != other.access) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (account?.hashCode() ?: 0)
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + color
        result = 31 * result + (ctag?.hashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (icon ?: 0)
        result = 31 * result + order
        result = 31 * result + access
        return result
    }

    override fun toString(): String {
        return "CaldavCalendar(id=$id, account=$account, uuid=$uuid, name=$name, color=$color, ctag=$ctag, url=$url, icon=$icon, order=$order, access=$access)"
    }

    companion object {
        const val ACCESS_OWNER = 0
        const val ACCESS_READ_WRITE = 1
        const val ACCESS_READ_ONLY = 2

        @JvmField val TABLE = Table("caldav_lists")
        val ACCOUNT = TABLE.column("cdl_account")
        @JvmField val UUID = TABLE.column("cdl_uuid")
        @JvmField val NAME = TABLE.column("cdl_name")
        @JvmField val CREATOR: Parcelable.Creator<CaldavCalendar> = object : Parcelable.Creator<CaldavCalendar> {
            override fun createFromParcel(source: Parcel): CaldavCalendar = CaldavCalendar(source)

            override fun newArray(size: Int): Array<CaldavCalendar?> = arrayOfNulls(size)
        }
    }
}