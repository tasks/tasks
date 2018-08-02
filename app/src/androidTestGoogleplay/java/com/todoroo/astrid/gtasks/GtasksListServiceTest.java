package com.todoroo.astrid.gtasks;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.tasks.makers.GtaskListMaker.ID;
import static org.tasks.makers.GtaskListMaker.LAST_SYNC;
import static org.tasks.makers.GtaskListMaker.NAME;
import static org.tasks.makers.GtaskListMaker.REMOTE_ID;
import static org.tasks.makers.GtaskListMaker.newGtaskList;
import static org.tasks.makers.RemoteGtaskListMaker.newRemoteList;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.support.test.runner.AndroidJUnit4;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.service.TaskDeleter;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.LocalBroadcastManager;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.jobs.WorkManager;
import org.tasks.makers.RemoteGtaskListMaker;

@RunWith(AndroidJUnit4.class)
public class GtasksListServiceTest extends InjectingTestCase {

  @Inject TaskDeleter taskDeleter;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject GoogleTaskDao googleTaskDao;
  @Inject TaskDao taskDao;
  @Inject WorkManager workManager;

  @Inject GoogleTaskListDao googleTaskListDao;
  private GtasksListService gtasksListService;

  @Override
  public void setUp() {
    super.setUp();
    workManager.init();
    gtasksListService =
        new GtasksListService(
            googleTaskListDao, taskDeleter, localBroadcastManager, googleTaskDao, taskDao);
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }

  @Test
  public void testCreateNewList() {
    setLists(
        newRemoteList(
            with(RemoteGtaskListMaker.REMOTE_ID, "1"), with(RemoteGtaskListMaker.NAME, "Default")));

    assertEquals(
        newGtaskList(with(ID, 1L), with(REMOTE_ID, "1"), with(NAME, "Default")),
        googleTaskListDao.getById(1L));
  }

  @Test
  public void testGetListByRemoteId() {
    GoogleTaskList list = newGtaskList(with(REMOTE_ID, "1"));
    list.setId(googleTaskListDao.insertOrReplace(list));

    assertEquals(list, gtasksListService.getList("1"));
  }

  @Test
  public void testGetListReturnsNullWhenNotFound() {
    assertNull(gtasksListService.getList("1"));
  }

  @Test
  public void testDeleteMissingList() {
    googleTaskListDao.insertOrReplace(newGtaskList(with(ID, 1L), with(REMOTE_ID, "1")));

    TaskList taskList = newRemoteList(with(RemoteGtaskListMaker.REMOTE_ID, "2"));

    setLists(taskList);

    assertEquals(
        singletonList(newGtaskList(with(ID, 2L), with(REMOTE_ID, "2"))),
        googleTaskListDao.getLists("account"));
  }

  @Test
  public void testUpdateListName() {
    googleTaskListDao.insertOrReplace(
        newGtaskList(with(ID, 1L), with(REMOTE_ID, "1"), with(NAME, "oldName")));

    setLists(
        newRemoteList(
            with(RemoteGtaskListMaker.REMOTE_ID, "1"), with(RemoteGtaskListMaker.NAME, "newName")));

    assertEquals("newName", googleTaskListDao.getById(1).getTitle());
  }

  @Test
  public void testNewListLastSyncIsZero() {
    setLists(new TaskList().setId("1"));

    assertEquals(0L, gtasksListService.getList("1").getLastSync());
  }

  @Test
  public void testNewListNeedsUpdate() {
    TaskList taskList =
        new TaskList().setId("1").setTitle("Default").setUpdated(new DateTime(currentTimeMillis()));

    setLists(taskList);

    assertEquals(
        singletonList(newGtaskList(with(ID, 1L), with(REMOTE_ID, "1"), with(LAST_SYNC, 0L))),
        gtasksListService.getListsToUpdate(singletonList(taskList)));
  }

  private void setLists(TaskList... list) {
    GoogleTaskAccount account = new GoogleTaskAccount("account");
    googleTaskListDao.insert(account);
    gtasksListService.updateLists(account, asList(list));
  }
}
