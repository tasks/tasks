package org.tasks.opentasks

import android.content.ContentProviderResult
import android.content.Context
import at.bitfire.ical4android.BatchOperation
import at.bitfire.ical4android.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import org.dmfs.tasks.contract.TaskContract
import org.dmfs.tasks.contract.TaskContract.TaskListColumns.ACCESS_LEVEL_OWNER
import org.tasks.caldav.iCalendar
import org.tasks.data.MyAndroidTask
import org.tasks.data.OpenTaskDao
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavCalendar
import javax.inject.Inject

class TestOpenTaskDao @Inject constructor(
        @ApplicationContext context: Context,
        private val caldavDao: CaldavDao
) : OpenTaskDao(context, caldavDao) {
    suspend fun insertList(
        name: String = DEFAULT_LIST,
        type: String = DEFAULT_TYPE,
        account: String = DEFAULT_ACCOUNT,
        url: String = UUIDHelper.newUUID(),
        accessLevel: Int = ACCESS_LEVEL_OWNER,
    ): Pair<Long, CaldavCalendar> {
        val uri = taskLists.buildUpon()
                .appendQueryParameter(TaskContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(TaskContract.TaskLists.ACCOUNT_NAME, account)
                .appendQueryParameter(TaskContract.TaskLists.ACCOUNT_TYPE, type)
                .build()
        val result = applyOperation(
                BatchOperation.CpoBuilder.newInsert(uri)
                        .withValue(TaskContract.CommonSyncColumns._SYNC_ID, url)
                        .withValue(TaskContract.TaskListColumns.LIST_NAME, name)
                        .withValue(TaskContract.TaskLists.SYNC_ENABLED, "1")
                        .withValue(TaskContract.TaskLists.ACCESS_LEVEL, accessLevel)
        )
        val calendar = CaldavCalendar(
            uuid = UUIDHelper.newUUID(),
            name = name,
            account = "$type:$account",
            url = url,
        )
        caldavDao.insert(calendar)
        return Pair(result.uri!!.lastPathSegment!!.toLong(), calendar)
    }

    fun insertTask(listId: Long, vtodo: String) {
        val ops = ArrayList<BatchOperation.CpoBuilder>()
        val task = MyAndroidTask(iCalendar.fromVtodo(vtodo)!!)
        ops.add(task.toBuilder(tasks).withValue(TaskContract.TaskColumns.LIST_ID, listId))
        task.enqueueProperties(properties, ops, 0)
        applyOperation(*ops.toTypedArray())
    }

    fun getTasks(): List<Task> {
        val result = ArrayList<Task>()
        cr.query(
                tasks.buildUpon().appendQueryParameter(TaskContract.LOAD_PROPERTIES, "1").build(),
                null,
                null,
                null,
                null)?.use {
            while (it.moveToNext()) {
                MyAndroidTask(it).task?.let { task -> result.add(task) }
            }
        }
        return result
    }

    fun reset(
            type: String = DEFAULT_TYPE,
            account: String = DEFAULT_ACCOUNT
    ) {
        cr.delete(
                taskLists.buildUpon()
                        .appendQueryParameter(TaskContract.CALLER_IS_SYNCADAPTER, "true")
                        .appendQueryParameter(TaskContract.TaskLists.ACCOUNT_NAME, account)
                        .appendQueryParameter(TaskContract.TaskLists.ACCOUNT_TYPE, type)
                        .build(),
                null,
                null
        )
        cr.delete(tasks, null, null)
    }

    private fun applyOperation(vararg builders: BatchOperation.CpoBuilder): ContentProviderResult =
            cr.applyBatch(authority, ArrayList(builders.asList().map { it.build() }))[0]

    companion object {
        const val DEFAULT_ACCOUNT = "test_account"
        const val DEFAULT_TYPE = ACCOUNT_TYPE_DAVX5
        const val DEFAULT_LIST = "default_list"
    }
}