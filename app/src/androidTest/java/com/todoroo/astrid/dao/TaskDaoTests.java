/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;

import android.support.test.runner.AndroidJUnit4;
import com.google.common.collect.ImmutableList;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskDeleter;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.jobs.WorkManager;

@RunWith(AndroidJUnit4.class)
public class TaskDaoTests extends InjectingTestCase {

  @Inject TaskDao taskDao;
  @Inject TaskDeleter taskDeleter;
  @Inject WorkManager workManager;

  @Override
  public void setUp() {
    super.setUp();
    workManager.init();
  }

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

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }

  // TODO check eventing
}
