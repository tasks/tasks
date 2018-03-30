package com.todoroo.astrid.subtasks;

import static junit.framework.Assert.assertEquals;

import android.support.test.runner.AndroidJUnit4;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.data.TaskListMetadata;
import org.tasks.injection.TestComponent;

@RunWith(AndroidJUnit4.class)
public class SubtasksHelperTest extends SubtasksTestCase {

  private static final String[] EXPECTED_ORDER = {"-1", "1", "2", "3", "4", "5", "6"};
  private static final String EXPECTED_REMOTE =
      "[\"-1\", [\"6\", \"4\", [\"3\", \"1\"]], \"2\", \"5\"]".replaceAll("\\s", "");
  @Inject TaskDao taskDao;

  @Override
  public void setUp() {
    super.setUp();
    createTasks();
    TaskListMetadata m = new TaskListMetadata();
    m.setFilter(TaskListMetadata.FILTER_ID_ALL);
    updater.initializeFromSerializedTree(
        m, filter, SubtasksHelper.convertTreeToRemoteIds(taskDao, DEFAULT_SERIALIZED_TREE));
  }

  private void createTask(String title, String uuid) {
    Task t = new Task();
    t.setTitle(title);
    t.setUuid(uuid);
    taskDao.createNew(t);
  }

  private void createTasks() {
    createTask("A", "6"); // Local id 1
    createTask("B", "4"); // Local id 2
    createTask("C", "3"); // Local id 3
    createTask("D", "1"); // Local id 4
    createTask("E", "2"); // Local id 5
    createTask("F", "5"); // Local id 6
  }

  // Default order: "[-1, [1, 2, [3, 4]], 5, 6]"

  @Test
  public void testOrderedIdArray() {
    String[] ids = SubtasksHelper.getStringIdArray(DEFAULT_SERIALIZED_TREE);
    assertEquals(EXPECTED_ORDER.length, ids.length);
    for (int i = 0; i < EXPECTED_ORDER.length; i++) {
      assertEquals(EXPECTED_ORDER[i], ids[i]);
    }
  }

  @Test
  public void testLocalToRemoteIdMapping() {
    String mapped =
        SubtasksHelper.convertTreeToRemoteIds(taskDao, DEFAULT_SERIALIZED_TREE)
            .replaceAll("\\s", "");
    assertEquals(EXPECTED_REMOTE, mapped);
  }

  @Override
  protected void inject(TestComponent component) {
    super.inject(component);

    component.inject(this);
  }
}
