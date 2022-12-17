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
data class CaldavCalendar(
    @PrimaryKey(autoGenerate = true)
    @Transient
    @ColumnInfo(name = "cdl_id") var id: Long = 0,
    @ColumnInfo(name = "cdl_account") val account: String? = Task.NO_UUID,
    @ColumnInfo(name = "cdl_uuid") var uuid: String? = Task.NO_UUID,
    @ColumnInfo(name = "cdl_name") var name: String? = "",
    @ColumnInfo(name = "cdl_color") var color: Int = 0,
    @ColumnInfo(name = "cdl_ctag") var ctag: String? = null,
    @ColumnInfo(name = "cdl_url") var url: String? = "",
    @ColumnInfo(name = "cdl_icon") private var icon: Int? = -1,
    @ColumnInfo(name = "cdl_order") val order: Int = NO_ORDER,
    @ColumnInfo(name = "cdl_access") var access: Int = ACCESS_OWNER,
    @ColumnInfo(name = "cdl_last_sync") val lastSync: Long = 0,
) : Parcelable {
    @Ignore
    constructor(source: Parcel): this(
        id = source.readLong(),
        account = source.readString(),
        uuid = source.readString(),
        name = source.readString(),
        color = source.readInt(),
        ctag = source.readString(),
        url = source.readString(),
        icon = source.readInt(),
        order = source.readInt(),
        access = source.readInt(),
        lastSync = source.readLong(),
    )

    @Suppress("RedundantNullableReturnType")
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
            writeLong(lastSync)
        }
    }

    companion object {
        const val ACCESS_UNKNOWN = -1
        const val ACCESS_OWNER = 0
        const val ACCESS_READ_WRITE = 1
        const val ACCESS_READ_ONLY = 2

        const val INVITE_UNKNOWN = -1
        const val INVITE_ACCEPTED = 0
        const val INVITE_NO_RESPONSE = 1
        const val INVITE_DECLINED = 2
        const val INVITE_INVALID = 3

        val TABLE = Table("caldav_lists")
        val ACCOUNT = TABLE.column("cdl_account")
        val UUID = TABLE.column("cdl_uuid")
        val NAME = TABLE.column("cdl_name")
        @JvmField val CREATOR: Parcelable.Creator<CaldavCalendar> = object : Parcelable.Creator<CaldavCalendar> {
            override fun createFromParcel(source: Parcel): CaldavCalendar = CaldavCalendar(source)

            override fun newArray(size: Int): Array<CaldavCalendar?> = arrayOfNulls(size)
        }
    }
}