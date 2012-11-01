package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.AstridOrderedListUpdater.Node;
import com.todoroo.astrid.test.DatabaseTestCase;

public class SubtasksMovingTest extends DatabaseTestCase {

    private SubtasksUpdater updater;
    private Filter filter;
    private Task A, B, C, D, E, F;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        filter = CoreFilterExposer.buildInboxFilter(getContext().getResources());
        Preferences.clear(SubtasksUpdater.ACTIVE_TASKS_ORDER);
        updater = new SubtasksUpdater();
        createTasks();
        updater.initializeFromSerializedTree(null, filter, getSerializedTree());
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

    private void thenExpectParentAndPosition(Task task, Task parent, int positionInParent) {
        long parentId = (parent == null ? -1 : parent.getId());
        Node n = updater.findNodeForTask(task.getId());
        assertNotNull("No node found for task " + task.getValue(Task.TITLE), n);
        assertEquals("Parent mismatch", parentId, n.parent.taskId);
        assertEquals("Position mismatch", positionInParent, n.parent.children.indexOf(n));
    }

    /* Starting State:
    *
    * A
    *  B
    *  C
    *   D
    * E
    * F
    */
    private String getSerializedTree() {
        return "[{\"1\":[{\"2\":[]}, {\"3\":[{\"4\":[]}]}]}, {\"5\":[]}, {\"6\":[]}]";
    }

    public void testMoveBeforeIntoSelf() { // Should have no effect
        whenTriggerMoveBefore(A, B);

        thenExpectParentAndPosition(A, null, 0);
        thenExpectParentAndPosition(B, A, 0);
        thenExpectParentAndPosition(C, A, 1);
        thenExpectParentAndPosition(D, C, 0);
        thenExpectParentAndPosition(E, null, 1);
        thenExpectParentAndPosition(F, null, 2);
    }

    public void testMoveIntoDescendant() { // Should have no effect
        whenTriggerMoveBefore(A, C);

        thenExpectParentAndPosition(A, null, 0);
        thenExpectParentAndPosition(B, A, 0);
        thenExpectParentAndPosition(C, A, 1);
        thenExpectParentAndPosition(D, C, 0);
        thenExpectParentAndPosition(E, null, 1);
        thenExpectParentAndPosition(F, null, 2);
    }

    public void testMoveToEndOfChildren() { // Should have no effect
        whenTriggerMoveBefore(A, E);

        thenExpectParentAndPosition(A, null, 0);
        thenExpectParentAndPosition(B, A, 0);
        thenExpectParentAndPosition(C, A, 1);
        thenExpectParentAndPosition(D, C, 0);
        thenExpectParentAndPosition(E, null, 1);
        thenExpectParentAndPosition(F, null, 2);
    }

    public void testStandardMove() {
        whenTriggerMoveBefore(A, F);

        thenExpectParentAndPosition(A, null, 1);
        thenExpectParentAndPosition(B, A, 0);
        thenExpectParentAndPosition(C, A, 1);
        thenExpectParentAndPosition(D, C, 0);
        thenExpectParentAndPosition(E, null, 0);
        thenExpectParentAndPosition(F, null, 2);
    }

    public void testMoveToEndOfList() {
        whenTriggerMoveBefore(A, null);

        thenExpectParentAndPosition(A, null, 2);
        thenExpectParentAndPosition(B, A, 0);
        thenExpectParentAndPosition(C, A, 1);
        thenExpectParentAndPosition(D, C, 0);
        thenExpectParentAndPosition(E, null, 0);
        thenExpectParentAndPosition(F, null, 1);
    }
}
