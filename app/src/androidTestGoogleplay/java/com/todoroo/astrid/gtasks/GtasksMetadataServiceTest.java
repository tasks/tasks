/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import android.support.test.runner.AndroidJUnit4;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

@SuppressWarnings("nls")
@RunWith(AndroidJUnit4.class)
public class GtasksMetadataServiceTest extends InjectingTestCase {

  @Inject TaskDao taskDao;
  @Inject GoogleTaskDao googleTaskDao;
  private Task task;
  private GoogleTask metadata;

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }

  @Test
  public void testMetadataFound() {
    givenTask(taskWithMetadata(null));

    whenSearchForMetadata();

    thenExpectMetadataFound();
  }

  @Test
  public void testMetadataDoesntExist() {
    givenTask(taskWithoutMetadata());

    whenSearchForMetadata();

    thenExpectNoMetadataFound();
  }

  private void thenExpectNoMetadataFound() {
    assertNull(metadata);
  }

  private void thenExpectMetadataFound() {
    assertNotNull(metadata);
  }

  // --- helpers

  private void whenSearchForMetadata() {
    metadata = googleTaskDao.getByTaskId(task.getId());
  }

  private Task taskWithMetadata(String id) {
    Task task = new Task();
    task.setTitle("cats");
    taskDao.createNew(task);
    GoogleTask metadata = new GoogleTask(task.getId(), "");
    if (id != null) {
      metadata.setRemoteId(id);
    }
    metadata.setTask(task.getId());
    googleTaskDao.insert(metadata);
    return task;
  }

  private void givenTask(Task taskToTest) {
    task = taskToTest;
  }

  private Task taskWithoutMetadata() {
    Task task = new Task();
    task.setTitle("dogs");
    taskDao.createNew(task);
    return task;
  }
}
