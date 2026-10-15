package org.tasks.viewmodel

import co.touchlab.kermit.Logger
import kotlinx.io.IOException
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.resources.getString
import org.tasks.service.TaskDeleter
import org.tasks.sync.todoist.TodoistClientProvider
import org.tasks.sync.todoist.TodoistColors
import org.tasks.sync.todoist.TodoistService
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.error_adding_account
import tasks.kmp.generated.resources.network_error

/**
 * Creates, renames and deletes Todoist projects.
 *
 * These go over the REST endpoints rather than sync commands because the UI needs the
 * created project back immediately.
 */
open class TodoistListSettingsViewModel(
    caldavDao: CaldavDao,
    taskDeleter: TaskDeleter,
    reporting: Reporting,
    private val clientProvider: TodoistClientProvider,
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
        val project = clientProvider
            .getService(account)
            .createProject(name = name, color = TodoistColors.toName(color))
        return CaldavCalendar(
            account = account.uuid,
            color = color,
            icon = icon,
        ).apply {
            project.applyTo(this)
            // Todoist snaps the color onto its own palette; keep what the user picked
            // so the list looks the way they set it locally.
            this.color = color
        }
    }

    override suspend fun renameRemoteList(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        name: String,
    ) {
        val service = clientProvider.getService(account)
        if (isInbox(service, calendar.uuid!!)) {
            throw IllegalStateException("The Inbox cannot be renamed")
        }
        service.updateProject(id = calendar.uuid!!, name = name)
    }

    override suspend fun deleteRemoteList(account: CaldavAccount, calendar: CaldavCalendar) {
        val service = clientProvider.getService(account)
        if (isInbox(service, calendar.uuid!!)) {
            throw IllegalStateException("The Inbox cannot be deleted")
        }
        service.deleteProject(calendar.uuid!!)
    }

    override suspend fun handleError(e: Exception) {
        Logger.e(e) { "Todoist project operation failed" }
        val message = when (e) {
            is IOException -> getString(Res.string.network_error)
            else -> getString(Res.string.error_adding_account, e.message ?: e::class.simpleName ?: "")
        }
        stateManager.update { it.copy(snackbar = message) }
    }

    private suspend fun isInbox(service: TodoistService, uuid: String): Boolean = try {
        service.getProject(uuid).isInboxProject
    } catch (e: Exception) {
        Logger.e(e) { "Failed to check Inbox project" }
        false
    }
}
