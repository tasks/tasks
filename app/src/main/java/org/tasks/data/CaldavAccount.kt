package org.tasks.data

import android.app.Activity
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.caldav.CaldavCalendarSettingsActivity
import org.tasks.caldav.LocalListSettingsActivity
import org.tasks.data.OpenTaskDao.Companion.isDavx5
import org.tasks.data.OpenTaskDao.Companion.isDecSync
import org.tasks.data.OpenTaskDao.Companion.isEteSync
import org.tasks.etebase.EtebaseAccountSettingsActivity
import org.tasks.etebase.EtebaseCalendarSettingsActivity
import org.tasks.etesync.EteSyncAccountSettingsActivity
import org.tasks.opentasks.OpenTaskAccountSettingsActivity
import org.tasks.opentasks.OpenTasksListSettingsActivity
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.microsoft.MicrosoftListSettingsActivity
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

    fun getPassword(encryption: KeyStoreEncryption): String {
        return encryption.decrypt(password) ?: ""
    }

    val isCaldavAccount: Boolean
        get() = accountType == TYPE_CALDAV

    @Deprecated("use etebase")
    val isEteSyncAccount: Boolean
        get() = accountType == TYPE_ETESYNC

    private val isEtebaseAccount: Boolean
        get() = accountType == TYPE_ETEBASE

    private val isOpenTasks: Boolean
        get() = accountType == TYPE_OPENTASKS

    val isTasksOrg: Boolean
        get() = accountType == TYPE_TASKS

    val isMicrosoft: Boolean
        get() = accountType == TYPE_MICROSOFT

    val isGoogleTasks: Boolean
        get() = accountType == TYPE_GOOGLE_TASKS

    fun listSettingsClass(): Class<out Activity> = when(accountType) {
        TYPE_LOCAL -> LocalListSettingsActivity::class.java
        TYPE_ETESYNC, TYPE_OPENTASKS -> OpenTasksListSettingsActivity::class.java
        TYPE_ETEBASE -> EtebaseCalendarSettingsActivity::class.java
        TYPE_MICROSOFT -> MicrosoftListSettingsActivity::class.java
        else -> CaldavCalendarSettingsActivity::class.java
    }

    val accountSettingsClass: Class<out BaseCaldavAccountSettingsActivity>
        get() = when {
            isCaldavAccount -> CaldavAccountSettingsActivity::class.java
            isEteSyncAccount -> EteSyncAccountSettingsActivity::class.java
            isEtebaseAccount -> EtebaseAccountSettingsActivity::class.java
            isOpenTasks -> OpenTaskAccountSettingsActivity::class.java
            else -> throw IllegalArgumentException("Unexpected account type: $this")
        }

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

    fun isTasksSubscription(context: Context): Boolean {
        val caldavUrl = context.getString(R.string.tasks_caldav_url)
        return url?.startsWith("${caldavUrl}/calendars/") == true &&
                !isPaymentRequired() &&
                !isLoggedOut()
    }

    fun isLoggedOut() = error?.startsWith(ERROR_UNAUTHORIZED) == true

    fun isPaymentRequired() = error.isPaymentRequired()

    val hasError: Boolean
        get() = !error.isNullOrBlank()

    val prefTitle: Int
        get() = when {
            isTasksOrg -> R.string.tasks_org
            isCaldavAccount -> R.string.caldav
            isEtebaseAccount || uuid.isEteSync() -> R.string.etesync
            isEteSyncAccount -> R.string.etesync_v1
            uuid.isDavx5() -> R.string.davx5
            uuid.isDecSync() -> R.string.decsync
            isMicrosoft -> R.string.microsoft
            isGoogleTasks -> R.string.gtasks_GPr_header
            else -> 0
        }

    val prefIcon: Int
        get() = when {
            isTasksOrg -> R.drawable.ic_round_icon
            isCaldavAccount -> R.drawable.ic_webdav_logo
            isEtebaseAccount || isEteSyncAccount || uuid.isEteSync() -> R.drawable.ic_etesync
            uuid.isDavx5() -> R.drawable.ic_davx5_icon_green_bg
            uuid.isDecSync() -> R.drawable.ic_decsync
            isMicrosoft -> R.drawable.ic_microsoft_tasks
            isGoogleTasks -> R.drawable.ic_google
            else -> 0
        }

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