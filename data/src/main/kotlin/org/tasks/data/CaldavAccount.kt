package org.tasks.data

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.data.Task
import java.net.HttpURLConnection

@Entity(tableName = "caldav_accounts")
class CaldavAccount : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "cda_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "cda_uuid")
    var uuid: String? = Task.NO_UUID

    @ColumnInfo(name = "cda_name")
    var name: String? = ""

    @ColumnInfo(name = "cda_url")
    var url: String? = ""

    @ColumnInfo(name = "cda_username")
    var username: String? = ""

    @ColumnInfo(name = "cda_password")
    @Transient
    var password: String? = ""

    @ColumnInfo(name = "cda_error")
    @Transient
    var error: String? = ""

    @ColumnInfo(name = "cda_account_type")
    var accountType = TYPE_CALDAV

    @ColumnInfo(name = "cda_collapsed")
    var isCollapsed = false

    @ColumnInfo(name = "cda_server_type")
    var serverType = SERVER_UNKNOWN

    constructor()

    @Ignore
    constructor(source: Parcel) {
        id = source.readLong()
        uuid = source.readString()
        name = source.readString()
        url = source.readString()
        username = source.readString()
        password = source.readString()
        error = source.readString()
        accountType = source.readInt()
        isCollapsed = ParcelCompat.readBoolean(source)
        serverType = source.readInt()
    }

    val isCaldavAccount: Boolean
        get() = accountType == TYPE_CALDAV

    @Deprecated("use etebase")
    val isEteSyncAccount: Boolean
        get() = accountType == TYPE_ETESYNC

    val isEtebaseAccount: Boolean
        get() = accountType == TYPE_ETEBASE

    val isOpenTasks: Boolean
        get() = accountType == TYPE_OPENTASKS

    val isTasksOrg: Boolean
        get() = accountType == TYPE_TASKS

    val isMicrosoft: Boolean
        get() = accountType == TYPE_MICROSOFT

    val isGoogleTasks: Boolean
        get() = accountType == TYPE_GOOGLE_TASKS

    val isSuppressRepeatingTasks: Boolean
        get() = when (serverType) {
            SERVER_OPEN_XCHANGE,
            SERVER_MAILBOX_ORG -> true
            else -> false
        }

    val reminderSync: Boolean
        get() = serverType != SERVER_SYNOLOGY_CALENDAR

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        with(dest) {
            writeLong(id)
            writeString(uuid)
            writeString(name)
            writeString(url)
            writeString(username)
            writeString(password)
            writeString(error)
            writeInt(accountType)
            ParcelCompat.writeBoolean(this, isCollapsed)
            writeInt(serverType)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CaldavAccount

        if (id != other.id) return false
        if (uuid != other.uuid) return false
        if (name != other.name) return false
        if (url != other.url) return false
        if (username != other.username) return false
        if (password != other.password) return false
        if (error != other.error) return false
        if (accountType != other.accountType) return false
        if (isCollapsed != other.isCollapsed) return false
        if (serverType != other.serverType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (username?.hashCode() ?: 0)
        result = 31 * result + (password?.hashCode() ?: 0)
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + accountType
        result = 31 * result + isCollapsed.hashCode()
        result = 31 * result + serverType
        return result
    }

    override fun toString(): String {
        return "CaldavAccount(id=$id, uuid=$uuid, name=$name, url=$url, username=$username, password=<redacted>>, error=$error, accountType=$accountType, isCollapsed=$isCollapsed, serverType=$serverType)"
    }

    fun isLoggedOut() = error?.startsWith(ERROR_UNAUTHORIZED) == true

    fun isPaymentRequired() = error.isPaymentRequired()

    val hasError: Boolean
        get() = !error.isNullOrBlank()

    companion object {
        val TABLE = Table("caldav_accounts")
        val UUID = TABLE.column("cda_uuid")
        val ACCOUNT_TYPE = TABLE.column("cda_account_type")

        const val TYPE_CALDAV = 0
        @Deprecated("use etebase") const val TYPE_ETESYNC = 1
        const val TYPE_LOCAL = 2
        const val TYPE_OPENTASKS = 3
        const val TYPE_TASKS = 4
        const val TYPE_ETEBASE = 5
        const val TYPE_MICROSOFT = 6
        const val TYPE_GOOGLE_TASKS = 7

        const val SERVER_UNKNOWN = -1
        const val SERVER_TASKS = 0
        const val SERVER_OWNCLOUD = 1
        const val SERVER_SABREDAV = 2
        const val SERVER_OPEN_XCHANGE = 3
        const val SERVER_NEXTCLOUD = 4
        const val SERVER_SYNOLOGY_CALENDAR = 5
        const val SERVER_MAILBOX_ORG = 6
        const val SERVER_OTHER = 99

        const val ERROR_UNAUTHORIZED = "HTTP ${HttpURLConnection.HTTP_UNAUTHORIZED}"
        const val ERROR_PAYMENT_REQUIRED = "HTTP ${HttpURLConnection.HTTP_PAYMENT_REQUIRED}"

        fun String?.openTaskType(): String? = this?.split(":")?.get(0)

        fun String?.isPaymentRequired(): Boolean = this?.startsWith(ERROR_PAYMENT_REQUIRED) == true

        @JvmField val CREATOR: Parcelable.Creator<CaldavAccount> = object : Parcelable.Creator<CaldavAccount> {
            override fun createFromParcel(source: Parcel): CaldavAccount = CaldavAccount(source)

            override fun newArray(size: Int): Array<CaldavAccount?> = arrayOfNulls(size)
        }
    }
}