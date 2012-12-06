package com.todoroo.astrid.subtasks;

import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

public class SubtasksHelperTest extends SubtasksTestCase {

    private Task A, B, C, D, E, F;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createTasks();
        updater.initializeFromSerializedTree(SubtasksUpdater.ACTIVE_TASKS_ORDER, filter, DEFAULT_SERIALIZED_TREE);
    }

    private Task createTask(String title, long remoteId) {
        Task t = new Task();
        t.setValue(Task.TITLE, title);
        t.setValue(Task.REMOTE_ID, remoteId);
        PluginServices.getTaskService().save(t);
        return t;
    }

    private void createTasks() {
        A = createTask("A", 6); // Local id 1
        B = createTask("B", 4); // Local id 2
        C = createTask("C", 3); // Local id 3
        D = createTask("D", 1); // Local id 4
        E = createTask("E", 2); // Local id 5
        F = createTask("F", 5); // Local id 6
    }

    private static final Long[] EXPECTED_ORDER = {-1L, 1L, 2L, 3L, 4L, 5L, 6L };

    public void testOrderedIdArray() {
        Long[] ids = SubtasksHelper.getIdArray(DEFAULT_SERIALIZED_TREE);
        assertEquals(EXPECTED_ORDER.length, ids.length);
        for (int i = 0; i < EXPECTED_ORDER.length; i++) {
            assertEquals(EXPECTED_ORDER[i], ids[i]);
        }
    }

    // Default order: "[-1, [1, 2, [3, 4]], 5, 6]"

    private static String EXPECTED_REMOTE = "[-1, [6, 4, [3, 1]], 2, 5]".replaceAll("\\s", "");
    public void testLocalToRemoteIdMapping() {
        String mapped = SubtasksHelper.convertTreeToRemoteIds(DEFAULT_SERIALIZED_TREE).replaceAll("\\s", "");
        assertEquals(EXPECTED_REMOTE, mapped);
    }


    private static String EXPECTED_LOCAL = "[-1, [4, 5, [3, 2]], 6, 1]".replaceAll("\\s", "");
    public void testRemoteToLocalIdMapping() {
        String mapped = SubtasksHelper.convertTreeToLocalIds(DEFAULT_SERIALIZED_TREE).replaceAll("\\s", "");
        assertEquals(EXPECTED_LOCAL, mapped);
    }

}
