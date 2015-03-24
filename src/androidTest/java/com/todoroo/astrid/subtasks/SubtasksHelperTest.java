package com.todoroo.astrid.subtasks;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.service.TaskService;

import javax.inject.Inject;

public class SubtasksHelperTest extends SubtasksTestCase {

    @Inject TaskService taskService;

    @Override
    protected void setUp() {
        super.setUp();
        createTasks();
        TaskListMetadata m = new TaskListMetadata();
        m.setFilter(TaskListMetadata.FILTER_ID_ALL);
        updater.initializeFromSerializedTree(m, filter, SubtasksHelper.convertTreeToRemoteIds(taskService, DEFAULT_SERIALIZED_TREE));
    }

    private void createTask(String title, String uuid) {
        Task t = new Task();
        t.setTitle(title);
        t.setUuid(uuid);
        taskService.save(t);
    }

    private void createTasks() {
        createTask("A", "6"); // Local id 1
        createTask("B", "4"); // Local id 2
        createTask("C", "3"); // Local id 3
        createTask("D", "1"); // Local id 4
        createTask("E", "2"); // Local id 5
        createTask("F", "5"); // Local id 6
    }

    private static final String[] EXPECTED_ORDER = { "-1", "1", "2", "3", "4", "5", "6" };

    public void testOrderedIdArray() {
        String[] ids = SubtasksHelper.getStringIdArray(DEFAULT_SERIALIZED_TREE);
        assertEquals(EXPECTED_ORDER.length, ids.length);
        for (int i = 0; i < EXPECTED_ORDER.length; i++) {
            assertEquals(EXPECTED_ORDER[i], ids[i]);
        }
    }

    // Default order: "[-1, [1, 2, [3, 4]], 5, 6]"

    private static String EXPECTED_REMOTE = "[\"-1\", [\"6\", \"4\", [\"3\", \"1\"]], \"2\", \"5\"]".replaceAll("\\s", "");
    public void disabled_testLocalToRemoteIdMapping() {
        String mapped = SubtasksHelper.convertTreeToRemoteIds(taskService, DEFAULT_SERIALIZED_TREE).replaceAll("\\s", "");
        assertEquals(EXPECTED_REMOTE, mapped);
    }
}
