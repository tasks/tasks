package com.todoroo.astrid.subtasks;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.AstridOrderedListUpdater.Node;
import com.todoroo.astrid.test.DatabaseTestCase;

import org.tasks.preferences.Preferences;

import javax.inject.Inject;

/**
 * Contains useful methods common to all subtasks tests
 * @author Sam
 *
 */
public class SubtasksTestCase extends DatabaseTestCase {

    @Inject TaskListMetadataDao taskListMetadataDao;
    @Inject TaskService taskService;
    @Inject Preferences preferences;

    protected SubtasksUpdater<TaskListMetadata> updater;
    protected Filter filter;

    /* Starting State:
    *
    * A
    *  B
    *  C
    *   D
    * E
    * F
    */
    public static final String DEFAULT_SERIALIZED_TREE = "[-1, [1, 2, [3, 4]], 5, 6]".replaceAll("\\s", "");

    @Override
    protected void setUp() {
        super.setUp();
        filter = BuiltInFilterExposer.getMyTasksFilter(getContext().getResources());
        preferences.clear(SubtasksUpdater.ACTIVE_TASKS_ORDER);
        updater = new SubtasksFilterUpdater(taskListMetadataDao, taskService);
    }

    protected void expectParentAndPosition(Task task, Task parent, int positionInParent) {
        String parentId = (parent == null ? "-1" : parent.getUuid());
        Node n = updater.findNodeForTask(task.getUuid());
        assertNotNull("No node found for task " + task.getTitle(), n);
        assertEquals("Parent mismatch", parentId, n.parent.uuid);
        assertEquals("Position mismatch", positionInParent, n.parent.children.indexOf(n));
    }

}
