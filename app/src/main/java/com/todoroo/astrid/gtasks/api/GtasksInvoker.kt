package com.todoroo.astrid.gtasks.api

import android.content.Context
import com.google.api.client.http.HttpResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.GenericJson
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.tasks.Tasks
import com.google.api.services.tasks.TasksRequest
import com.google.api.services.tasks.TasksScopes
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import com.google.api.services.tasks.model.TaskLists
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.BuildConfig
import org.tasks.DebugNetworkInterceptor
import org.tasks.gtasks.GoogleAccountManager
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Wrapper around the official Google Tasks API to simplify common operations. In the case of an
 * exception, each request is tried twice in case of a timeout.
 *
 * @author Sam Bosley
 */
class GtasksInvoker {
    private val context: Context
    private val googleAccountManager: GoogleAccountManager
    private val preferences: Preferences
    private val interceptor: DebugNetworkInterceptor
    private val account: String?
    private val service: Tasks?
    private val credentialsAdapter: HttpCredentialsAdapter?

    @Inject
    constructor(
            @ApplicationContext context: Context,
            googleAccountManager: GoogleAccountManager,
            preferences: Preferences,
            interceptor: DebugNetworkInterceptor) {
        this.context = context
        this.googleAccountManager = googleAccountManager
        this.preferences = preferences
        this.interceptor = interceptor
        account = null
        service = null
        credentialsAdapter = null
    }

    private constructor(
            context: Context,
            googleAccountManager: GoogleAccountManager,
            preferences: Preferences,
            interceptor: DebugNetworkInterceptor,
            account: String) {
        this.context = context
        this.googleAccountManager = googleAccountManager
        this.preferences = preferences
        this.interceptor = interceptor
        this.account = account
        credentialsAdapter = HttpCredentialsAdapter(googleAccountManager)
        service = Tasks.Builder(NetHttpTransport(), JacksonFactory(), credentialsAdapter)
                .setApplicationName(String.format("Tasks/%s", BuildConfig.VERSION_NAME))
                .build()
    }

    fun forAccount(account: String): GtasksInvoker {
        return GtasksInvoker(context, googleAccountManager, preferences, interceptor, account)
    }

    @Throws(IOException::class)
    fun allGtaskLists(pageToken: String?): TaskLists? {
        return execute(service!!.tasklists().list().setMaxResults(100L).setPageToken(pageToken))
    }

    @Throws(IOException::class)
    fun getAllGtasksFromListId(
            listId: String?, lastSyncDate: Long, pageToken: String?): com.google.api.services.tasks.model.Tasks? {
        return execute(
                service!!
                        .tasks()
                        .list(listId)
                        .setMaxResults(100L)
                        .setShowDeleted(true)
                        .setShowHidden(true)
                        .setPageToken(pageToken)
                        .setUpdatedMin(
                                GtasksApiUtilities.unixTimeToGtasksCompletionTime(lastSyncDate).toStringRfc3339()))
    }

    @Throws(IOException::class)
    fun getAllPositions(
            listId: String?, pageToken: String?): com.google.api.services.tasks.model.Tasks {
        return execute(
                service!!
                        .tasks()
                        .list(listId)
                        .setMaxResults(100L)
                        .setShowDeleted(false)
                        .setShowHidden(false)
                        .setPageToken(pageToken)
                        .setFields("items(id,parent,position),nextPageToken"))!!
    }

    @Throws(IOException::class)
    fun createGtask(
            listId: String?, task: Task?, parent: String?, previous: String?): Task? {
        return execute(service!!.tasks().insert(listId, task).setParent(parent).setPrevious(previous))
    }

    @Throws(IOException::class)
    fun updateGtask(listId: String?, task: Task) {
        execute(service!!.tasks().update(listId, task.id, task))
    }

    @Throws(IOException::class)
    fun moveGtask(listId: String?, taskId: String?, parentId: String?, previousId: String?): Task? {
        return execute(
                service!!.tasks().move(listId, taskId).setParent(parentId).setPrevious(previousId))
    }

    @Throws(IOException::class)
    fun deleteGtaskList(listId: String?) {
        try {
            execute(service!!.tasklists().delete(listId))
        } catch (ignored: HttpNotFoundException) {
        }
    }

    @Throws(IOException::class)
    fun renameGtaskList(listId: String?, title: String?): TaskList? {
        return execute(service!!.tasklists().patch(listId, TaskList().setTitle(title)))
    }

    @Throws(IOException::class)
    fun createGtaskList(title: String?): TaskList? {
        return execute(service!!.tasklists().insert(TaskList().setTitle(title)))
    }

    @Throws(IOException::class)
    fun deleteGtask(listId: String?, taskId: String?) {
        try {
            execute(service!!.tasks().delete(listId, taskId))
        } catch (ignored: HttpNotFoundException) {
        }
    }

    @Synchronized
    @Throws(IOException::class)
    private fun <T> execute(request: TasksRequest<T>): T? {
        return execute(request, false)
    }

    @Synchronized
    @Throws(IOException::class)
    private fun <T> execute(request: TasksRequest<T>, retry: Boolean): T? {
        credentialsAdapter!!.checkToken(account, TasksScopes.TASKS)
        val response: T?
        response = try {
            val httpRequest = request.buildHttpRequest()
            Timber.d("%s", httpRequest.url)
            if (preferences.isFlipperEnabled) {
                interceptor.execute(httpRequest, request.responseClass)
            } else {
                httpRequest.execute().parseAs(request.responseClass)
            }
        } catch (e: HttpResponseException) {
            return if (e.statusCode == 401 && !retry) {
                credentialsAdapter.invalidateToken()
                execute(request, true)
            } else if (e.statusCode == 404) {
                throw HttpNotFoundException(e)
            } else {
                throw e
            }
        }
        Timber.d("%s response: %s", getCaller(retry), prettyPrint(response))
        return response
    }

    @Throws(IOException::class)
    private fun <T> prettyPrint(`object`: T?): Any? {
        if (BuildConfig.DEBUG) {
            if (`object` is GenericJson) {
                return (`object` as GenericJson).toPrettyString()
            }
        }
        return `object`
    }

    private fun getCaller(retry: Boolean): String {
        if (BuildConfig.DEBUG) {
            try {
                return Thread.currentThread().stackTrace[if (retry) 6 else 5].methodName
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        return ""
    }
}