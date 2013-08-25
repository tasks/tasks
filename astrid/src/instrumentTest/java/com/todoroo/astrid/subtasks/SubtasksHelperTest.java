package com.todoroo.astrid.subtasks;

import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;

public class SubtasksHelperTest extends SubtasksTestCase {

    private Task A, B, C, D, E, F;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createTasks();
        TaskListMetadata m = new TaskListMetadata();
        m.setValue(TaskListMetadata.FILTER, TaskListMetadata.FILTER_ID_ALL);
        updater.initializeFromSerializedTree(m, filter, SubtasksHelper.convertTreeToRemoteIds(DEFAULT_SERIALIZED_TREE));
    }

    private Task createTask(String title, String uuid) {
        Task t = new Task();
        t.setValue(Task.TITLE, title);
        t.setValue(Task.UUID, uuid);
        PluginServices.getTaskService().save(t);
        return t;
    }

    private void createTasks() {
        A = createTask("A", "6"); // Local id 1
        B = createTask("B", "4"); // Local id 2
        C = createTask("C", "3"); // Local id 3
        D = createTask("D", "1"); // Local id 4
        E = createTask("E", "2"); // Local id 5
        F = createTask("F", "5"); // Local id 6
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
    public void testLocalToRemoteIdMapping() {
        String mapped = SubtasksHelper.convertTreeToRemoteIds(DEFAULT_SERIALIZED_TREE).replaceAll("\\s", "");
        assertEquals(EXPECTED_REMOTE, mapped);
    }
}
