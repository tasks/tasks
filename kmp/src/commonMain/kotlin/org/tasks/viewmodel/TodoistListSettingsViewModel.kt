package org.tasks.viewmodel

import co.touchlab.kermit.Logger
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.tasks.http.NetworkException
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

class ListSettingsDeps(
    val caldavDao: CaldavDao,
    val taskDeleter: TaskDeleter,
    val reporting: Reporting,
    val purchaseState: PurchaseState,
)

class ListSettingsTarget(
    val account: CaldavAccount,
    val calendar: CaldavCalendar,
    val isDark: Boolean,
    val hasColorWheel: Boolean = false,
)

open class TodoistListSettingsViewModel(
    deps: ListSettingsDeps,
    target: ListSettingsTarget,
    private val clientProvider: TodoistClientProvider,
) : ListSettingsViewModel(
    caldavDao = deps.caldavDao,
    taskDeleter = deps.taskDeleter,
    reporting = deps.reporting,
    purchaseState = deps.purchaseState,
    isDark = target.isDark,
    account = target.account,
    calendar = target.calendar,
    hasColorWheel = target.hasColorWheel,
    stateManager = ListSettingsStateManager(
        target.isDark,
        deps.purchaseState,
        target.account,
        target.calendar,
        target.hasColorWheel,
    ),
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
    } catch (e: NetworkException) {
        Logger.e(e) { "Failed to check Inbox project" }
        false
    } catch (e: IOException) {
        Logger.e(e) { "Failed to check Inbox project" }
        false
    } catch (e: SerializationException) {
        Logger.e(e) { "Failed to check Inbox project" }
        false
    }
}
