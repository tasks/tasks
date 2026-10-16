package org.tasks.sync.todoist
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.tasks.analytics.AnalyticsEvents.INITIAL_SYNC_COMPLETE
import org.tasks.analytics.AnalyticsEvents.PARAM_TASK_COUNT
import org.tasks.analytics.AnalyticsEvents.PARAM_TYPE
import org.tasks.analytics.Constants
import org.tasks.analytics.Reporting
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.VtodoCache
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskToPush
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.http.HttpException
import org.tasks.http.NotFoundException
import org.tasks.http.ServiceUnavailableException
import org.tasks.http.UnauthorizedException
import org.tasks.resources.getString
import org.tasks.service.TaskDeleter
import org.tasks.sync.todoist.TodoistConverter.applyRemote
import org.tasks.sync.todoist.TodoistConverter.toArgs
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cannot_access_account
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLException

class TodoistPushResults(
    private val stores: TodoistSyncStores,
    private val services: TodoistSyncServices,
    private val hooks: TodoistSyncHooks,
) {
    suspend fun handlePushResult(
        chunk: List<Pending>,
        response: TodoistSyncResponse,
    ): String? {
        var firstError: String? = null
        for (item in chunk) {
            when (val result = response.statusOf(item.command.uuid)) {
                TodoistSyncResponse.CommandResult.Ok -> onPushSucceeded(item, response)
                TodoistSyncResponse.CommandResult.Missing -> {
                    Logger.w(TAG) { "No sync_status for ${item.command.type} ${item.command.uuid}" }
                }
                is TodoistSyncResponse.CommandResult.Error ->
                    firstError = onPushFailed(item, result) ?: firstError
            }
        }
        return firstError
    }

    private suspend fun onPushFailed(
        item: Pending,
        result: TodoistSyncResponse.CommandResult.Error,
    ): String? {
        if (result.isNotFound) {
            Logger.w(TAG) {
                "${item.command.type} for taskId=${item.taskId} gone remotely: ${result.message}"
            }
            services.taskDeleter.delete(stores.taskDao.getChildren(item.taskId) + item.taskId)
            return null
        }
        Logger.e(TAG) {
            "${item.command.type} failed for taskId=${item.taskId}: ${result.message}"
        }
        services.reporting.reportException(
            IllegalStateException("Todoist ${item.command.type}: ${result.message}")
        )
        return "${item.command.type}: ${result.message}"
    }

    private suspend fun onPushSucceeded(item: Pending, response: TodoistSyncResponse) {
        if (item.deletesLocally) {
            services.taskDeleter.delete(stores.taskDao.getChildren(item.taskId) + item.taskId)
            return
        }
        val caldavTask = stores.caldavDao.getCaldavTaskById(item.caldavTaskId)
        if (caldavTask == null) {
            return
        }
        item.tempId?.let { tempId ->
            val assigned = response.tempIdMapping[tempId]
            if (assigned == null) {
                Logger.e(TAG) { "No id returned for temp_id $tempId on taskId=${item.taskId}" }
                services.reporting.reportException(
                    IllegalStateException("Todoist item_add returned no temp_id mapping")
                )
                return
            }
            caldavTask.remoteId = assigned
            caldavTask.obj = "$assigned.json"
        }
        val command = item.command
        if (command.type == TodoistCommand.ITEM_MOVE) {
            caldavTask.remoteParent =
                command.args["parent_id"]?.let { it.toString().trim('"') } ?: ""
        }
        if (item.dropsJoinRow) {
            stores.caldavDao.delete(caldavTask)
            return
        }
        stores.caldavDao.update(caldavTask)
        stores.dirtyDao.markPushed(item.caldavTaskId, item.dirtyVersion)
    }
}

private const val TAG = "TodoistSynchronizer"

private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
