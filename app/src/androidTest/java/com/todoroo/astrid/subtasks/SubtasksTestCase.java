package com.todoroo.astrid.subtasks;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Node;
import javax.inject.Inject;
import org.tasks.data.TaskListMetadataDao;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.preferences.Preferences;

/**
 * Contains useful methods common to all subtasks tests
 *
 * @author Sam
 */
public class SubtasksTestCase extends InjectingTestCase {

  /* Starting State:
   *
   * A
   *  B
   *  C
   *   D
   * E
   * F
   */
  static final String DEFAULT_SERIALIZED_TREE = "[-1, [1, 2, [3, 4]], 5, 6]".replaceAll("\\s", "");
  SubtasksFilterUpdater updater;
  Filter filter;
  @Inject TaskListMetadataDao taskListMetadataDao;
  @Inject TaskDao taskDao;
  @Inject Preferences preferences;

  @Override
  public void setUp() {
    super.setUp();
    filter = BuiltInFilterExposer.getMyTasksFilter(getTargetContext().getResources());
    preferences.clear(SubtasksFilterUpdater.ACTIVE_TASKS_ORDER);
    updater = new SubtasksFilterUpdater(taskListMetadataDao, taskDao);
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }

  void expectParentAndPosition(Task task, Task parent, int positionInParent) {
    String parentId = (parent == null ? "-1" : parent.getUuid());
    Node n = updater.findNodeForTask(task.getUuid());
    assertNotNull("No node found for task " + task.getTitle(), n);
    assertEquals("Parent mismatch", parentId, n.parent.uuid);
    assertEquals("Position mismatch", positionInParent, n.parent.children.indexOf(n));
  }
}
