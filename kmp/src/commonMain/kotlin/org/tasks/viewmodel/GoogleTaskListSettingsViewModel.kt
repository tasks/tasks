package org.tasks.viewmodel

import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.googleapis.GoogleTaskListClient
import org.tasks.service.TaskDeleter
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.gtasks_GLA_errorIOAuth

open class GoogleTaskListSettingsViewModel(
    caldavDao: CaldavDao,
    taskDeleter: TaskDeleter,
    reporting: Reporting,
    private val invokerFactory: suspend (CaldavAccount) -> GoogleTaskListClient,
    purchaseState: PurchaseState,
    isDark: Boolean,
    account: CaldavAccount,
    calendar: CaldavCalendar,
    hasColorWheel: Boolean = false,
    stateManager: ListSettingsStateManager =
        ListSettingsStateManager(isDark, purchaseState, account, calendar, hasColorWheel),
) : ListSettingsViewModel(
    caldavDao = caldavDao,
    taskDeleter = taskDeleter,
    reporting = reporting,
    purchaseState = purchaseState,
    isDark = isDark,
    account = account,
    calendar = calendar,
    hasColorWheel = hasColorWheel,
    stateManager = stateManager,
) {
    override suspend fun createRemoteList(
        account: CaldavAccount,
        name: String,
        color: Int,
        icon: String,
    ): CaldavCalendar {
        val taskList = invokerFactory(account).createGtaskList(name)
        return CaldavCalendar(
            uuid = taskList.id,
            account = account.username,
            name = taskList.title,
            color = color,
            icon = icon,
        )
    }

    override suspend fun renameRemoteList(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        name: String,
    ) {
        invokerFactory(account).renameGtaskList(calendar.uuid!!, name)
    }

    override suspend fun deleteRemoteList(account: CaldavAccount, calendar: CaldavCalendar) {
        invokerFactory(account).deleteGtaskList(calendar.uuid!!)
    }

    override suspend fun handleError(e: Exception) {
        Logger.e(e) { "Google Tasks list operation failed" }
        val message = getString(Res.string.gtasks_GLA_errorIOAuth)
        stateManager.update { it.copy(snackbar = message) }
    }
}
