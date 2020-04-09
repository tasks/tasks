package com.todoroo.astrid.sync;

import static org.junit.Assert.assertNotEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.todoroo.astrid.data.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.data.TagData;

@RunWith(AndroidJUnit4.class)
public class SyncModelTest extends NewSyncTestCase {

  @Test
  public void testCreateTaskMakesUuid() {
    Task task = createTask();
    assertNotEquals(Task.NO_UUID, task.getUuid());
  }

  @Test
  public void testCreateTagMakesUuid() {
    TagData tag = createTagData();
    assertNotEquals(Task.NO_UUID, tag.getRemoteId());
  }
}
