package org.tasks.opentasks

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TestUtilities
import org.tasks.injection.ProductionModule
import org.tasks.makers.CaldavTaskMaker
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import java.util.*

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class OpenTasksDueDateTests : OpenTasksTest() {

    @Test
    fun readDueDatePositiveOffset() = runBlocking {
        val (_, list) = withVtodo(ALL_DAY_DUE)

        TestUtilities.withTZ(BERLIN) {
            synchronizer.sync()
        }

        val caldavTask = caldavDao.getTaskByRemoteId(list.uuid!!, "3863299529704302692")
        val task = taskDao.fetch(caldavTask!!.task)
        assertEquals(
                DateTime(2021, 2, 1, 12, 0, 0, 0, BERLIN).millis,
                task?.dueDate
        )
    }

    @Test
    fun writeDueDatePositiveOffset() = TestUtilities.withTZ(BERLIN) {
        val (listId, list) = openTaskDao.insertList()
        val taskId = taskDao.createNew(newTask(
                with(TaskMaker.DUE_DATE, DateTime(2021, 2, 1))
        ))
        caldavDao.insert(newCaldavTask(
                with(CaldavTaskMaker.CALENDAR, list.uuid),
                with(CaldavTaskMaker.REMOTE_ID, "1234"),
                with(CaldavTaskMaker.TASK, taskId)
        ))

        synchronizer.sync()

        assertEquals(
                1612137600000,
                openTaskDao.getTask(listId.toLong(), "1234")?.task?.due?.date?.time
        )
    }

    @Test
    fun readDueDateNoOffset() = runBlocking {
        val (_, list) = withVtodo(ALL_DAY_DUE)

        TestUtilities.withTZ(LONDON) {
            synchronizer.sync()
        }

        val caldavTask = caldavDao.getTaskByRemoteId(list.uuid!!, "3863299529704302692")
        val task = taskDao.fetch(caldavTask!!.task)
        assertEquals(
                DateTime(2021, 2, 1, 12, 0, 0, 0, LONDON).millis,
                task?.dueDate
        )
    }

    @Test
    fun writeDueDateNoOffset() = TestUtilities.withTZ(LONDON) {
        val (listId, list) = openTaskDao.insertList()
        val taskId = taskDao.createNew(newTask(
                with(TaskMaker.DUE_DATE, DateTime(2021, 2, 1))
        ))
        caldavDao.insert(newCaldavTask(
                with(CaldavTaskMaker.CALENDAR, list.uuid),
                with(CaldavTaskMaker.REMOTE_ID, "1234"),
                with(CaldavTaskMaker.TASK, taskId)
        ))

        synchronizer.sync()

        assertEquals(
                1612137600000,
                openTaskDao.getTask(listId.toLong(), "1234")?.task?.due?.date?.time
        )
    }

    @Test
    fun readDueDateNegativeOffset() = runBlocking {
        val (_, list) = withVtodo(ALL_DAY_DUE)

        TestUtilities.withTZ(NEW_YORK) {
            synchronizer.sync()
        }

        val caldavTask = caldavDao.getTaskByRemoteId(list.uuid!!, "3863299529704302692")
        val task = taskDao.fetch(caldavTask!!.task)
        assertEquals(
                DateTime(2021, 2, 1, 12, 0, 0, 0, NEW_YORK).millis,
                task?.dueDate
        )
    }

    @Test
    fun writeDueDateNegativeOffset() = TestUtilities.withTZ(NEW_YORK) {
        val (listId, list) = openTaskDao.insertList()
        val taskId = taskDao.createNew(newTask(
                with(TaskMaker.DUE_DATE, DateTime(2021, 2, 1))
        ))
        caldavDao.insert(newCaldavTask(
                with(CaldavTaskMaker.CALENDAR, list.uuid),
                with(CaldavTaskMaker.REMOTE_ID, "1234"),
                with(CaldavTaskMaker.TASK, taskId)
        ))

        synchronizer.sync()

        assertEquals(
                1612137600000,
                openTaskDao.getTask(listId.toLong(), "1234")?.task?.due?.date?.time
        )
    }

    companion object {
        private val BERLIN = TimeZone.getTimeZone("Europe/Berlin")
        private val LONDON = TimeZone.getTimeZone("Europe/London")
        private val NEW_YORK = TimeZone.getTimeZone("America/New_York")

        private val ALL_DAY_DUE = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110304//EN
            BEGIN:VTODO
            DTSTAMP:20210129T155402Z
            UID:3863299529704302692
            CREATED:20210129T155318Z
            LAST-MODIFIED:20210129T155329Z
            SUMMARY:Due date
            DUE;VALUE=DATE:20210201
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}