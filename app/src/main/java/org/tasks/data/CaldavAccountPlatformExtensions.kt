package org.tasks.data

import android.app.Activity
import android.content.Context
import org.tasks.activities.GoogleTaskListSettingsActivity
import org.tasks.caldav.CaldavCalendarSettingsActivity
import org.tasks.caldav.LocalListSettingsActivity
import org.tasks.data.entity.CaldavAccount
import org.tasks.etebase.EtebaseCalendarSettingsActivity
import org.tasks.opentasks.OpenTasksListSettingsActivity
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.microsoft.MicrosoftListSettingsActivity

fun CaldavAccount.isTasksSubscription(context: Context): Boolean {
    return isTasksOrg && !isPaymentRequired() && !isLoggedOut()
}

fun CaldavAccount.listSettingsClass(): Class<out Activity> = when(accountType) {
    CaldavAccount.TYPE_LOCAL -> LocalListSettingsActivity::class.java
    CaldavAccount.TYPE_OPENTASKS -> OpenTasksListSettingsActivity::class.java
    CaldavAccount.TYPE_ETEBASE -> EtebaseCalendarSettingsActivity::class.java
    CaldavAccount.TYPE_MICROSOFT -> MicrosoftListSettingsActivity::class.java
    CaldavAccount.TYPE_GOOGLE_TASKS -> GoogleTaskListSettingsActivity::class.java
    else -> CaldavCalendarSettingsActivity::class.java
}

fun CaldavAccount.getPassword(encryption: KeyStoreEncryption): String {
    return encryption.decrypt(password) ?: ""
}
