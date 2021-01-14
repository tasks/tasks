package org.tasks.data

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
import org.tasks.activities.BaseListSettingsActivity
import org.tasks.caldav.CaldavCalendarSettingsActivity
import org.tasks.caldav.LocalListSettingsActivity
import org.tasks.etebase.EtebaseCalendarSettingsActivity
import org.tasks.etesync.EteSyncCalendarSettingsActivity
import org.tasks.opentasks.OpenTasksListSettingsActivity
import org.tasks.security.KeyStoreEncryption
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

    @ColumnInfo(name = "cda_repeat")
    var isSuppressRepeatingTasks = false

    @Deprecated("use etebase")
    @ColumnInfo(name = "cda_encryption_key")
    @Transient
    var encryptionKey: String? = null

    @ColumnInfo(name = "cda_account_type")
    var accountType = 0

    @ColumnInfo(name = "cda_collapsed")
    var isCollapsed = false

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
        isSuppressRepeatingTasks = ParcelCompat.readBoolean(source)
        accountType = source.readInt()
        encryptionKey = source.readString()
        isCollapsed = ParcelCompat.readBoolean(source)
    }

    fun getPassword(encryption: KeyStoreEncryption): String {
        return encryption.decrypt(password) ?: ""
    }

    @Deprecated("use etebase")
    fun getEncryptionPassword(encryption: KeyStoreEncryption): String {
        return encryption.decrypt(encryptionKey) ?: ""
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

    fun listSettingsClass(): Class<out BaseListSettingsActivity> = when(accountType) {
        TYPE_ETESYNC -> EteSyncCalendarSettingsActivity::class.java
        TYPE_LOCAL -> LocalListSettingsActivity::class.java
        TYPE_OPENTASKS -> OpenTasksListSettingsActivity::class.java
        TYPE_ETEBASE -> EtebaseCalendarSettingsActivity::class.java
        else -> CaldavCalendarSettingsActivity::class.java
    }

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
            ParcelCompat.writeBoolean(this, isSuppressRepeatingTasks)
            writeInt(accountType)
            writeString(encryptionKey)
            ParcelCompat.writeBoolean(this, isCollapsed)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CaldavAccount) return false

        if (id != other.id) return false
        if (uuid != other.uuid) return false
        if (name != other.name) return false
        if (url != other.url) return false
        if (username != other.username) return false
        if (password != other.password) return false
        if (error != other.error) return false
        if (isSuppressRepeatingTasks != other.isSuppressRepeatingTasks) return false
        if (encryptionKey != other.encryptionKey) return false
        if (accountType != other.accountType) return false
        if (isCollapsed != other.isCollapsed) return false

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
        result = 31 * result + isSuppressRepeatingTasks.hashCode()
        result = 31 * result + (encryptionKey?.hashCode() ?: 0)
        result = 31 * result + accountType
        result = 31 * result + isCollapsed.hashCode()
        return result
    }

    override fun toString(): String {
        return "CaldavAccount(id=$id, uuid=$uuid, name=$name, url=$url, username=$username, password=$password, error=$error, isSuppressRepeatingTasks=$isSuppressRepeatingTasks, encryptionKey=$encryptionKey, accountType=$accountType, isCollapsed=$isCollapsed)"
    }

    fun isTasksSubscription(context: Context): Boolean {
        val caldavUrl = context.getString(R.string.tasks_caldav_url)
        return url?.startsWith("${caldavUrl}/calendars/") == true &&
                !isPaymentRequired() &&
                !isLoggedOut()
    }

    fun isLoggedOut() = error?.startsWith(ERROR_UNAUTHORIZED) == true

    fun isPaymentRequired() = error?.startsWith(EROR_PAYMENT_REQUIRED) == true

    companion object {
        val TABLE = Table("caldav_accounts")
        val UUID = TABLE.column("cda_uuid")

        const val TYPE_CALDAV = 0
        @Deprecated("use etebase") const val TYPE_ETESYNC = 1
        const val TYPE_LOCAL = 2
        const val TYPE_OPENTASKS = 3
        const val TYPE_TASKS = 4
        const val TYPE_ETEBASE = 5

        const val ERROR_UNAUTHORIZED = "HTTP ${HttpURLConnection.HTTP_UNAUTHORIZED}"
        const val EROR_PAYMENT_REQUIRED = "HTTP ${HttpURLConnection.HTTP_PAYMENT_REQUIRED}"

        fun String?.openTaskType(): String? = this?.split(":")?.get(0)

        @JvmField val CREATOR: Parcelable.Creator<CaldavAccount> = object : Parcelable.Creator<CaldavAccount> {
            override fun createFromParcel(source: Parcel): CaldavAccount = CaldavAccount(source)

            override fun newArray(size: Int): Array<CaldavAccount?> = arrayOfNulls(size)
        }
    }
}