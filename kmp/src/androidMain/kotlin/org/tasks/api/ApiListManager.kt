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

class ApiListManager(
    private val caldavDao: CaldavDao,
    private val taskDeleter: TaskDeleter,
    private val caldavClientProvider: CaldavClientProvider,
    private val etebaseClientProvider: EtebaseClientProvider,
    private val microsoftClientProvider: MicrosoftClientProvider,
    private val gtasksInvoker: suspend (CaldavAccount) -> GtasksInvoker,
) {
    private val remoteCalls = Semaphore(1)

    suspend fun create(
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
                        ?: throw IllegalStateException("Google Tasks did not return a list")
                    CaldavCalendar(
                        uuid = list.id,
                        account = account.username,
                        name = list.title,
                        color = color,
                        icon = icon,
                    )
                }
                account.isMicrosoft -> {
                    val list = microsoftClientProvider
                        .getService(account)
                        .createList(TaskLists.TaskList(displayName = title))
                    CaldavCalendar(account = account.uuid, color = color, icon = icon)
                        .apply { list.applyTo(this) }
                }
                account.isEtebaseAccount -> CaldavCalendar(
                    uuid = UUIDHelper.newUUID(),
                    account = account.uuid,
                    url = etebaseClientProvider.forAccount(account).makeCollection(title, color),
                    name = title,
                    color = color,
                    icon = icon,
                )
                else -> CaldavCalendar(
                    uuid = UUIDHelper.newUUID(),
                    account = account.uuid,
                    url = caldavClientProvider.forAccount(account).makeCollection(title, color, icon),
                    name = title,
                    color = color,
                    icon = icon,
                )
            }
        }
        caldavDao.insert(calendar)
        caldavDao.getCalendarByUuid(calendar.uuid!!) ?: calendar
    }

    suspend fun update(
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
                            gtasksInvoker(account).renameGtaskList(calendar.uuid, title)
                        }
                    account.isMicrosoft ->
                        if (renamed) {
                            microsoftClientProvider
                                .getService(account)
                                .updateList(calendar.uuid!!, TaskLists.TaskList(displayName = title))
                        }
                    account.isEtebaseAccount ->
                        if (renamed || recolored) {
                            etebaseClientProvider.forAccount(account)
                                .updateCollection(calendar, title, color)
                        }
                    else ->
                        caldavClientProvider.forAccount(account, calendar.url!!)
                            .updateCollection(title, color, icon)
                }
            }
        }
        val updated = calendar.copy(name = title, color = color, icon = icon)
        caldavDao.update(updated)
        updated
    }

    suspend fun delete(account: CaldavAccount, calendar: CaldavCalendar) = withContext(NonCancellable) {
        remote("delete") {
            when {
                account.isLocalList -> Unit
                account.isGoogleTasks -> gtasksInvoker(account).deleteGtaskList(calendar.uuid)
                account.isMicrosoft ->
                    microsoftClientProvider.getService(account).deleteList(calendar.uuid!!)
                account.isEtebaseAccount ->
                    etebaseClientProvider.forAccount(account).deleteCollection(calendar)
                else ->
                    caldavClientProvider.forAccount(account, calendar.url!!).deleteCollection()
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
