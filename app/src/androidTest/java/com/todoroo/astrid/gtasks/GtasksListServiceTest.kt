package com.todoroo.astrid.gtasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.api.services.tasks.model.TaskList
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.service.TaskDeleter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.LocalBroadcastManager
import org.tasks.data.GoogleTaskAccount
import org.tasks.data.GoogleTaskListDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.GtaskListMaker.ID
import org.tasks.makers.GtaskListMaker.NAME
import org.tasks.makers.GtaskListMaker.REMOTE_ID
import org.tasks.makers.GtaskListMaker.newGtaskList
import org.tasks.makers.RemoteGtaskListMaker
import org.tasks.makers.RemoteGtaskListMaker.newRemoteList
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class GtasksListServiceTest : InjectingTestCase() {
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var googleTaskListDao: GoogleTaskListDao

    private lateinit var gtasksListService: GtasksListService

    override fun setUp() {
        super.setUp()
        gtasksListService = GtasksListService(googleTaskListDao, taskDeleter, localBroadcastManager)
    }

    override fun inject(component: TestComponent) = component.inject(this)

    @Test
    fun testCreateNewList() {
        setLists(
                newRemoteList(
                        with(RemoteGtaskListMaker.REMOTE_ID, "1"), with(RemoteGtaskListMaker.NAME, "Default")))
        assertEquals(
                newGtaskList(with(ID, 1L), with(REMOTE_ID, "1"), with(NAME, "Default")),
                googleTaskListDao.getById(1L))
    }

    @Test
    fun testGetListByRemoteId() {
        val list = newGtaskList(with(REMOTE_ID, "1"))
        list.id = googleTaskListDao.insertOrReplace(list)
        assertEquals(list, gtasksListService.getList("1"))
    }

    @Test
    fun testGetListReturnsNullWhenNotFound() {
        assertNull(gtasksListService.getList("1"))
    }

    @Test
    fun testDeleteMissingList() {
        googleTaskListDao.insertOrReplace(newGtaskList(with(ID, 1L), with(REMOTE_ID, "1")))
        val taskList = newRemoteList(with(RemoteGtaskListMaker.REMOTE_ID, "2"))
        setLists(taskList)
        assertEquals(
                listOf(newGtaskList(with(ID, 2L), with(REMOTE_ID, "2"))),
                googleTaskListDao.getLists("account"))
    }

    @Test
    fun testUpdateListName() {
        googleTaskListDao.insertOrReplace(
                newGtaskList(with(ID, 1L), with(REMOTE_ID, "1"), with(NAME, "oldName")))
        setLists(
                newRemoteList(
                        with(RemoteGtaskListMaker.REMOTE_ID, "1"), with(RemoteGtaskListMaker.NAME, "newName")))
        assertEquals("newName", googleTaskListDao.getById(1)!!.title)
    }

    @Test
    fun testNewListLastSyncIsZero() {
        setLists(TaskList().setId("1"))
        assertEquals(0L, gtasksListService.getList("1").lastSync)
    }

    private fun setLists(vararg list: TaskList) {
        val account = GoogleTaskAccount("account")
        googleTaskListDao.insert(account)
        gtasksListService.updateLists(account, listOf(*list))
    }
}