package org.tasks.data

import android.content.ContentProviderOperation
import android.content.ContentProviderOperation.newDelete
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dmfs.tasks.contract.TaskContract.CommonSyncColumns
import org.dmfs.tasks.contract.TaskContract.LOAD_PROPERTIES
import org.dmfs.tasks.contract.TaskContract.Properties
import org.dmfs.tasks.contract.TaskContract.TaskListColumns
import org.dmfs.tasks.contract.TaskContract.TaskLists
import org.dmfs.tasks.contract.TaskContract.Tasks
import org.json.JSONObject
import org.tasks.R
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.openTaskType
import org.tasks.data.entity.CaldavCalendar
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

open class OpenTaskDao @Inject constructor(
        @ApplicationContext context: Context,
        private val caldavDao: CaldavDao
) {
    protected val cr: ContentResolver = context.contentResolver
    val authority = context.getString(R.string.opentasks_authority)
    val tasks: Uri = Tasks.getContentUri(authority)
    val taskLists: Uri = TaskLists.getContentUri(authority)
    val properties: Uri = Properties.getContentUri(authority)

    suspend fun shouldSync() =
        caldavDao.getAccounts(TYPE_OPENTASKS).isNotEmpty() ||
                getListsByAccount().filterActive(caldavDao).isNotEmpty()

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
                calendars.add(
                    CaldavCalendar(
                        id = it.getLong(TaskLists._ID),
                        account = "$accountType:$accountName",
                        name = it.getString(TaskLists.LIST_NAME),
                        color = it.getInt(TaskLists.LIST_COLOR),
                        url = it.getString(CommonSyncColumns._SYNC_ID),
                        ctag = it.getString(TaskLists.SYNC_VERSION)
                            ?.let(::JSONObject)
                            ?.getString("value"),
                        access = when (it.getInt(TaskLists.ACCESS_LEVEL)) {
                            TaskLists.ACCESS_LEVEL_OWNER -> CaldavCalendar.ACCESS_OWNER
                            TaskLists.ACCESS_LEVEL_READ -> CaldavCalendar.ACCESS_READ_ONLY
                            else -> CaldavCalendar.ACCESS_READ_WRITE
                        },
                    )
                )
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

    suspend fun getTask(listId: Long, uid: String): MyAndroidTask? = withContext(Dispatchers.IO) {
        cr.query(
                tasks.buildUpon().appendQueryParameter(LOAD_PROPERTIES, "1").build(),
                null,
                "${Tasks.LIST_ID} = $listId AND ${Tasks._UID} = '$uid'",
                null,
                null)?.use {
            if (it.moveToFirst()) {
                MyAndroidTask(it)
            } else {
                null
            }
        }
    }

    companion object {
        private const val OPENTASK_BATCH_LIMIT = 499
        const val ACCOUNT_TYPE_DAVX5 = "bitfire.at.davdroid"
        const val ACCOUNT_TYPE_DAVX5_MANAGED = "com.davdroid.managed"
        private const val ACCOUNT_TYPE_ETESYNC = "com.etesync.syncadapter"
        private const val ACCOUNT_TYPE_DECSYNC = "org.decsync.tasks"
        val SUPPORTED_TYPES = setOf(
                ACCOUNT_TYPE_DAVX5,
                ACCOUNT_TYPE_DAVX5_MANAGED,
                ACCOUNT_TYPE_ETESYNC,
                ACCOUNT_TYPE_DECSYNC
        )
        val SUPPORTED_TYPE_FILTER = SUPPORTED_TYPES.joinToString(" OR ") { "ACCOUNT_TYPE = '$it'" }

        suspend fun Map<String, List<CaldavCalendar>>.filterActive(caldavDao: CaldavDao) =
                filterNot { (_, lists) -> caldavDao.anyExist(lists.map { it.url!! }) }

        fun String?.isDavx5(): Boolean = this?.startsWith(ACCOUNT_TYPE_DAVX5) == true

        fun String?.isDavx5Managed(): Boolean = this?.startsWith(ACCOUNT_TYPE_DAVX5_MANAGED) == true

        fun String?.isEteSync(): Boolean = this?.startsWith(ACCOUNT_TYPE_ETESYNC) == true

        fun String?.isDecSync(): Boolean = this?.startsWith(ACCOUNT_TYPE_DECSYNC) == true

        private fun Cursor.getString(columnName: String): String? =
                getString(getColumnIndexOrThrow(columnName))

        private fun Cursor.getInt(columnName: String): Int =
                getInt(getColumnIndexOrThrow(columnName))

        fun Cursor.getLong(columnName: String): Long =
                getLong(getColumnIndexOrThrow(columnName))

        fun CaldavCalendar.toLocalCalendar(): CaldavCalendar {
            return CaldavCalendar(
                uuid = UUID
                    .nameUUIDFromBytes("${account.openTaskType()}$url".toByteArray())
                    .toString(),
                url = url,
                color = color,
                name = name,
                account = account,
                access = access,
            )
        }
    }
}