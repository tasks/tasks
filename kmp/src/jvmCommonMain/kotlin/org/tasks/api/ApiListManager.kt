package org.tasks.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.etebase.EtebaseClientProvider
import org.tasks.googleapis.GtasksInvoker
import org.tasks.service.TaskDeleter
import org.tasks.sync.microsoft.MicrosoftClientProvider
import org.tasks.sync.microsoft.TaskLists
import org.tasks.sync.todoist.TodoistClientProvider
import org.tasks.sync.todoist.TodoistColors

class SyncClients(
    val caldav: CaldavClientProvider,
    val etebase: EtebaseClientProvider,
    val microsoft: MicrosoftClientProvider,
    val todoist: TodoistClientProvider,
)

class ApiListManager(
    private val caldavDao: CaldavDao,
    private val taskDeleter: TaskDeleter,
    private val clients: SyncClients,
    private val gtasksInvoker: suspend (CaldavAccount) -> GtasksInvoker,
) : ListManager {
    private val remoteCalls = Semaphore(1)

    override suspend fun create(
        account: CaldavAccount,
        title: String,
        color: Int,
        icon: String,
    ): CaldavCalendar = withContext(NonCancellable) {
        val calendar = remote("create") {
            when {
                account.isLocalList -> CaldavCalendar(
                    uuid = UUIDHelper.newUUID(),
                    account = account.uuid,
                    name = title,
                    color = color,
                    icon = icon,
                )
                account.isGoogleTasks -> {
                    val list = gtasksInvoker(account).createGtaskList(title)
                    CaldavCalendar(
                        uuid = list.id,
                        account = account.username,
                        name = list.title,
                        color = color,
                        icon = icon,
                    )
                }
                account.isMicrosoft -> {
                    val list = clients.microsoft
                        .getService(account)
                        .createList(TaskLists.TaskList(displayName = title))
                    CaldavCalendar(account = account.uuid, color = color, icon = icon)
                        .apply { list.applyTo(this) }
                }
                account.isTodoist -> {
                    val project = clients.todoist
                        .getService(account)
                        .createProject(name = title, color = TodoistColors.toName(color))
                    CaldavCalendar(account = account.uuid, color = color, icon = icon)
                        .apply {
                            project.applyTo(this)
                            // Todoist snaps the color onto its own palette; keep the
                            // exact color the caller asked for locally.
                            this.color = color
                        }
                }
                account.isEtebaseAccount -> CaldavCalendar(
                    uuid = UUIDHelper.newUUID(),
                    account = account.uuid,
                    url = clients.etebase.forAccount(account).makeCollection(title, color),
                    name = title,
                    color = color,
                    icon = icon,
                )
                else -> CaldavCalendar(
                    uuid = UUIDHelper.newUUID(),
                    account = account.uuid,
                    url = clients.caldav.forAccount(account).use { it.makeCollection(title, color, icon) },
                    name = title,
                    color = color,
                    icon = icon,
                )
            }
        }
        caldavDao.insert(calendar)
        caldavDao.getCalendarByUuid(calendar.uuid!!) ?: calendar
    }

    override suspend fun update(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        title: String,
        color: Int,
        icon: String,
    ): CaldavCalendar = withContext(NonCancellable) {
        val renamed = title != calendar.name
        val recolored = color != calendar.color
        val reiconed = icon != calendar.icon.orEmpty()
        if (renamed || recolored || reiconed) {
            remote("update") {
                when {
                    account.isLocalList -> Unit
                    account.isGoogleTasks ->
                        if (renamed) {
                            gtasksInvoker(account).renameGtaskList(calendar.uuid!!, title)
                        }
                    account.isMicrosoft ->
                        if (renamed) {
                            clients.microsoft
                                .getService(account)
                                .updateList(calendar.uuid!!, TaskLists.TaskList(displayName = title))
                        }
                    account.isTodoist ->
                        if (renamed || recolored) {
                            clients.todoist
                                .getService(account)
                                .updateProject(
                                    id = calendar.uuid!!,
                                    name = title.takeIf { renamed },
                                    color = TodoistColors.toName(color).takeIf { recolored },
                                )
                        }
                    account.isEtebaseAccount ->
                        if (renamed || recolored) {
                            clients.etebase.forAccount(account)
                                .updateCollection(calendar, title, color)
                        }
                    else ->
                        clients.caldav.forAccount(account, calendar.url!!)
                            .use { it.updateCollection(title, color, icon) }
                }
            }
        }
        val updated = calendar.copy(name = title, color = color, icon = icon)
        caldavDao.update(updated)
        updated
    }

    override suspend fun delete(account: CaldavAccount, calendar: CaldavCalendar) = withContext(NonCancellable) {
        remote("delete") {
            when {
                account.isLocalList -> Unit
                account.isGoogleTasks -> gtasksInvoker(account).deleteGtaskList(calendar.uuid!!)
                account.isMicrosoft ->
                    clients.microsoft.getService(account).deleteList(calendar.uuid!!)
                account.isTodoist ->
                    clients.todoist.getService(account).deleteProject(calendar.uuid!!)
                account.isEtebaseAccount ->
                    clients.etebase.forAccount(account).deleteCollection(calendar)
                else ->
                    clients.caldav.forAccount(account, calendar.url!!).use { it.deleteCollection() }
            }
        }
        taskDeleter.delete(calendar)
    }

    private suspend fun <T> remote(operation: String, block: suspend () -> T): T = try {
        withTimeout(QUEUE_TIMEOUT_MS) {
            remoteCalls.withPermit {
                withContext(Dispatchers.IO) {
                    withTimeout(CALL_TIMEOUT_MS) { block() }
                }
            }
        }
    } catch (e: TimeoutCancellationException) {
        throw IllegalStateException("Timed out waiting for the server to $operation the list", e)
    } catch (e: IllegalStateException) {
        throw e
    } catch (e: UnsupportedOperationException) {
        throw e
    } catch (e: Exception) {
        throw IllegalStateException(e.message ?: "Failed to $operation list", e)
    }

    companion object {
        private const val CALL_TIMEOUT_MS = 30_000L

        private const val QUEUE_TIMEOUT_MS = 60_000L
    }
}
