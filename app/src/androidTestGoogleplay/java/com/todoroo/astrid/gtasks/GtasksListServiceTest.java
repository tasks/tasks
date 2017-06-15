package com.todoroo.astrid.gtasks;

import android.support.test.runner.AndroidJUnit4;

import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.test.DatabaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.Broadcaster;
import org.tasks.data.TaskListDataProvider;
import org.tasks.injection.TestComponent;
import org.tasks.makers.RemoteGtaskListMaker;

import javax.inject.Inject;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.tasks.makers.GtaskListMaker.ID;
import static org.tasks.makers.GtaskListMaker.LAST_SYNC;
import static org.tasks.makers.GtaskListMaker.NAME;
import static org.tasks.makers.GtaskListMaker.REMOTE_ID;
import static org.tasks.makers.GtaskListMaker.SAVED;
import static org.tasks.makers.GtaskListMaker.newGtaskList;
import static org.tasks.makers.RemoteGtaskListMaker.newRemoteList;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

@RunWith(AndroidJUnit4.class)
public class GtasksListServiceTest extends DatabaseTestCase {

    @Inject Database database;
    @Inject TaskListDataProvider taskListDataProvider;
    @Inject TaskDeleter taskDeleter;
    @Inject MetadataDao metadataDao;
    @Inject Broadcaster broadcaster;

    private StoreObjectDao storeObjectDao;
    private GtasksListService gtasksListService;

    @Override
    public void setUp() {
        super.setUp();
        storeObjectDao = spy(new StoreObjectDao(database));
        gtasksListService = new GtasksListService(storeObjectDao, taskListDataProvider, taskDeleter, metadataDao, broadcaster);
    }

    @Override
    protected void inject(TestComponent component) {
        component.inject(this);
    }

    @Test
    public void testCreateNewList() {
        setLists(newRemoteList(
                with(RemoteGtaskListMaker.REMOTE_ID, "1"),
                with(RemoteGtaskListMaker.NAME, "Default")));

        assertEquals(
                newGtaskList(
                        with(ID, 1L),
                        with(REMOTE_ID, "1"),
                        with(NAME, "Default"),
                        with(SAVED, true)),
                storeObjectDao.getGtasksList(1L));
    }

    @Test
    public void testGetListByRemoteId() {
        GtasksList list = newGtaskList(with(REMOTE_ID, "1"));
        storeObjectDao.persist(list);

        assertEquals(list, gtasksListService.getList("1"));
    }

    @Test
    public void testGetListReturnsNullWhenNotFound() {
        assertNull(gtasksListService.getList("1"));
    }

    @Test
    public void testDeleteMissingList() {
        storeObjectDao.persist(newGtaskList(with(REMOTE_ID, "1")));

        setLists(newRemoteList(with(RemoteGtaskListMaker.REMOTE_ID, "2")));

        verify(storeObjectDao).delete(1L);
    }

    @Test
    public void testUpdateListName() {
        storeObjectDao.persist(newGtaskList(
                with(REMOTE_ID, "1"),
                with(NAME, "oldName")));

        setLists(newRemoteList(
                with(RemoteGtaskListMaker.REMOTE_ID, "1"),
                with(RemoteGtaskListMaker.NAME, "newName")));

        assertEquals("newName", storeObjectDao.getGtasksList(1).getName());
    }

    @Test
    public void testNewListLastSyncIsZero() {
        setLists(new TaskList().setId("1"));

        assertEquals(0L, gtasksListService.getList("1").getLastSync());
    }

    @Test
    public void testNewListNeedsUpdate() {
        TaskList taskList = new TaskList().setId("1").setTitle("Default").setUpdated(new DateTime(currentTimeMillis()));

        setLists(taskList);

        assertEquals(
                asList(newGtaskList(with(ID, 1L), with(REMOTE_ID, "1"), with(LAST_SYNC, 0L), with(SAVED, true))),
                gtasksListService.getListsToUpdate(asList(taskList)));
    }

    private void setLists(TaskList... list) {
        gtasksListService.updateLists(asList(list));
    }
}
