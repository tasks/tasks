package com.todoroo.astrid.gtasks

import com.google.api.services.tasks.model.TaskList
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.RemoteGtaskListMaker
import org.tasks.makers.RemoteGtaskListMaker.newRemoteList
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class GtasksListServiceTest : InjectingTestCase() {
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var caldavDao: CaldavDao

    private lateinit var gtasksListService: GtasksListService

    @Before
    override fun setUp() {
        super.setUp()
        gtasksListService = GtasksListService(caldavDao, taskDeleter, localBroadcastManager)
    }

    @Test
    fun testCreateNewList() = runBlocking {
        setLists(
                newRemoteList(
                        with(RemoteGtaskListMaker.REMOTE_ID, "1"), with(RemoteGtaskListMaker.NAME, "Default")))
        assertEquals(
            CaldavCalendar(id = 1, account = "account", uuid = "1", name = "Default"),
            caldavDao.getCalendarById(1L)
        )
    }

    @Test
    fun testGetListByRemoteId() = runBlocking {
        val list = CaldavCalendar(uuid = "1")
        caldavDao.insert(list)
        assertEquals(list, caldavDao.getCalendarByUuid("1"))
    }

    @Test
    fun testGetListReturnsNullWhenNotFound() = runBlocking {
        assertNull(caldavDao.getCalendarByUuid("1"))
    }

    @Test
    fun testDeleteMissingList() = runBlocking {
        caldavDao.insert(CaldavCalendar(account = "account", uuid = "1"))
        val taskList = newRemoteList(with(RemoteGtaskListMaker.REMOTE_ID, "2"))
        setLists(taskList)
        assertEquals(
            listOf(CaldavCalendar(id = 2, account = "account", uuid = "2", name = "Default")),
            caldavDao.getCalendarsByAccount("account")
        )
    }

    @Test
    fun testUpdateListName() = runBlocking {
        val calendar = CaldavCalendar(uuid = "1", name = "oldName", account = "account")
        caldavDao.insert(calendar)
        setLists(
                newRemoteList(
                        with(RemoteGtaskListMaker.REMOTE_ID, "1"), with(RemoteGtaskListMaker.NAME, "newName")))
        assertEquals("newName", caldavDao.getCalendarById(calendar.id)!!.name)
    }

    @Test
    fun testNewListLastSyncIsZero() = runBlocking {
        setLists(TaskList().setId("1"))
        assertEquals(0L, caldavDao.getCalendarByUuid("1")!!.lastSync)
    }

    private suspend fun setLists(vararg list: TaskList) {
        val account = CaldavAccount(
            username = "account",
            uuid = "account",
        )
        caldavDao.insert(account)
        gtasksListService.updateLists(account, listOf(*list))
    }
}