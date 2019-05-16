package org.tasks.data;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.tasks.makers.GoogleTaskMaker.REMOTE_ID;
import static org.tasks.makers.GoogleTaskMaker.TASK;
import static org.tasks.makers.GoogleTaskMaker.newGoogleTask;
import static org.tasks.makers.GtaskListMaker.newGtaskList;
import static org.tasks.makers.TaskMaker.newTask;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

@RunWith(AndroidJUnit4.class)
public class GoogleTaskDaoTests extends InjectingTestCase {

  @Inject GoogleTaskListDao googleTaskListDao;
  @Inject GoogleTaskDao googleTaskDao;
  @Inject TaskDao taskDao;

  @Override
  @Before
  public void setUp() {
    super.setUp();

    googleTaskListDao.insert(newGtaskList());
  }

  @Test
  public void insertAtTopOfEmptyList() {
    insertTop(newGoogleTask(with(REMOTE_ID, "1234")));
    List<GoogleTask> tasks = googleTaskDao.getByLocalOrder("1");
    assertEquals(1, tasks.size());
    GoogleTask task = tasks.get(0);
    assertEquals("1234", task.getRemoteId());
    assertEquals(0, task.getOrder());
  }

  @Test
  public void insertAtBottomOfEmptyList() {
    insertBottom(newGoogleTask(with(REMOTE_ID, "1234")));
    List<GoogleTask> tasks = googleTaskDao.getByLocalOrder("1");
    assertEquals(1, tasks.size());
    GoogleTask task = tasks.get(0);
    assertEquals("1234", task.getRemoteId());
    assertEquals(0, task.getOrder());
  }

  @Test
  public void getPreviousIsNullForTopTask() {
    googleTaskDao.insertAndShift(newGoogleTask(), true);
    assertNull(googleTaskDao.getPrevious("1", 0, 0));
  }

  @Test
  public void getPrevious() {
    insertTop(newGoogleTask());
    insertTop(newGoogleTask(with(REMOTE_ID, "1234")));

    assertEquals("1234", googleTaskDao.getPrevious("1", 0, 1));
  }

  @Test
  public void insertAtTopOfList() {
    insertTop(newGoogleTask(with(REMOTE_ID, "1234")));
    insertTop(newGoogleTask(with(REMOTE_ID, "5678")));

    List<GoogleTask> tasks = googleTaskDao.getByLocalOrder("1");
    assertEquals(2, tasks.size());
    GoogleTask top = tasks.get(0);
    assertEquals("5678", top.getRemoteId());
    assertEquals(0, top.getOrder());
  }

  @Test
  public void insertAtTopOfListShiftsExisting() {
    insertTop(newGoogleTask(with(REMOTE_ID, "1234")));
    insertTop(newGoogleTask(with(REMOTE_ID, "5678")));

    List<GoogleTask> tasks = googleTaskDao.getByLocalOrder("1");
    assertEquals(2, tasks.size());
    GoogleTask bottom = tasks.get(1);
    assertEquals("1234", bottom.getRemoteId());
    assertEquals(1, bottom.getOrder());
  }

  @Test
  public void getTaskFromRemoteId() {
    googleTaskDao.insert(newGoogleTask(with(REMOTE_ID, "1234"), with(TASK, 4)));
    assertEquals(4, googleTaskDao.getTask("1234"));
  }

  @Test
  public void getRemoteIdForTask() {
    googleTaskDao.insert(newGoogleTask(with(REMOTE_ID, "1234"), with(TASK, 4)));
    assertEquals("1234", googleTaskDao.getRemoteId(4L));
  }

  @Test
  public void moveDownInList() {
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "1")), false);
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "2")), false);
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "3")), false);

    GoogleTask two = googleTaskDao.getByRemoteId("2");

    googleTaskDao.move(two, 0, 0);

    assertEquals(0, googleTaskDao.getByRemoteId("2").getOrder());
    assertEquals(1, googleTaskDao.getByRemoteId("1").getOrder());
    assertEquals(2, googleTaskDao.getByRemoteId("3").getOrder());
  }

  @Test
  public void moveUpInList() {
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "1")), false);
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "2")), false);
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "3")), false);

    GoogleTask one = googleTaskDao.getByRemoteId("1");

    googleTaskDao.move(one, 0, 1);

    assertEquals(0, googleTaskDao.getByRemoteId("2").getOrder());
    assertEquals(1, googleTaskDao.getByRemoteId("1").getOrder());
    assertEquals(2, googleTaskDao.getByRemoteId("3").getOrder());
  }

  @Test
  public void moveToTop() {
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "1")), false);
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "2")), false);
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "3")), false);

    GoogleTask three = googleTaskDao.getByRemoteId("3");

    googleTaskDao.move(three, 0, 0);

    assertEquals(0, googleTaskDao.getByRemoteId("3").getOrder());
    assertEquals(1, googleTaskDao.getByRemoteId("1").getOrder());
    assertEquals(2, googleTaskDao.getByRemoteId("2").getOrder());
  }

  @Test
  public void moveToBottom() {
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "1")), false);
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "2")), false);
    googleTaskDao.insertAndShift(newGoogleTask(with(REMOTE_ID, "3")), false);

    GoogleTask one = googleTaskDao.getByRemoteId("1");

    googleTaskDao.move(one, 0, 2);

    assertEquals(0, googleTaskDao.getByRemoteId("2").getOrder());
    assertEquals(1, googleTaskDao.getByRemoteId("3").getOrder());
    assertEquals(2, googleTaskDao.getByRemoteId("1").getOrder());
  }

  private void insertTop(GoogleTask googleTask) {
    insert(googleTask, true);
  }

  private void insertBottom(GoogleTask googleTask) {
    insert(googleTask, false);
  }

  private void insert(GoogleTask googleTask, boolean top) {
    Task task = newTask();
    taskDao.createNew(task);
    googleTask.setTask(task.getId());
    googleTaskDao.insertAndShift(googleTask, top);
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
