package com.todoroo.astrid.service;

import static com.google.common.collect.FluentIterable.from;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.tasks.makers.CaldavTaskMaker.CALENDAR;
import static org.tasks.makers.CaldavTaskMaker.REMOTE_ID;
import static org.tasks.makers.CaldavTaskMaker.REMOTE_PARENT;
import static org.tasks.makers.CaldavTaskMaker.newCaldavTask;
import static org.tasks.makers.GoogleTaskMaker.LIST;
import static org.tasks.makers.GoogleTaskMaker.PARENT;
import static org.tasks.makers.GoogleTaskMaker.TASK;
import static org.tasks.makers.GoogleTaskMaker.newGoogleTask;
import static org.tasks.makers.GtaskListMaker.newGtaskList;
import static org.tasks.makers.TaskMaker.ID;
import static org.tasks.makers.TaskMaker.newTask;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import com.google.common.primitives.Longs;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.jobs.WorkManager;
import org.tasks.makers.CaldavTaskMaker;
import org.tasks.makers.GtaskListMaker;
import org.tasks.makers.TaskMaker;

@RunWith(AndroidJUnit4.class)
public class TaskMoverTest extends InjectingTestCase {

  @Inject TaskDao taskDao;
  @Inject GoogleTaskDao googleTaskDao;
  @Inject WorkManager workManager;
  @Inject CaldavDao caldavDao;
  @Inject TaskMover taskMover;

  @Before
  public void setUp() {
    super.setUp();
    taskDao.initialize(workManager);
  }

  @Test
  public void moveBetweenGoogleTaskLists() {
    createTasks(1);
    googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")));

    moveToGoogleTasks("2", 1);

    assertEquals("2", googleTaskDao.getByTaskId(1).getListId());
  }

  @Test
  public void deleteGoogleTaskAfterMove() {
    createTasks(1);
    googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")));

    moveToGoogleTasks("2", 1);

    List<GoogleTask> deleted = googleTaskDao.getDeletedByTaskId(1);
    assertEquals(1, deleted.size());
    assertEquals(1, deleted.get(0).getTask());
    assertTrue(deleted.get(0).getDeleted() > 0);
  }

  @Test
  public void moveChildrenBetweenGoogleTaskLists() {
    createTasks(1, 2);
    googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")));
    googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)));

    moveToGoogleTasks("2", 1);

    List<GoogleTask> deleted = googleTaskDao.getDeletedByTaskId(2);
    assertEquals(1, deleted.size());
    assertEquals(2, deleted.get(0).getTask());
    assertTrue(deleted.get(0).getDeleted() > 0);

    GoogleTask task = googleTaskDao.getByTaskId(2);
    assertEquals(1, task.getParent());
    assertEquals("2", task.getListId());
  }

  @Test
  public void moveBetweenCaldavList() {
    createTasks(1);
    caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")));

    moveToCaldavList("2", 1);

    assertEquals("2", caldavDao.getTask(1).getCalendar());
  }

  @Test
  public void deleteCaldavTaskAfterMove() {
    createTasks(1);
    caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")));

    moveToCaldavList("2", 1);

    List<CaldavTask> deleted = caldavDao.getDeleted("1");
    assertEquals(1, deleted.size());
    assertEquals(1, deleted.get(0).getTask());
    assertTrue(deleted.get(0).getDeleted() > 0);
  }

  @Test
  @SdkSuppress(minSdkVersion = 21)
  public void moveRecursiveCaldavChildren() {
    createTasks(1);
    createSubtask(2, 1);
    createSubtask(3, 2);
    caldavDao.insert(
        asList(
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 2L),
                with(CALENDAR, "1"),
                with(REMOTE_ID, "b"),
                with(REMOTE_PARENT, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 3L),
                with(CALENDAR, "1"),
                with(REMOTE_PARENT, "b"))));

    moveToCaldavList("2", 1);

    List<CaldavTask> deleted = caldavDao.getDeleted("1");
    assertEquals(3, deleted.size());
    CaldavTask task = caldavDao.getTask(3);
    assertEquals("2", task.getCalendar());
    assertEquals(2, taskDao.fetch(3).getParent());
  }

  @Test
  public void moveGoogleTaskChildrenToCaldav() {
    createTasks(1, 2);
    googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")));
    googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)));

    moveToCaldavList("1", 1);

    CaldavTask task = caldavDao.getTask(2);
    assertEquals("1", task.getCalendar());
    assertEquals(1, taskDao.fetch(2).getParent());
  }

  @Test
  @SdkSuppress(minSdkVersion = 21)
  public void flattenLocalSubtasksWhenMovingToGoogleTasks() {
    createTasks(1);
    createSubtask(2, 1);
    createSubtask(3, 2);

    moveToGoogleTasks("1", 1);

    assertEquals(1, googleTaskDao.getByTaskId(3).getParent());
    assertEquals(0, taskDao.fetch(3).getParent());
  }

  @Test
  public void moveLocalChildToGoogleTasks() {
    createTasks(1);
    createSubtask(2, 1);

    moveToGoogleTasks("1", 2);

    assertEquals(0, taskDao.fetch(2).getParent());
  }

  @Test
  public void moveLocalChildToCaldav() {
    createTasks(1);
    createSubtask(2, 1);

    moveToCaldavList("1", 2);

    assertEquals(0, taskDao.fetch(2).getParent());
  }

  @Test
  @SdkSuppress(minSdkVersion = 21)
  public void flattenCaldavSubtasksWhenMovingToGoogleTasks() {
    createTasks(1);
    createSubtask(2, 1);
    createSubtask(3, 2);
    caldavDao.insert(
        asList(
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 2L),
                with(CALENDAR, "1"),
                with(REMOTE_ID, "b"),
                with(REMOTE_PARENT, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 3L),
                with(CALENDAR, "1"),
                with(REMOTE_PARENT, "b"))));

    moveToGoogleTasks("1", 1);

    GoogleTask task = googleTaskDao.getByTaskId(3L);
    assertEquals(1, task.getParent());
  }

  @Test
  public void moveGoogleTaskChildWithoutParent() {
    createTasks(1, 2);
    googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")));
    googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)));

    moveToGoogleTasks("2", 2);

    GoogleTask task = googleTaskDao.getByTaskId(2);
    assertEquals(0L, task.getParent());
    assertEquals("2", task.getListId());
  }

  @Test
  public void moveCaldavChildWithoutParent() {
    createTasks(1);
    createSubtask(2, 1);
    caldavDao.insert(
        asList(
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 2L),
                with(CALENDAR, "1"),
                with(REMOTE_PARENT, "a"))));

    moveToCaldavList("2", 2);

    assertEquals("2", caldavDao.getTask(2).getCalendar());
    assertEquals(0, taskDao.fetch(2).getParent());
  }

  @Test
  public void moveGoogleTaskToCaldav() {
    createTasks(1);
    googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")));

    moveToCaldavList("2", 1);

    assertEquals("2", caldavDao.getTask(1).getCalendar());
  }

  @Test
  public void moveCaldavToGoogleTask() {
    createTasks(1);
    caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")));

    moveToGoogleTasks("2", 1);

    assertEquals("2", googleTaskDao.getByTaskId(1L).getListId());
  }

  @Test
  public void moveLocalToCaldav() {
    createTasks(1);
    createSubtask(2, 1);
    createSubtask(3, 2);

    moveToCaldavList("1", 1);

    assertEquals("1", caldavDao.getTask(3).getCalendar());
    assertEquals(2, taskDao.fetch(3).getParent());
  }

  @Test
  public void dontSyncGoogleTask() {
    createTasks(1);
    googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")));

    dontSync(1);

    assertNull(googleTaskDao.getByTaskId(1));
    assertFalse(taskDao.fetch(1).isDeleted());
  }

  @Test
  public void dontSyncCaldavTask() {
    createTasks(1);
    caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")));

    dontSync(1);

    assertNull(caldavDao.getTask(1));
    assertFalse(taskDao.fetch(1).isDeleted());
  }

  @Test
  public void dontSyncGoogleTaskWithSubtasks() {
    createTasks(1, 2);
    googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")));
    googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)));

    dontSync(1);

    assertNull(googleTaskDao.getByTaskId(2));
    Task task = taskDao.fetch(2);
    assertFalse(task.isDeleted());
    assertEquals(1, task.getParent());
    assertEquals(taskDao.fetch(1).getUuid(), task.getParentUuid());
  }

  @Test
  public void dontSyncCaldavWithSubtasks() {
    createTasks(1);
    createSubtask(2, 1);
    createSubtask(3, 2);
    caldavDao.insert(
        asList(
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 2L),
                with(CALENDAR, "1"),
                with(REMOTE_ID, "b"),
                with(REMOTE_PARENT, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 3L),
                with(CALENDAR, "1"),
                with(REMOTE_PARENT, "b"))));

    dontSync(1);

    assertNull(caldavDao.getTask(3));
    Task task = taskDao.fetch(3);
    assertFalse(task.isDeleted());
    assertEquals(2, task.getParent());
    assertEquals(taskDao.fetch(2).getUuid(), task.getParentUuid());
  }

  @Test
  public void moveToSameGoogleTaskListIsNoop() {
    createTasks(1);
    googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")));

    moveToGoogleTasks("1", 1);

    assertTrue(googleTaskDao.getDeletedByTaskId(1).isEmpty());
    assertEquals(1, googleTaskDao.getAllByTaskId(1).size());
  }

  @Test
  public void moveToSameCaldavListIsNoop() {
    createTasks(1);
    caldavDao.insert(newCaldavTask(with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1")));

    moveToCaldavList("1", 1);

    assertTrue(caldavDao.getDeleted("1").isEmpty());
    assertEquals(1, caldavDao.getTasks(1).size());
  }

  @Test
  public void dontDuplicateWhenParentAndChildGoogleTaskMoved() {
    createTasks(1, 2);
    googleTaskDao.insert(newGoogleTask(with(TASK, 1), with(LIST, "1")));
    googleTaskDao.insert(newGoogleTask(with(TASK, 2), with(LIST, "1"), with(PARENT, 1L)));

    moveToGoogleTasks("2", 1, 2);

    assertEquals(1, from(googleTaskDao.getAllByTaskId(2)).filter(t -> t.getDeleted() == 0).size());
  }

  @Test
  public void dontDuplicateWhenParentAndChildCaldavMoved() {
    createTasks(1);
    createSubtask(2, 1);
    caldavDao.insert(
        asList(
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 2L),
                with(CALENDAR, "1"),
                with(REMOTE_PARENT, "a"))));

    moveToCaldavList("2", 1, 2);

    assertEquals(1, from(caldavDao.getTasks(2)).filter(t -> t.getDeleted() == 0).size());
  }

  private void createTasks(long... ids) {
    for (long id : ids) {
      taskDao.createNew(newTask(with(ID, id)));
    }
  }

  private void createSubtask(long id, long parent) {
    taskDao.createNew(newTask(with(ID, id), with(TaskMaker.PARENT, parent)));
  }

  private void moveToGoogleTasks(String list, long... tasks) {
    taskMover.move(
        Longs.asList(tasks), new GtasksFilter(newGtaskList(with(GtaskListMaker.REMOTE_ID, list))));
  }

  private void moveToCaldavList(String calendar, long... tasks) {
    taskMover.move(Longs.asList(tasks), new CaldavFilter(new CaldavCalendar("", calendar)));
  }

  private void dontSync(long task) {
    taskMover.move(singletonList(task), null);
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
