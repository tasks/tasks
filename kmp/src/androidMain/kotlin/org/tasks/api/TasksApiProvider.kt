package org.tasks.api

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.analytics.Analytics
import org.tasks.analytics.AnalyticsEvents
import org.tasks.api.TasksContract.Reminders
import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Places
import org.tasks.api.TasksContract.Tags
import org.tasks.api.TasksContract.TaskTags
import org.tasks.api.TasksContract.Tasks
import org.tasks.data.db.Database
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

abstract class TasksApiProvider : ContentProvider() {
    interface Dependencies {
        val database: Database
        val queryEngine: ApiQueryEngine
        val writer: ApiWriter
        val analytics: Analytics
    }

    protected abstract val dependencies: Dependencies

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ownershipChecked = AtomicBoolean(false)
    private var watching: Database? = null
    private var watchJob: Job? = null

    private val batchDispatch = ThreadLocal<((suspend () -> Any?) -> Any?)?>()

    override fun onCreate(): Boolean {
        scope.launch {
            Database.opened.first { it }
            runCatching { startWatching(dependencies.database) }
        }
        return true
    }

    override fun getType(uri: Uri): String = when (URI_MATCHER.match(uri)) {
        TASKS -> Tasks.TYPE_DIR
        TASK -> Tasks.TYPE_ITEM
        REMINDERS -> Reminders.TYPE_DIR
        REMINDER -> Reminders.TYPE_ITEM
        TASK_TAGS -> TaskTags.TYPE_DIR
        TASK_TAG -> TaskTags.TYPE_ITEM
        LISTS -> Lists.TYPE_DIR
        LIST -> Lists.TYPE_ITEM
        TAGS -> Tags.TYPE_DIR
        TAG -> Tags.TYPE_ITEM
        PLACES -> Places.TYPE_DIR
        PLACE -> Places.TYPE_ITEM
        ACCOUNTS -> TasksContract.Accounts.TYPE_DIR
        ACCOUNT -> TasksContract.Accounts.TYPE_ITEM
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        ApiQueryArgs.rejectSql(selection, selectionArgs, sortOrder)
        return query(uri, projection, queryArgs = null, cancellationSignal = null)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?,
    ): Cursor {
        onFirstUse()
        val match = URI_MATCHER.match(uri)
        val table = tableFor(match, uri)
        val engine = dependencies.queryEngine
        val resolver = context?.contentResolver
        val collectionUri = collectionUri(table.path)
        return blocking {
            val item = match.isItem
            val rows = if (item) {
                ApiQueryArgs.parse(uri, queryArgs, emptyList())
                engine.queryItem(table, uri.itemId)
            } else {
                val args = ApiQueryArgs.parse(uri, queryArgs, TasksContract.paramsFor(table.path))
                engine.queryCollection(table, args)
            }
            rows.toCursor(projection).apply {
                if (!item) {
                    extras = Bundle().apply {
                        putInt(ContentResolver.EXTRA_TOTAL_COUNT, rows.total)
                    }
                }
                resolver?.let { setNotificationUri(it, collectionUri) }
            }
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        onFirstUse()
        rejectParameters(uri)
        val match = URI_MATCHER.match(uri)
        val writer = dependencies.writer
        val row = (values ?: ContentValues()).toApiValues()
        val path = when (match) {
            TASKS -> Tasks.PATH
            REMINDERS -> Reminders.PATH
            TASK_TAGS -> TaskTags.PATH
            LISTS -> Lists.PATH
            TAGS -> Tags.PATH
            PLACES -> Places.PATH
            else -> throw unsupported("insert", uri)
        }
        val id = blocking {
            when (match) {
                TASKS -> writer.insertTask(row)
                REMINDERS -> writer.insertReminder(row)
                TASK_TAGS -> writer.insertTaskTag(row)
                LISTS -> writer.insertList(row)
                TAGS -> writer.insertTag(row).id
                else -> writer.insertPlace(row).id
            }
        }
        return ContentUris.withAppendedId(collectionUri(path), id)
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        ApiQueryArgs.rejectSql(selection, selectionArgs)
        onFirstUse()
        val writer = dependencies.writer
        val row = (values ?: ContentValues()).toApiValues()
        return when (URI_MATCHER.match(uri)) {
            TASK -> rejectParameters(uri).let { blocking { writer.updateTask(uri.itemId, row) } }
            REMINDER -> rejectParameters(uri).let { blocking { writer.updateReminder(uri.itemId, row) } }
            LIST -> rejectParameters(uri).let { blocking { writer.updateList(uri.itemId, row) } }
            TAG -> rejectParameters(uri).let { blocking { writer.updateTag(uri.itemId, row) } }
            PLACE -> rejectParameters(uri).let { blocking { writer.updatePlace(uri.itemId, row) } }
            else -> throw unsupported("update", uri)
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        ApiQueryArgs.rejectSql(selection, selectionArgs)
        onFirstUse()
        val writer = dependencies.writer
        return when (val match = URI_MATCHER.match(uri)) {
            TASK, REMINDER, TASK_TAG, LIST, TAG, PLACE -> {
                rejectParameters(uri)
                val id = uri.itemId
                blocking {
                    when (match) {
                        TASK -> writer.deleteTask(id)
                        REMINDER -> writer.deleteReminder(id)
                        TASK_TAG -> writer.deleteTaskTag(id)
                        LIST -> writer.deleteList(id)
                        TAG -> writer.deleteTag(id)
                        else -> writer.deletePlace(id)
                    }
                }
            }
            TASK_TAGS -> {
                val args = ApiQueryArgs.parse(uri, null, listOf(TaskTags.PARAM_TASK, TaskTags.PARAM_TAG))
                val task = args.long(TaskTags.PARAM_TASK)
                    ?: throw IllegalArgumentException("${TaskTags.PARAM_TASK} is required")
                val tag = args.long(TaskTags.PARAM_TAG)
                    ?: throw IllegalArgumentException("${TaskTags.PARAM_TAG} is required")
                blocking { writer.deleteTaskTag(task, tag) }
            }
            else -> throw unsupported("delete", uri)
        }
    }

    override fun applyBatch(
        operations: ArrayList<ContentProviderOperation>,
    ): Array<ContentProviderResult> {
        onFirstUse()
        rejectListWrites(operations)
        val results = arrayOfNulls<ContentProviderResult>(operations.size)
        val calls = Channel<BatchCall>(Channel.UNLIMITED)
        val failure = AtomicReference<Throwable?>(null)
        var worker: Thread? = null
        try {
            runBlocking {
                try {
                    dependencies.database.useWriterConnection { transactor ->
                        transactor.immediateTransaction {
                            worker = thread(name = "tasks-api-batch") {
                                batchDispatch.set { block ->
                                    BatchCall(block)
                                        .also { calls.trySend(it) }
                                        .await()
                                }
                                try {
                                    operations.forEachIndexed { index, operation ->
                                        results[index] = operation.apply(this@TasksApiProvider, results, index)
                                    }
                                } catch (t: Throwable) {
                                    failure.set(t)
                                } finally {
                                    batchDispatch.remove()
                                    calls.close()
                                }
                            }
                            for (call in calls) {
                                call.run()
                            }
                            failure.get()?.let { throw it }
                        }
                    }
                } finally {
                    calls.close()
                    while (true) {
                        val pending = calls.tryReceive().getOrNull() ?: break
                        pending.abandon()
                    }
                }
            }
        } finally {
            worker?.join()
        }
        @Suppress("UNCHECKED_CAST")
        return results as Array<ContentProviderResult>
    }

    private class BatchCall(private val block: suspend () -> Any?) {
        private val latch = CountDownLatch(1)
        private var value: Any? = null
        private var error: Throwable? = null

        suspend fun run() {
            try {
                value = block()
            } catch (t: Throwable) {
                error = t
            } finally {
                latch.countDown()
            }
        }

        fun abandon() {
            error = IllegalStateException("Batch transaction ended")
            latch.countDown()
        }

        fun await(): Any? {
            latch.await()
            error?.let { throw it }
            return value
        }
    }

    private fun <T> blocking(block: suspend () -> T): T {
        val dispatch = batchDispatch.get()
        @Suppress("UNCHECKED_CAST")
        return if (dispatch == null) {
            runBlocking { block() }
        } else {
            dispatch(block as suspend () -> Any?) as T
        }
    }

    private fun onFirstUse() {
        checkPermissionOwnership()
        val dependencies = dependencies
        val caller = runCatching { callingPackage }.getOrNull() ?: UNKNOWN_CALLER
        scope.launch {
            dependencies.analytics.logEventOncePerDay(
                event = AnalyticsEvents.CONTENT_PROVIDER_API,
                dedupeBy = caller,
                AnalyticsEvents.PARAM_PACKAGE to caller,
            )
        }
        startWatching(dependencies.database)
    }

    @Synchronized
    private fun startWatching(database: Database) {
        if (watching === database) return
        val resolver = context?.contentResolver ?: return
        watchJob?.cancel()
        watching = database
        watchJob = scope.launch {
            database
                .invalidationTracker
                .createFlow(*NOTIFY.keys.toTypedArray(), emitInitialState = false)
                .collect { changed ->
                    changed
                        .flatMapTo(mutableSetOf()) { NOTIFY[it].orEmpty() }
                        .forEach { resolver.notifyChange(collectionUri(it), null) }
                }
        }
    }

    private fun checkPermissionOwnership() {
        val context = context ?: return
        if (!ownershipChecked.compareAndSet(false, true)) return
        try {
            listOf(TasksContract.PERMISSION_READ, TasksContract.PERMISSION_WRITE).forEach {
                val owner = context.packageManager.getPermissionInfo(it, 0).packageName
                if (owner != context.packageName) {
                    ownershipChecked.set(false)
                    throw SecurityException("$it is defined by $owner, not ${context.packageName}")
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            ownershipChecked.set(false)
            throw SecurityException("Tasks permissions are not installed", e)
        }
    }

    private fun tableFor(match: Int, uri: Uri): ApiTable = when (match) {
        TASKS, TASK -> ApiTables.TASKS
        REMINDERS, REMINDER -> ApiTables.REMINDERS
        TASK_TAGS, TASK_TAG -> ApiTables.TASK_TAGS
        LISTS, LIST -> ApiTables.LISTS
        TAGS, TAG -> ApiTables.TAGS
        PLACES, PLACE -> ApiTables.PLACES
        ACCOUNTS, ACCOUNT -> ApiTables.ACCOUNTS
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    private fun rejectListWrites(operations: List<ContentProviderOperation>) {
        operations.forEachIndexed { index, operation ->
            if (!operation.isReadOperation && URI_MATCHER.match(operation.uri) in LIST_MATCHES) {
                throw IllegalArgumentException(
                    "Operation $index writes ${operation.uri}. List writes can block on the network," +
                            " so they are not allowed in a batch — do it in its own call"
                )
            }
        }
    }

    private fun rejectParameters(uri: Uri) {
        if (!uri.isOpaque && uri.queryParameterNames.isNotEmpty()) {
            throw IllegalArgumentException(
                "${uri.queryParameterNames.joinToString(", ")} is not supported on $uri"
            )
        }
    }

    private fun unsupported(verb: String, uri: Uri) =
        IllegalArgumentException("$verb is not supported on $uri")

    private val Uri.itemId: Long
        get() = try {
            ContentUris.parseId(this)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Expected a numeric id: $this", e)
        }

    private val Int.isItem: Boolean
        get() = this in ITEM_MATCHES

    companion object {
        internal const val UNKNOWN_CALLER = "unknown"

        private const val TASKS = 1
        private const val TASK = 2
        private const val REMINDERS = 3
        private const val REMINDER = 4
        private const val TASK_TAGS = 5
        private const val TASK_TAG = 6
        private const val LISTS = 9
        private const val LIST = 10
        private const val TAGS = 11
        private const val TAG = 12
        private const val PLACES = 13
        private const val PLACE = 14
        private const val ACCOUNTS = 15
        private const val ACCOUNT = 16

        private val ITEM_MATCHES = setOf(TASK, REMINDER, TASK_TAG, LIST, TAG, PLACE, ACCOUNT)

        private val LIST_MATCHES = setOf(LISTS, LIST)

        private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            val v = TasksContract.VERSION
            listOf(
                Tasks.PATH to (TASKS to TASK),
                Reminders.PATH to (REMINDERS to REMINDER),
                TaskTags.PATH to (TASK_TAGS to TASK_TAG),
                Lists.PATH to (LISTS to LIST),
                Tags.PATH to (TAGS to TAG),
                Places.PATH to (PLACES to PLACE),
                TasksContract.Accounts.PATH to (ACCOUNTS to ACCOUNT),
            ).forEach { (path, codes) ->
                addURI(TasksContract.AUTHORITY, "$v/$path", codes.first)
                addURI(TasksContract.AUTHORITY, "$v/$path/#", codes.second)
            }
        }

        private val TASK_SCOPED =
            listOf(Tasks.PATH, Reminders.PATH, TaskTags.PATH)

        private val NOTIFY: Map<String, List<String>> = mapOf(
            "tasks" to TASK_SCOPED,
            "caldav_tasks" to TASK_SCOPED,
            "alarms" to listOf(Reminders.PATH),
            "tags" to listOf(TaskTags.PATH, Tasks.PATH),
            "geofences" to listOf(Reminders.PATH, Tasks.PATH),
            "tagdata" to listOf(Tags.PATH),
            "places" to listOf(Places.PATH),
            "caldav_lists" to listOf(Lists.PATH),
            "caldav_accounts" to listOf(TasksContract.Accounts.PATH),
        )

        internal fun collectionUri(path: String): Uri =
            Uri.parse("${TasksContract.CONTENT_URI}/$path")
    }
}
