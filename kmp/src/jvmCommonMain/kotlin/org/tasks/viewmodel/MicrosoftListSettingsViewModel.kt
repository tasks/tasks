package org.tasks.viewmodel

import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.service.TaskDeleter
import org.tasks.sync.microsoft.MicrosoftClientProvider
import org.tasks.sync.microsoft.MicrosoftService
import org.tasks.sync.microsoft.TaskLists
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.error_adding_account
import tasks.kmp.generated.resources.network_error
import java.io.IOException

open class MicrosoftListSettingsViewModel(
    caldavDao: CaldavDao,
    taskDeleter: TaskDeleter,
    reporting: Reporting,
    private val clientProvider: MicrosoftClientProvider,
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
        val taskList = clientProvider.getService(account)
            .createList(TaskLists.TaskList(displayName = name))
        return CaldavCalendar(
            account = account.uuid,
            color = color,
            icon = icon,
        ).apply {
            taskList.applyTo(this)
        }
    }

    override suspend fun renameRemoteList(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        name: String,
    ) {
        val service = clientProvider.getService(account)
        if (isDefault(service, calendar.uuid!!)) {
            throw IllegalStateException("The default list cannot be renamed")
        }
        service.updateList(calendar.uuid!!, TaskLists.TaskList(displayName = name))
    }

    override suspend fun deleteRemoteList(account: CaldavAccount, calendar: CaldavCalendar) {
        val service = clientProvider.getService(account)
        if (isDefault(service, calendar.uuid!!)) {
            throw IllegalStateException("The default list cannot be deleted")
        }
        service.deleteList(calendar.uuid!!)
    }

    override suspend fun handleError(e: Exception) {
        Logger.e(e) { "Microsoft list operation failed" }
        val message = when (e) {
            is IOException -> getString(Res.string.network_error)
            else -> getString(Res.string.error_adding_account, e.message ?: e::class.simpleName ?: "")
        }
        stateManager.update { it.copy(snackbar = message) }
    }

    private suspend fun isDefault(service: MicrosoftService, uuid: String): Boolean = try {
        service.getList(uuid).isDefaultList
    } catch (e: Exception) {
        Logger.e(e) { "Failed to check default list" }
        false
    }
}
