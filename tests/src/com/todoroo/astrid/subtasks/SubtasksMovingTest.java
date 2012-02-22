package com.todoroo.astrid.subtasks;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

@SuppressWarnings("nls")
public class SubtasksMovingTest extends DatabaseTestCase {

    private SubtasksUpdater updater;

    private Filter filter;
    private Task A, B, C, D, E, F;
    private final String list = SubtasksMetadata.LIST_ACTIVE_TASKS;

    /* Starting State:
     *
     * A
     *  B
     *  C
     *   D
     * E
     * F
     */

    public void testMoveBeforeIntoSelf() {
        givenTasksABCDEF();

        whenTriggerMoveBefore(A, B);

        /*
         * A
         *  B
         *  C
         *   D
         * E
         */
        thenExpectMetadataOrderAndIndent(A, 0, 0);
        thenExpectMetadataOrderAndIndent(B, 1, 1);
        thenExpectMetadataOrderAndIndent(C, 2, 1);
        thenExpectMetadataOrderAndIndent(D, 3, 2);
        thenExpectMetadataOrderAndIndent(E, 4, 0);
    }

    public void testMoveIntoChild() {
        givenTasksABCDEF();

        whenTriggerMoveBefore(A, C);

        /*
         * A
         *  B
         *  C
         *   D
         * E
         */
        thenExpectMetadataOrderAndIndent(A, 0, 0);
        thenExpectMetadataOrderAndIndent(B, 1, 1);
        thenExpectMetadataOrderAndIndent(C, 2, 1);
        thenExpectMetadataOrderAndIndent(D, 3, 2);
        thenExpectMetadataOrderAndIndent(E, 4, 0);
    }

    public void testMoveEndOfChildren() {
        givenTasksABCDEF();

        whenTriggerMoveBefore(A, E);

        /*
         * A
         *  B
         *  C
         *   D
         * E
         */
        thenExpectMetadataOrderAndIndent(A, 0, 0);
        thenExpectMetadataOrderAndIndent(B, 1, 1);
        thenExpectMetadataOrderAndIndent(C, 2, 1);
        thenExpectMetadataOrderAndIndent(D, 3, 2);
        thenExpectMetadataOrderAndIndent(E, 4, 0);
    }

    public void testMoveAfterChildren() {
        givenTasksABCDEF();

        whenTriggerMoveBefore(A, F);

        /*
         * E
         * A
         *  B
         *  C
         *   D
         */
        thenExpectMetadataOrderAndIndent(E, 0, 0);
        thenExpectMetadataOrderAndIndent(A, 1, 0);
        thenExpectMetadataOrderAndIndent(B, 2, 1);
        thenExpectMetadataOrderAndIndent(C, 3, 1);
        thenExpectMetadataOrderAndIndent(D, 4, 2);
    }

    // --- helpers

    /** moveTo = null => move to end */
    private void whenTriggerMoveBefore(Task target, Task moveTo) {
        System.err.println("CAN I GET A WITNESS?");
        updater.debugPrint(filter, list);
        updater.moveTo(filter, list, target.getId(), moveTo == null ? -1 : moveTo.getId());
        updater.debugPrint(filter, list);
    }

    private void thenExpectMetadataOrderAndIndent(Task task, long order, int indent) {
        Metadata metadata = updater.getTaskMetadata(list, task.getId());
        assertNotNull("metadata was found", metadata);
        assertEquals("order", order, metadata.getValue(SubtasksMetadata.ORDER).longValue());
        assertEquals("indentation", indent, (int)metadata.getValue(SubtasksMetadata.INDENT));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        updater = new SubtasksUpdater();
        filter = CoreFilterExposer.buildInboxFilter(getContext().getResources());
        updater.applySubtasksToFilter(filter, list);
    }

    private Task[] givenTasksABCDEF() {
        Task[] tasks = new Task[] {
            A = createTask("A", 0, 0),
            B = createTask("B", 1, 1),
            C = createTask("C", 2, 1),
            D = createTask("D", 3, 2),
            E = createTask("E", 4, 0),
            F = createTask("F", 5, 0),
        };

        return tasks;
    }

    private Task createTask(String title, long order, int indent) {
        Task task = new Task();
        task.setValue(Task.TITLE, title);
        PluginServices.getTaskService().save(task);
        Metadata metadata = updater.createEmptyMetadata(list, task.getId());
        metadata.setValue(SubtasksMetadata.ORDER, order);
        metadata.setValue(SubtasksMetadata.INDENT, indent);
        PluginServices.getMetadataService().save(metadata);
        return task;
    }

}
