package com.todoroo.astrid.subtasks;

import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

public class SubtasksMovingTest extends SubtasksTestCase {

    private Task A, B, C, D, E, F;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createTasks();
        updater.initializeFromSerializedTree(SubtasksUpdater.ACTIVE_TASKS_ORDER, filter, DEFAULT_SERIALIZED_TREE);

        // Assert initial state is correct
        expectParentAndPosition(A, null, 0);
        expectParentAndPosition(B, A, 0);
        expectParentAndPosition(C, A, 1);
        expectParentAndPosition(D, C, 0);
        expectParentAndPosition(E, null, 1);
        expectParentAndPosition(F, null, 2);
    }

    private void createTasks() {
        A = createTask("A");
        B = createTask("B");
        C = createTask("C");
        D = createTask("D");
        E = createTask("E");
        F = createTask("F");
    }

    private Task createTask(String title) {
        Task task = new Task();
        task.setValue(Task.TITLE, title);
        PluginServices.getTaskService().save(task);
        return task;
    }

    private void whenTriggerMoveBefore(Task target, Task before) {
        long beforeId = (before == null ? -1 : before.getId());
        updater.moveTo(null, filter, target.getId(), beforeId);
    }

    /* Starting State (see SubtasksTestCase):
    *
    * A
    *  B
    *  C
    *   D
    * E
    * F
    */

    public void testMoveBeforeIntoSelf() { // Should have no effect
        whenTriggerMoveBefore(A, B);

        expectParentAndPosition(A, null, 0);
        expectParentAndPosition(B, A, 0);
        expectParentAndPosition(C, A, 1);
        expectParentAndPosition(D, C, 0);
        expectParentAndPosition(E, null, 1);
        expectParentAndPosition(F, null, 2);
    }

    public void testMoveIntoDescendant() { // Should have no effect
        whenTriggerMoveBefore(A, C);

        expectParentAndPosition(A, null, 0);
        expectParentAndPosition(B, A, 0);
        expectParentAndPosition(C, A, 1);
        expectParentAndPosition(D, C, 0);
        expectParentAndPosition(E, null, 1);
        expectParentAndPosition(F, null, 2);
    }

    public void testMoveToEndOfChildren() { // Should have no effect
        whenTriggerMoveBefore(A, E);

        expectParentAndPosition(A, null, 0);
        expectParentAndPosition(B, A, 0);
        expectParentAndPosition(C, A, 1);
        expectParentAndPosition(D, C, 0);
        expectParentAndPosition(E, null, 1);
        expectParentAndPosition(F, null, 2);
    }

    public void testStandardMove() {
        whenTriggerMoveBefore(A, F);

        expectParentAndPosition(A, null, 1);
        expectParentAndPosition(B, A, 0);
        expectParentAndPosition(C, A, 1);
        expectParentAndPosition(D, C, 0);
        expectParentAndPosition(E, null, 0);
        expectParentAndPosition(F, null, 2);
    }

    public void testMoveToEndOfList() {
        whenTriggerMoveBefore(A, null);

        expectParentAndPosition(A, null, 2);
        expectParentAndPosition(B, A, 0);
        expectParentAndPosition(C, A, 1);
        expectParentAndPosition(D, C, 0);
        expectParentAndPosition(E, null, 0);
        expectParentAndPosition(F, null, 1);
    }
}
