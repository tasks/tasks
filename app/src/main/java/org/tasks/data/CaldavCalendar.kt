package org.tasks.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.api.Filter.Companion.NO_ORDER
import com.todoroo.astrid.data.Task
import kotlinx.parcelize.Parcelize
import org.tasks.themes.CustomIcons.LIST

@Parcelize
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
    @Suppress("RedundantNullableReturnType")
    fun getIcon(): Int? {
        return (if (icon == null) LIST else icon!!)
    }

    fun setIcon(icon: Int?) {
        this.icon = icon
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
        @JvmField val NAME = TABLE.column("cdl_name")
        @JvmField val ORDER = TABLE.column("cdl_order")
    }
}