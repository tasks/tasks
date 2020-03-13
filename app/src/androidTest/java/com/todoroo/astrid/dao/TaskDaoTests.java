/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.dao;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static org.tasks.makers.TaskMaker.ID;
import static org.tasks.makers.TaskMaker.PARENT;
import static org.tasks.makers.TaskMaker.newTask;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskDeleter;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

@RunWith(AndroidJUnit4.class)
public class TaskDaoTests extends InjectingTestCase {

  @Inject TaskDao taskDao;
  @Inject TaskDeleter taskDeleter;

  /** Test basic task creation, fetch, and save */
  @Test
  public void testTaskCreation() {
    assertEquals(0, taskDao.getAll().size());

    // create task "happy"
    Task task = new Task();
    task.setTitle("happy");
    taskDao.createNew(task);
    assertEquals(1, taskDao.getAll().size());
    long happyId = task.getId();
    assertNotSame(Task.NO_ID, happyId);
    task = taskDao.fetch(happyId);
    assertEquals("happy", task.getTitle());

    // create task "sad"
    task = new Task();
    task.setTitle("sad");
    taskDao.createNew(task);
    assertEquals(2, taskDao.getAll().size());

    // rename sad to melancholy
    long sadId = task.getId();
    assertNotSame(Task.NO_ID, sadId);
    task.setTitle("melancholy");
    taskDao.save(task);
    assertEquals(2, taskDao.getAll().size());

    // check state
    task = taskDao.fetch(happyId);
    assertEquals("happy", task.getTitle());
    task = taskDao.fetch(sadId);
    assertEquals("melancholy", task.getTitle());
  }

  /** Test various task fetch conditions */
  @Test
  public void testTaskConditions() {
    // create normal task
    Task task = new Task();
    task.setTitle("normal");
    taskDao.createNew(task);

    // create blank task
    task = new Task();
    task.setTitle("");
    taskDao.createNew(task);

    // create hidden task
    task = new Task();
    task.setTitle("hidden");
    task.setHideUntil(DateUtilities.now() + 10000);
    taskDao.createNew(task);

    // create task with deadlines
    task = new Task();
    task.setTitle("deadlineInFuture");
    task.setDueDate(DateUtilities.now() + 10000);
    taskDao.createNew(task);

    task = new Task();
    task.setTitle("deadlineInPast");
    task.setDueDate(DateUtilities.now() - 10000);
    taskDao.createNew(task);

    // create completed task
    task = new Task();
    task.setTitle("completed");
    task.setCompletionDate(DateUtilities.now() - 10000);
    taskDao.createNew(task);

    // check is active
    assertEquals(5, taskDao.getActiveTasks().size());

    // check is visible
    assertEquals(5, taskDao.getVisibleTasks().size());
  }

  /** Test task deletion */
  @Test
  public void testTDeletion() {
    assertEquals(0, taskDao.getAll().size());

    // create task "happy"
    Task task = new Task();
    task.setTitle("happy");
    taskDao.createNew(task);
    assertEquals(1, taskDao.getAll().size());

    // delete
    taskDeleter.delete(task);
    assertEquals(0, taskDao.getAll().size());
  }

  /** Test save without prior create doesn't work */
  @Test
  public void testSaveWithoutCreate() {
    // try to save task "happy"
    Task task = new Task();
    task.setTitle("happy");
    task.setId(1L);

    taskDao.save(task);

    assertEquals(0, taskDao.getAll().size());
  }

  /** Test passing invalid task indices to various things */
  @Test
  public void testInvalidIndex() {
    assertEquals(0, taskDao.getAll().size());

    assertNull(taskDao.fetch(1));

    taskDeleter.delete(ImmutableList.of(1L));

    // make sure db still works
    assertEquals(0, taskDao.getAll().size());
  }

  @Test
  @SdkSuppress(minSdkVersion = 21)
  public void findChildrenInList() {
    taskDao.createNew(newTask(with(ID, 1L)));
    taskDao.createNew(newTask(with(ID, 2L), with(PARENT, 1L)));

    assertEquals(singletonList(2L), taskDao.findChildrenInList(Longs.asList(1, 2)));
  }

  @Test
  @SdkSuppress(minSdkVersion = 21)
  public void findRecursiveChildrenInList() {
    taskDao.createNew(newTask(with(ID, 1L)));
    taskDao.createNew(newTask(with(ID, 2L), with(PARENT, 1L)));
    taskDao.createNew(newTask(with(ID, 3L), with(PARENT, 2L)));

    assertEquals(asList(2L, 3L), taskDao.findChildrenInList(Longs.asList(1, 2, 3)));
  }

  @Test
  @SdkSuppress(minSdkVersion = 21)
  public void findRecursiveChildrenInListAfterSkippingParent() {
    taskDao.createNew(newTask(with(ID, 1L)));
    taskDao.createNew(newTask(with(ID, 2L), with(PARENT, 1L)));
    taskDao.createNew(newTask(with(ID, 3L), with(PARENT, 2L)));

    assertEquals(singletonList(3L), taskDao.findChildrenInList(Longs.asList(1, 3)));
  }
  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }

  // TODO check eventing
}
