package org.tasks.data

import android.content.ContentProviderOperation
import android.content.ContentProviderOperation.newDelete
import android.content.ContentProviderOperation.newInsert
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import at.bitfire.ical4android.BatchOperation
import at.bitfire.ical4android.Task
import at.bitfire.ical4android.UnknownProperty
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.fortuna.ical4j.model.property.XProperty
import org.dmfs.tasks.contract.TaskContract.*
import org.dmfs.tasks.contract.TaskContract.Property.Category
import org.dmfs.tasks.contract.TaskContract.Property.Relation
import org.json.JSONObject
import org.tasks.R
import org.tasks.caldav.iCalendar.Companion.APPLE_SORT_ORDER
import timber.log.Timber
import javax.inject.Inject

open class OpenTaskDao @Inject constructor(
        @ApplicationContext context: Context,
        private val caldavDao: CaldavDao
) {
    protected val cr = context.contentResolver
    val authority = context.getString(R.string.opentasks_authority)
    val tasks: Uri = Tasks.getContentUri(authority)
    val taskLists: Uri = TaskLists.getContentUri(authority)
    private val properties = Properties.getContentUri(authority)

    suspend fun newAccounts(): List<String> = getListsByAccount().newAccounts(caldavDao)

    suspend fun getListsByAccount(): Map<String, List<CaldavCalendar>> =
            getLists().groupBy { it.account!! }

    suspend fun getLists(): List<CaldavCalendar> = withContext(Dispatchers.IO) {
        val calendars = ArrayList<CaldavCalendar>()
        cr.query(
                taskLists,
                null,
                "${TaskListColumns.SYNC_ENABLED}=1 AND ($SUPPORTED_TYPE_FILTER)",
                null,
                null)?.use {
            while (it.moveToNext()) {
                val accountType = it.getString(TaskLists.ACCOUNT_TYPE)
                val accountName = it.getString(TaskLists.ACCOUNT_NAME)
                calendars.add(CaldavCalendar().apply {
                    id = it.getLong(TaskLists._ID)
                    account = "$accountType:$accountName"
                    name = it.getString(TaskLists.LIST_NAME)
                    color = it.getInt(TaskLists.LIST_COLOR)
                    url = it.getString(CommonSyncColumns._SYNC_ID)
                    ctag = it.getString(TaskLists.SYNC_VERSION)
                            ?.let(::JSONObject)
                            ?.getString("value")
                })
            }
        }
        calendars
    }

    suspend fun getEtags(listId: Long): List<Triple<String, String?, String>> = withContext(Dispatchers.IO) {
        val items = ArrayList<Triple<String, String?, String>>()
        cr.query(
                tasks,
                arrayOf(Tasks._UID, Tasks.SYNC1, "version"),
                "${Tasks.LIST_ID} = $listId",
                null,
                null)?.use {
            while (it.moveToNext()) {
                items.add(Triple(
                        it.getString(Tasks._UID)!!,
                        it.getString(Tasks.SYNC1),
                        it.getLong("version").toString()))
            }
        }
        items
    }

    fun delete(listId: Long, uid: String): ContentProviderOperation =
            newDelete(tasks)
                    .withSelection(
                            "${Tasks.LIST_ID} = $listId AND ${Tasks._UID} = '$uid'",
                            null)
                    .build()

    fun insert(builder: BatchOperation.CpoBuilder): ContentProviderOperation = builder.build()

    fun update(listId: Long, uid: String, builder: BatchOperation.CpoBuilder): ContentProviderOperation =
            builder
                    .withSelection(
                            "${Tasks.LIST_ID} = $listId AND ${Tasks._UID} = '$uid'",
                            emptyArray()
                    )
                    .build()

    suspend fun getId(listId: Long, uid: String?): Long? =
            uid
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        withContext(Dispatchers.IO) {
                            cr.query(
                                    tasks,
                                    arrayOf(Tasks._ID),
                                    "${Tasks.LIST_ID} = $listId AND ${Tasks._UID} = '$uid'",
                                    null,
                                    null)?.use {
                                if (it.moveToFirst()) {
                                    it.getLong(Tasks._ID)
                                } else {
                                    null
                                }
                            }
                        }
                    }
                    ?: uid?.let {
                        Timber.e("No task with uid=$it")
                        null
                    }

    suspend fun batch(operations: List<ContentProviderOperation>) = withContext(Dispatchers.IO) {
        operations.chunked(OPENTASK_BATCH_LIMIT).forEach {
            cr.applyBatch(authority, ArrayList(it))
        }
    }

    fun setTags(id: Long, tags: List<String>): List<ContentProviderOperation> {
        val delete = listOf(
                newDelete(properties)
                        .withSelection(
                                "${Properties.TASK_ID} = $id AND ${Properties.MIMETYPE} = '${Category.CONTENT_ITEM_TYPE}'",
                                null)
                        .build())
        val inserts = tags.map {
            newInsert(properties)
                    .withValues(ContentValues().apply {
                        put(Category.MIMETYPE, Category.CONTENT_ITEM_TYPE)
                        put(Category.TASK_ID, id)
                        put(Category.CATEGORY_NAME, it)
                    })
                    .build()
        }
        return delete + inserts
    }

    fun setRemoteOrder(id: Long, caldavTask: CaldavTask): List<ContentProviderOperation> {
        val operations = ArrayList<ContentProviderOperation>()
        operations.add(
                newDelete(properties)
                        .withSelection(
                                "${Properties.TASK_ID} = $id AND ${Properties.MIMETYPE} = '${UnknownProperty.CONTENT_ITEM_TYPE}' AND ${Properties.DATA0} LIKE '%$APPLE_SORT_ORDER%'",
                                null)
                        .build())
        caldavTask.order?.let {
            operations.add(
                    newInsert(properties)
                            .withValues(ContentValues().apply {
                                put(Properties.MIMETYPE, UnknownProperty.CONTENT_ITEM_TYPE)
                                put(Properties.TASK_ID, id)
                                put(Properties.DATA0, UnknownProperty.toJsonString(XProperty(APPLE_SORT_ORDER, it.toString())))
                            })
                            .build())
        }
        return operations
    }

    fun updateParent(id: Long, parent: Long?): List<ContentProviderOperation> {
        val operations = ArrayList<ContentProviderOperation>()
        operations.add(
                newDelete(properties)
                        .withSelection(
                                "${Properties.TASK_ID} = $id AND ${Properties.MIMETYPE} = '${Relation.CONTENT_ITEM_TYPE}' AND ${Relation.RELATED_TYPE} = ${Relation.RELTYPE_PARENT}",
                                null
                        )
                        .build())
        parent?.let {
            operations.add(
                    newInsert(properties)
                            .withValues(ContentValues().apply {
                                put(Relation.MIMETYPE, Relation.CONTENT_ITEM_TYPE)
                                put(Relation.TASK_ID, id)
                                put(Relation.RELATED_TYPE, Relation.RELTYPE_PARENT)
                                put(Relation.RELATED_ID, parent)
                            })
                            .build())
        }
        return operations
    }

    suspend fun getTask(listId: Long, uid: String): Task? = withContext(Dispatchers.IO) {
        cr.query(
                tasks.buildUpon().appendQueryParameter(LOAD_PROPERTIES, "1").build(),
                null,
                "${Tasks.LIST_ID} = $listId AND ${Tasks._UID} = '$uid'",
                null,
                null)?.use {
            if (it.moveToFirst()) {
                MyAndroidTask(it).task
            } else {
                null
            }
        }
    }

    companion object {
        private const val OPENTASK_BATCH_LIMIT = 499
        const val ACCOUNT_TYPE_DAVx5 = "bitfire.at.davdroid"
        private const val ACCOUNT_TYPE_ETESYNC = "com.etesync.syncadapter"
        private const val ACCOUNT_TYPE_DECSYNC = "org.decsync.tasks"
        val SUPPORTED_TYPES = setOf(
                ACCOUNT_TYPE_DAVx5,
                ACCOUNT_TYPE_ETESYNC,
                ACCOUNT_TYPE_DECSYNC
        )
        val SUPPORTED_TYPE_FILTER = SUPPORTED_TYPES.joinToString(" OR ") { "ACCOUNT_TYPE = '$it'" }

        suspend fun Map<String, List<CaldavCalendar>>.newAccounts(caldavDao: CaldavDao) =
                filterNot { (_, lists) -> caldavDao.anyExist(lists.map { it.url!! }) }
                        .map { it.key }
                        .distinct()

        fun String?.isDavx5(): Boolean = this?.startsWith(ACCOUNT_TYPE_DAVx5) == true

        fun String?.isEteSync(): Boolean = this?.startsWith(ACCOUNT_TYPE_ETESYNC) == true

        fun String?.isDecSync(): Boolean = this?.startsWith(ACCOUNT_TYPE_DECSYNC) == true

        private fun Cursor.getString(columnName: String): String? =
                getString(getColumnIndex(columnName))

        private fun Cursor.getInt(columnName: String): Int =
                getInt(getColumnIndex(columnName))

        private fun Cursor.getLong(columnName: String): Long =
                getLong(getColumnIndex(columnName))
    }
}