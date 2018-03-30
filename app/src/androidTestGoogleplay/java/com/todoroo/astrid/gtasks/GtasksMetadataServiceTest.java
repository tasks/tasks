/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.preferences.Preferences;

@SuppressWarnings("nls")
@RunWith(AndroidJUnit4.class)
public class GtasksMetadataServiceTest extends InjectingTestCase {

  @Inject TaskDao taskDao;
  @Inject GoogleTaskDao googleTaskDao;
  private Task task;
  private GoogleTask metadata;

  @Override
  protected void inject(TestComponent component) {
    component.plus(new GtasksMetadataServiceTestModule(getTargetContext())).inject(this);
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

  @Subcomponent(modules = GtasksMetadataServiceTest.GtasksMetadataServiceTestModule.class)
  public interface GtasksMetadataServiceTestComponent {

    void inject(GtasksMetadataServiceTest gtasksMetadataServiceTest);
  }

  @Module
  public class GtasksMetadataServiceTestModule {

    private final GtasksTestPreferenceService service;

    public GtasksMetadataServiceTestModule(Context context) {
      service = new GtasksTestPreferenceService(new Preferences(context, null));
    }

    @Provides
    public GtasksTestPreferenceService getGtasksTestPreferenceService() {
      return service;
    }

    @Provides
    public GtasksPreferenceService getGtasksPreferenceService() {
      return service;
    }
  }
}
