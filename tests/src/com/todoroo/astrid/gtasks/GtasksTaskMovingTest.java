package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.gtasks.GoogleTaskListInfo;

public class GtasksTaskMovingTest extends DatabaseTestCase {

    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksMetadataService gtasksMetadataService;
    @Autowired private GtasksTaskListUpdater gtasksTaskListUpdater;

    private Task A, B, C, D, E, F;

    /* Starting State:
     *
     * A
     *  B
     *  C
     *   D
     * E
     * F
     */

    public void testMoveDownFromListBottom() {
        givenTasksABCDEF();

        whenTriggerMove(F, null);

        thenExpectMetadataOrderAndIndent(E, 4, 0);
        thenExpectMetadataOrderAndIndent(F, 5, 0);
    }

    public void testMoveDownToListBottom() {
        givenTasksABCDEF();

        whenTriggerMove(E, null);

        thenExpectMetadataOrderAndIndent(E, 5, 0);
        thenExpectMetadataOrderAndIndent(F, 4, 0);
    }

    public void testMoveUpSimple() {
        givenTasksABCDEF();

        whenTriggerMove(F, E);

        thenExpectMetadataOrderAndIndent(E, 5, 0);
        thenExpectMetadataOrderAndIndent(F, 4, 0);
    }

    public void testMoveUpWithSubtasks() {
        givenTasksABCDEF();

        whenTriggerMove(C, B);

        /*
         * A
         *  C
         *   D
         *  B
         */

        thenExpectMetadataOrderAndIndent(A, 0, 0);
        thenExpectMetadataOrderAndIndent(B, 3, 1);
        thenExpectMetadataOrderAndIndent(C, 1, 1);
        thenExpectMetadataOrderAndIndent(D, 2, 2);
    }

    public void testMoveDownThroughSubtasks() {
        givenTasksABCDEF();

        whenTriggerMove(B, E);

        /*
         * A
         *  C
         *   D
         * B
         * E
         */

        thenExpectMetadataOrderAndIndent(A, 0, 0);
        thenExpectMetadataOrderAndIndent(B, 3, 0);
        thenExpectMetadataOrderAndIndent(C, 1, 1);
        thenExpectMetadataOrderAndIndent(D, 2, 2);
    }

    public void testMoveUpAboveParent() {
        givenTasksABCDEF();

        whenTriggerMove(B, A);

        /*
         * B
         * A
         *  C
         *   D
         * E
         * F
         */

        thenExpectMetadataOrderAndIndent(A, 1, 0);
        thenExpectMetadataOrderAndIndent(B, 0, 0);
        thenExpectMetadataOrderAndIndent(C, 2, 1);
    }

    public void testMoveDownWithChildren() {
        givenTasksABCDEF();

        whenTriggerMove(C, F);

        /*
         * A
         *  B
         * E
         * C
         *  D
         * F
         */

        thenExpectMetadataOrderAndIndent(A, 0, 0);
        thenExpectMetadataOrderAndIndent(B, 1, 1);
        thenExpectMetadataOrderAndIndent(C, 3, 0);
        thenExpectMetadataOrderAndIndent(D, 4, 1);
        thenExpectMetadataOrderAndIndent(E, 2, 0);
    }

    public void testMoveDownIndentingTwice() {
        givenTasksABCDEF();

        whenTriggerMove(D, F);

        /*
         * A
         *  B
         *  C
         * E
         * D
         */

        thenExpectMetadataOrderAndIndent(A, 0, 0);
        thenExpectMetadataOrderAndIndent(B, 1, 1);
        thenExpectMetadataOrderAndIndent(C, 2, 1);
        thenExpectMetadataOrderAndIndent(D, 4, 0);
        thenExpectMetadataOrderAndIndent(E, 3, 0);
    }

    public void testMoveUpMultiple() {
        givenTasksABCDEF();

        whenTriggerMove(C, A);

        /*
         * C
         *  D
         * A
         *  B
         */

        thenExpectMetadataOrderAndIndent(A, 2, 0);
        thenExpectMetadataOrderAndIndent(B, 3, 1);
        thenExpectMetadataOrderAndIndent(C, 0, 0);
        thenExpectMetadataOrderAndIndent(D, 1, 1);
    }

    public void testMoveUpIntoSublist() {
        givenTasksABCDEF();

        whenTriggerMove(F, D);

        /*
         * A
         *  B
         *  C
         *   F
         *   D
         */

        thenExpectMetadataOrderAndIndent(A, 0, 0);
        thenExpectMetadataOrderAndIndent(B, 1, 1);
        thenExpectMetadataOrderAndIndent(C, 2, 1);
        thenExpectMetadataOrderAndIndent(D, 4, 2);
        thenExpectMetadataOrderAndIndent(E, 5, 0);
        thenExpectMetadataOrderAndIndent(F, 3, 2);
    }

    public void testMoveDownMultiple() {
        givenTasksABCDEF();

        whenTriggerMove(B, F);

        /*
         * A
         *  C
         *   D
         * E
         * B
         */

        thenExpectMetadataOrderAndIndent(A, 0, 0);
        thenExpectMetadataOrderAndIndent(B, 4, 0);
        thenExpectMetadataOrderAndIndent(C, 1, 1);
        thenExpectMetadataOrderAndIndent(D, 2, 2);
        thenExpectMetadataOrderAndIndent(E, 3, 0);
        thenExpectMetadataOrderAndIndent(F, 5, 0);
    }


    // --- helpers

    /** moveTo = null => move to end */
    private void whenTriggerMove(Task target, Task moveTo) {
        gtasksTaskListUpdater.moveTo(target.getId(), moveTo == null ? -1 : moveTo.getId());
        gtasksTaskListUpdater.debugPrint("1");
    }

    private void thenExpectMetadataOrderAndIndent(Task task, int order, int indent) {
        Metadata metadata = gtasksMetadataService.getTaskMetadata(task.getId());
        assertNotNull("metadata was found", metadata);
        assertEquals("order", order, (int)metadata.getValue(GtasksMetadata.ORDER));
        assertEquals("indentation", indent, (int)metadata.getValue(GtasksMetadata.INDENT));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        GoogleTaskListInfo[] lists = new GoogleTaskListInfo[1];
        GoogleTaskListInfo list = new GoogleTaskListInfo("1", "Tim's Tasks");
        lists[0] = list;
        gtasksListService.updateLists(lists);
    }

    /**
     * A
     *  B
     *  C
     *   D
     * E
     * F
     */
    private Task[] givenTasksABCDEF() {
        Task[] tasks = new Task[] {
            A = createTask("A", 0, 0),
            B = createTask("B", 1, 1),
            C = createTask("C", 2, 1),
            D = createTask("D", 3, 2),
            E = createTask("E", 4, 0),
            F = createTask("F", 5, 0),
        };
        gtasksTaskListUpdater.correctMetadataForList("1");
        return tasks;
    }

    private Task createTask(String title, int order, int indent) {
        Task task = new Task();
        task.setValue(Task.TITLE, title);
        PluginServices.getTaskService().save(task);
        Metadata metadata = GtasksMetadata.createEmptyMetadata(task.getId());
        metadata.setValue(GtasksMetadata.LIST_ID, "1");
        if(order != GtasksMetadata.VALUE_UNSET)
            metadata.setValue(GtasksMetadata.ORDER, order);
        if(indent != GtasksMetadata.VALUE_UNSET)
            metadata.setValue(GtasksMetadata.INDENT, indent);
        PluginServices.getMetadataService().save(metadata);
        return task;
    }

}
