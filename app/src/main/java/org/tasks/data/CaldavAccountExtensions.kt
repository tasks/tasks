package org.tasks.data

import android.app.Activity
import android.content.Context
import org.tasks.R
import org.tasks.activities.GoogleTaskListSettingsActivity
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.caldav.CaldavCalendarSettingsActivity
import org.tasks.caldav.LocalAccountSettingsActivity
import org.tasks.caldav.LocalListSettingsActivity
import org.tasks.data.OpenTaskDao.Companion.isDavx5
import org.tasks.data.OpenTaskDao.Companion.isDavx5Managed
import org.tasks.data.OpenTaskDao.Companion.isDecSync
import org.tasks.data.OpenTaskDao.Companion.isEteSync
import org.tasks.data.entity.CaldavAccount
import org.tasks.etebase.EtebaseAccountSettingsActivity
import org.tasks.etebase.EtebaseCalendarSettingsActivity
import org.tasks.opentasks.OpenTaskAccountSettingsActivity
import org.tasks.opentasks.OpenTasksListSettingsActivity
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.microsoft.MicrosoftListSettingsActivity
import org.tasks.todoist.TodoistAccountSettingsActivity
import org.tasks.todoist.TodoistCalendarSettingsActivity

val CaldavAccount.prefTitle: Int
    get() = when {
        isTasksOrg -> R.string.tasks_org
        isCaldavAccount -> R.string.caldav
        isEtebaseAccount || uuid.isEteSync() -> R.string.etesync
        isTodoistAccount -> R.string.todoist
        uuid.isDavx5() || uuid.isDavx5Managed() -> R.string.davx5
        uuid.isDecSync() -> R.string.decsync
        isMicrosoft -> R.string.microsoft
        isGoogleTasks -> R.string.gtasks_GPr_header
        isLocalList -> R.string.local_lists
        else -> 0
    }

val CaldavAccount.prefIcon: Int
    get() = when {
        isTasksOrg -> R.drawable.ic_round_icon
        isCaldavAccount -> R.drawable.ic_webdav_logo
        isEtebaseAccount || uuid.isEteSync() -> R.drawable.ic_etesync
        isTodoistAccount -> R.drawable.ic_todoist
        uuid.isDavx5() -> R.drawable.ic_davx5_icon_green_bg
        uuid.isDavx5Managed() -> R.drawable.ic_davx5_icon_blue_bg
        uuid.isDecSync() -> R.drawable.ic_decsync
        isMicrosoft -> R.drawable.ic_microsoft_tasks
        isGoogleTasks -> R.drawable.ic_google
        isLocalList -> R.drawable.ic_outline_cloud_off_24px
        else -> 0
    }

fun CaldavAccount.isTasksSubscription(context: Context): Boolean {
    val caldavUrl = context.getString(R.string.tasks_caldav_url)
    return url?.startsWith("${caldavUrl}/calendars/") == true &&
            !isPaymentRequired() &&
            !isLoggedOut()
}

fun CaldavAccount.listSettingsClass(): Class<out Activity> = when(accountType) {
    CaldavAccount.TYPE_LOCAL -> LocalListSettingsActivity::class.java
    CaldavAccount.TYPE_OPENTASKS -> OpenTasksListSettingsActivity::class.java
    CaldavAccount.TYPE_ETEBASE -> EtebaseCalendarSettingsActivity::class.java
    CaldavAccount.TYPE_TODOIST -> TodoistCalendarSettingsActivity::class.java
    CaldavAccount.TYPE_MICROSOFT -> MicrosoftListSettingsActivity::class.java
    CaldavAccount.TYPE_GOOGLE_TASKS -> GoogleTaskListSettingsActivity::class.java
    else -> CaldavCalendarSettingsActivity::class.java
}

val CaldavAccount.accountSettingsClass: Class<out BaseCaldavAccountSettingsActivity>
    get() = when {
        isCaldavAccount -> CaldavAccountSettingsActivity::class.java
        isEtebaseAccount -> EtebaseAccountSettingsActivity::class.java
        isTodoistAccount -> TodoistAccountSettingsActivity::class.java
        isOpenTasks -> OpenTaskAccountSettingsActivity::class.java
        isLocalList -> LocalAccountSettingsActivity::class.java
        else -> throw IllegalArgumentException("Unexpected account type: $this")
    }

fun CaldavAccount.getPassword(encryption: KeyStoreEncryption): String {
    return encryption.decrypt(password) ?: ""
}

val CaldavAccount.isTodoistAccount: Boolean
    get() = accountType == CaldavAccount.TYPE_TODOIST
