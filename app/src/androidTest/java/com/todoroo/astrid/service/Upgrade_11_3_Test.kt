@file:Suppress("ClassName")

package com.todoroo.astrid.service

import android.content.ContentProviderResult
import at.bitfire.ical4android.BatchOperation
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.dmfs.tasks.contract.TaskContract
import org.dmfs.tasks.contract.TaskContract.CALLER_IS_SYNCADAPTER
import org.dmfs.tasks.contract.TaskContract.TaskLists
import org.junit.Test
import org.tasks.SuspendFreeze.Companion.freezeAt
import org.tasks.TestUtilities.assertEquals
import org.tasks.TestUtilities.fromString
import org.tasks.data.*
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.TASK
import org.tasks.makers.CaldavTaskMaker.VTODO
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.DUE_DATE
import org.tasks.makers.TaskMaker.HIDE_TYPE
import org.tasks.makers.TaskMaker.MODIFICATION_TIME
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class Upgrade_11_3_Test : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var openTaskDao: OpenTaskDao
    @Inject lateinit var upgrader: Upgrade_11_3

    @Test
    fun applyRemoteiCalendarStartDate() = runBlocking {
        val taskId = taskDao.insert(newTask())
        caldavDao.insert(newCaldavTask(with(TASK, taskId), with(VTODO, VTODO_WITH_START_DATE)))
        upgrader.applyiCalendarStartDates()

        assertEquals(DateTime(2021, 1, 21), taskDao.fetch(taskId)?.hideUntil)
    }

    @Test
    fun ignoreRemoteiCalendarStartDate() = runBlocking {
        val taskId = taskDao.insert(newTask(
                with(DUE_DATE, DateTime(2021, 1, 20)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE)
        ))
        caldavDao.insert(newCaldavTask(with(TASK, taskId), with(VTODO, VTODO_WITH_START_DATE)))
        upgrader.applyiCalendarStartDates()

        assertEquals(DateTime(2021, 1, 20), taskDao.fetch(taskId)?.hideUntil)
    }

    @Test
    fun touchTaskWithLocaliCalendarStartDate() = runBlocking {
        val upgradeTime = DateTime(2021, 1, 21, 11, 47, 32, 450)
        val taskId = taskDao.insert(newTask(
                with(DUE_DATE, DateTime(2021, 1, 20)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE),
                with(MODIFICATION_TIME, DateTime(2021, 1, 21, 9, 50, 4, 348))
        ))
        caldavDao.insert(newCaldavTask(with(TASK, taskId), with(VTODO, VTODO_WITH_START_DATE)))

        freezeAt(upgradeTime) {
            upgrader.applyiCalendarStartDates()
        }

        assertEquals(upgradeTime, taskDao.fetch(taskId)?.modificationDate)
    }

    @Test
    fun dontTouchWhenNoiCalendarStartDate() = runBlocking {
        val modificationTime = DateTime(2021, 1, 21, 9, 50, 4, 348)
        val taskId = taskDao.insert(newTask(with(MODIFICATION_TIME, modificationTime)))
        caldavDao.insert(newCaldavTask(with(TASK, taskId), with(VTODO, VTODO_NO_START_DATE)))

        upgrader.applyiCalendarStartDates()

        assertEquals(modificationTime, taskDao.fetch(taskId)?.modificationDate)
    }

    @Test
    fun applyRemoteOpenTaskStartDate() = runBlocking {
        val (listId, list) = insertList()
        applyOperation(
                MyAndroidTask(fromString(VTODO_WITH_START_DATE))
                        .toBuilder(openTaskDao.tasks, true)
                        .withValue(TaskContract.TaskColumns.LIST_ID, listId)
        )
        val taskId = taskDao.insert(newTask())
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "4586964443060640060"),
                with(TASK, taskId)
        ))

        upgrader.applyOpenTaskStartDates()

        assertEquals(DateTime(2021, 1, 21), taskDao.fetch(taskId)?.hideUntil)
    }

    @Test
    fun ignoreRemoteOpenTaskStartDate() = runBlocking {
        val (listId, list) = insertList()
        applyOperation(
                MyAndroidTask(fromString(VTODO_WITH_START_DATE))
                        .toBuilder(openTaskDao.tasks, true)
                        .withValue(TaskContract.TaskColumns.LIST_ID, listId)
        )
        val taskId = taskDao.insert(newTask(
                with(DUE_DATE, DateTime(2021, 1, 20)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE)
        ))
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "4586964443060640060"),
                with(TASK, taskId)
        ))

        upgrader.applyOpenTaskStartDates()

        assertEquals(DateTime(2021, 1, 20), taskDao.fetch(taskId)?.hideUntil)
    }

    @Test
    fun touchWithOpenTaskStartDate() = runBlocking {
        val upgradeTime = DateTime(2021, 1, 21, 11, 47, 32, 450)
        val (listId, list) = insertList()
        applyOperation(
                MyAndroidTask(fromString(VTODO_WITH_START_DATE))
                        .toBuilder(openTaskDao.tasks, true)
                        .withValue(TaskContract.TaskColumns.LIST_ID, listId)
        )
        val taskId = taskDao.insert(newTask(
                with(DUE_DATE, DateTime(2021, 1, 20)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE),
                with(MODIFICATION_TIME, DateTime(2021, 1, 21, 9, 50, 4, 348))
        ))
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "4586964443060640060"),
                with(TASK, taskId)
        ))

        freezeAt(upgradeTime) {
            upgrader.applyOpenTaskStartDates()
        }

        assertEquals(upgradeTime, taskDao.fetch(taskId)?.modificationDate)
    }

    @Test
    fun dontTouchNoOpenTaskStartDate() = runBlocking {
        val modificationTime = DateTime(2021, 1, 21, 9, 50, 4, 348)
        val (listId, list) = insertList()
        applyOperation(
                MyAndroidTask(fromString(VTODO_NO_START_DATE))
                        .toBuilder(openTaskDao.tasks, true)
                        .withValue(TaskContract.TaskColumns.LIST_ID, listId)
        )
        val taskId = taskDao.insert(newTask(with(MODIFICATION_TIME, modificationTime)))
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "4586964443060640060"),
                with(TASK, taskId)
        ))

        upgrader.applyOpenTaskStartDates()

        assertEquals(modificationTime, taskDao.fetch(taskId)?.modificationDate)
    }

    private suspend fun insertList(
            type: String = OpenTaskDao.ACCOUNT_TYPE_DAVx5,
            account: String = "account"
    ): Pair<String, CaldavCalendar> {
        val url = UUIDHelper.newUUID()
        val uri = openTaskDao.taskLists.buildUpon()
                .appendQueryParameter(CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(TaskLists.ACCOUNT_NAME, account)
                .appendQueryParameter(TaskLists.ACCOUNT_TYPE, type)
                .build()
        val result = applyOperation(
                BatchOperation.CpoBuilder.newInsert(uri)
                        .withValue(TaskContract.CommonSyncColumns._SYNC_ID, url)
                        .withValue(TaskLists.SYNC_ENABLED, "1")
        )
        return Pair(result.uri!!.lastPathSegment!!, CaldavCalendar().apply {
            uuid = UUIDHelper.newUUID()
            this.account = "$type:$account"
            this.url = url
            caldavDao.insert(this)
        })
    }

    private fun applyOperation(build: BatchOperation.CpoBuilder): ContentProviderResult =
            context.contentResolver.applyBatch(openTaskDao.authority, arrayListOf(build.build()))[0]

    companion object {
        val VTODO_WITH_START_DATE = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110301//EN
            BEGIN:VTODO
            DTSTAMP:20210121T153032Z
            UID:4586964443060640060
            CREATED:20210121T153000Z
            LAST-MODIFIED:20210121T153029Z
            SUMMARY:Test
            PRIORITY:9
            X-APPLE-SORT-ORDER:-27
            DTSTART;VALUE=DATE:20210121
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val VTODO_NO_START_DATE = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110301//EN
            BEGIN:VTODO
            DTSTAMP:20210121T153032Z
            UID:4586964443060640060
            CREATED:20210121T153000Z
            LAST-MODIFIED:20210121T153029Z
            SUMMARY:Test
            PRIORITY:9
            X-APPLE-SORT-ORDER:-27
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}