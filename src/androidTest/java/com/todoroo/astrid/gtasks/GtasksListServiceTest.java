package com.todoroo.astrid.gtasks;

import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.test.DatabaseTestCase;

import org.tasks.makers.RemoteGtaskListMaker;

import javax.inject.Inject;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.makers.GtaskListMaker.LAST_SYNC;
import static org.tasks.makers.GtaskListMaker.NAME;
import static org.tasks.makers.GtaskListMaker.REMOTE_ID;
import static org.tasks.makers.GtaskListMaker.newGtaskList;
import static org.tasks.makers.RemoteGtaskListMaker.newRemoteList;

public class GtasksListServiceTest extends DatabaseTestCase {

    @Inject Database database;

    private StoreObjectDao storeObjectDao;
    private GtasksListService gtasksListService;

    @Override
    public void setUp() {
        super.setUp();
        storeObjectDao = spy(new StoreObjectDao(database));
        gtasksListService = new GtasksListService(storeObjectDao);
    }

    public void testCreateNewList() {
        setLists(newRemoteList(
                with(RemoteGtaskListMaker.REMOTE_ID, "1"),
                with(RemoteGtaskListMaker.NAME, "Default")));

        verify(storeObjectDao).persist(newGtaskList(
                with(REMOTE_ID, "1"),
                with(NAME, "Default")));
    }

    public void testGetListByRemoteId() {
        GtasksList list = newGtaskList(with(REMOTE_ID, "1"));
        storeObjectDao.createNew(list.getStoreObject());

        assertEquals(list, gtasksListService.getList("1"));
    }

    public void testGetListReturnsNullWhenNotFound() {
        assertNull(gtasksListService.getList("1"));
    }

    public void testDeleteMissingList() {
        storeObjectDao.createNew(newGtaskList(with(REMOTE_ID, "1")).getStoreObject());

        setLists(newRemoteList(with(RemoteGtaskListMaker.REMOTE_ID, "2")));

        verify(storeObjectDao).delete(1L);
    }

    public void testUpdateListName() {
        storeObjectDao.createNew(newGtaskList(
                with(REMOTE_ID, "1"),
                with(NAME, "oldName")).getStoreObject());

        setLists(newRemoteList(
                with(RemoteGtaskListMaker.REMOTE_ID, "1"),
                with(RemoteGtaskListMaker.NAME, "newName")));

        assertEquals("newName", storeObjectDao.getGtasksList(1).getName());
    }

    public void testNewListLastSyncIsZero() {
        setLists(new TaskList().setId("1"));

        assertEquals(0L, gtasksListService.getList("1").getLastSync());
    }

    public void testNewListNeedsUpdate() {
        TaskList taskList = new TaskList().setId("1").setTitle("Default").setUpdated(new DateTime(currentTimeMillis()));

        setLists(taskList);

        assertEquals(
                asList(newGtaskList(with(REMOTE_ID, "1"), with(LAST_SYNC, 0L))),
                gtasksListService.getListsToUpdate(new TaskLists().setItems(asList(taskList))));
    }

    private void setLists(TaskList... list) {
        gtasksListService.updateLists(new TaskLists().setItems(
                asList(list)));
    }
}
