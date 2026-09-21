package org.tasks.opentasks

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.TASK as CALDAV_TASK
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.DESCRIPTION
import org.tasks.makers.TaskMaker.TITLE
import org.tasks.makers.TaskMaker.newTask

@HiltAndroidTest
class OpenTasksMergeTest : OpenTasksTest() {

    @Test
    fun coldConflictKeepsLocalEdits() = runBlocking {
        // No initial sync, so no base is cached.
        val (listId, list) = openTaskDao.insertList()
        openTaskDao.insertTask(listId, TASK)
        val taskId = taskDao.createNew(newTask(
                with(TITLE, "local title"),
                with(DESCRIPTION, "local notes"),
        ))
        // Linking the task to a caldav list marks it dirty via the task_dirty_after_insert trigger.
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, UID),
                with(CALDAV_TASK, taskId),
        ))

        openTaskDao.setDescription(listId, UID, "remote notes") // concurrent remote edit

        synchronizer.sync(hasPro = true)

        // Local wins; the remote edit is dropped this round.
        val merged = taskDao.fetch(taskId)!!
        assertEquals("local title", merged.title)
        assertEquals("local notes", merged.notes)

        val provider = openTaskDao.getTask(listId, UID)!!.task!!
        assertEquals("local title", provider.summary)
        assertEquals("local notes", provider.description)
    }

    @Test
    fun unknownPropertyIsNotClobberedByMerge() = runBlocking {
        val (listId, list) = withVtodo(TASK_WITH_X_PROP)
        synchronizer.sync(hasPro = true)

        val caldavTask = caldavDao.getTaskByRemoteId(list.uuid!!, UID)!!
        val local = taskDao.fetch(caldavTask.task)!!
        local.title = "local title"
        taskDao.update(local, markDirty = true)

        synchronizer.sync(hasPro = true)

        val provider = openTaskDao.getTask(listId, UID)!!.task!!
        assertEquals("local title", provider.summary)
        assertEquals(
            "bar",
            provider.unknownProperties.find { it.name == "X-CUSTOM-PROP" }?.value
        )
    }

    companion object {
        private const val UID = "1234"
        private val TASK = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110304//EN
            BEGIN:VTODO
            DTSTAMP:20210201T204211Z
            UID:1234
            CREATED:20210201T204143Z
            LAST-MODIFIED:20210201T204209Z
            SUMMARY:original title
            DESCRIPTION:original notes
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        private val TASK_WITH_X_PROP = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110304//EN
            BEGIN:VTODO
            DTSTAMP:20210201T204211Z
            UID:1234
            CREATED:20210201T204143Z
            LAST-MODIFIED:20210201T204209Z
            SUMMARY:original title
            DESCRIPTION:original notes
            X-CUSTOM-PROP:bar
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}
