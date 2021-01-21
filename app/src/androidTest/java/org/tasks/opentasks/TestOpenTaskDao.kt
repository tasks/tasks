package org.tasks.opentasks

import android.content.ContentProviderResult
import android.content.Context
import at.bitfire.ical4android.BatchOperation
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import org.dmfs.tasks.contract.TaskContract
import org.tasks.TestUtilities
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.MyAndroidTask
import org.tasks.data.OpenTaskDao
import javax.inject.Inject

class TestOpenTaskDao @Inject constructor(
        @ApplicationContext context: Context,
        private val caldavDao: CaldavDao
) : OpenTaskDao(context, caldavDao) {
    suspend fun insertList(
            name: String = DEFAULT_LIST,
            type: String = DEFAULT_TYPE,
            account: String = DEFAULT_ACCOUNT
    ): Pair<String, CaldavCalendar> {
        val url = UUIDHelper.newUUID()
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
        )
        return Pair(result.uri!!.lastPathSegment!!, CaldavCalendar().apply {
            uuid = UUIDHelper.newUUID()
            this.account = "$type:$account"
            this.url = url
            caldavDao.insert(this)
        })
    }

    fun insertTask(listId: String, vtodo: String) {
        applyOperation(
                MyAndroidTask(TestUtilities.fromString(vtodo))
                        .toBuilder(tasks, true)
                        .withValue(TaskContract.TaskColumns.LIST_ID, listId)
        )
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

    private fun applyOperation(build: BatchOperation.CpoBuilder): ContentProviderResult =
            cr.applyBatch(authority, arrayListOf(build.build()))[0]

    companion object {
        const val DEFAULT_ACCOUNT = "test_account"
        const val DEFAULT_TYPE = ACCOUNT_TYPE_DAVx5
        const val DEFAULT_LIST = "default_list"
    }
}