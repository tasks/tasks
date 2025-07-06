package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.tasks.CommonParcelable
import org.tasks.CommonParcelize
import org.tasks.data.Redacted
import org.tasks.data.db.Table
import java.net.HttpURLConnection

@Serializable
@CommonParcelize
@Entity(tableName = "caldav_accounts")
data class CaldavAccount(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "cda_id")
    @Transient
    val id: Long = 0,
    @ColumnInfo(name = "cda_uuid")
    val uuid: String? = Task.NO_UUID,
    @Redacted
    @ColumnInfo(name = "cda_name")
    var name: String? = "",
    @Redacted
    @ColumnInfo(name = "cda_url")
    var url: String? = "",
    @Redacted
    @ColumnInfo(name = "cda_username")
    var username: String? = "",
    @Redacted
    @ColumnInfo(name = "cda_password")
    @Transient
    var password: String? = "",
    @ColumnInfo(name = "cda_error")
    @Transient
    var error: String? = "",
    @ColumnInfo(name = "cda_account_type")
    val accountType: Int = TYPE_CALDAV,
    @ColumnInfo(name = "cda_collapsed")
    val isCollapsed: Boolean = false,
    @ColumnInfo(name = "cda_server_type")
    var serverType: Int = SERVER_UNKNOWN,
) : CommonParcelable {
    val isCaldavAccount: Boolean
        get() = accountType == TYPE_CALDAV

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

    val isLocalList: Boolean
        get() = accountType == TYPE_LOCAL

    val isSuppressRepeatingTasks: Boolean
        get() = when (serverType) {
            SERVER_OPEN_XCHANGE,
            SERVER_MAILBOX_ORG -> true
            else -> false
        }

    val reminderSync: Boolean
        get() = serverType != SERVER_SYNOLOGY_CALENDAR

    fun isLoggedOut() = error?.startsWith(ERROR_UNAUTHORIZED) == true

    fun isPaymentRequired() = error.isPaymentRequired()

    val hasError: Boolean
        get() = !error.isNullOrBlank()

    companion object {
        val TABLE = Table("caldav_accounts")
        val UUID = TABLE.column("cda_uuid")
        val ACCOUNT_TYPE = TABLE.column("cda_account_type")

        const val TYPE_CALDAV = 0
//        const val TYPE_ETESYNC = 1
        const val TYPE_LOCAL = 2
        const val TYPE_OPENTASKS = 3
        const val TYPE_TASKS = 4
        const val TYPE_ETEBASE = 5
        const val TYPE_MICROSOFT = 6
        const val TYPE_GOOGLE_TASKS = 7
        const val TYPE_TODOIST = 8

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
    }
}