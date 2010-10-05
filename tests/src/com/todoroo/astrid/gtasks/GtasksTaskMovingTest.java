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

        thenExpectMetadataIndentAndOrder(E, 4, 0);
        thenExpectMetadataIndentAndOrder(F, 5, 0);
    }

    public void testMoveDownToListbottom() {
        givenTasksABCDEF();

        whenTriggerMove(E, null);

        thenExpectMetadataIndentAndOrder(E, 5, 0);
        thenExpectMetadataIndentAndOrder(F, 4, 00);
    }

    public void testMoveUpSimple() {
        givenTasksABCDEF();

        whenTriggerMove(F, E);

        thenExpectMetadataIndentAndOrder(E, 5, 0);
        thenExpectMetadataIndentAndOrder(F, 4, 00);
    }

    public void testMoveWithSubtasks() {
        givenTasksABCDEF();

        whenTriggerMove(C, B);

        /*
         * A
         *  C
         *   D
         *  B
         */

        thenExpectMetadataIndentAndOrder(A, 0, 0);
        thenExpectMetadataIndentAndOrder(B, 3, 1);
        thenExpectMetadataIndentAndOrder(C, 1, 1);
        thenExpectMetadataIndentAndOrder(D, 2, 2);
    }

    public void testMoveThroughSubtasks() {
        givenTasksABCDEF();

        whenTriggerMove(B, E);

        /*
         * A
         *  C
         *   D
         * B
         * E
         */

        thenExpectMetadataIndentAndOrder(A, 0, 0);
        thenExpectMetadataIndentAndOrder(B, 3, 0);
        thenExpectMetadataIndentAndOrder(C, 1, 1);
        thenExpectMetadataIndentAndOrder(D, 2, 2);
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

        thenExpectMetadataIndentAndOrder(A, 1, 0);
        thenExpectMetadataIndentAndOrder(B, 0, 0);
        thenExpectMetadataIndentAndOrder(C, 2, 1);
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

        thenExpectMetadataIndentAndOrder(A, 0, 0);
        thenExpectMetadataIndentAndOrder(B, 1, 1);
        thenExpectMetadataIndentAndOrder(C, 3, 0);
        thenExpectMetadataIndentAndOrder(D, 4, 1);
        thenExpectMetadataIndentAndOrder(E, 2, 0);
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

        thenExpectMetadataIndentAndOrder(A, 0, 0);
        thenExpectMetadataIndentAndOrder(B, 1, 1);
        thenExpectMetadataIndentAndOrder(C, 2, 1);
        thenExpectMetadataIndentAndOrder(D, 4, 0);
        thenExpectMetadataIndentAndOrder(E, 3, 0);
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

        thenExpectMetadataIndentAndOrder(A, 2, 0);
        thenExpectMetadataIndentAndOrder(B, 3, 1);
        thenExpectMetadataIndentAndOrder(C, 0, 0);
        thenExpectMetadataIndentAndOrder(D, 1, 1);
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

        thenExpectMetadataIndentAndOrder(A, 0, 0);
        thenExpectMetadataIndentAndOrder(B, 1, 1);
        thenExpectMetadataIndentAndOrder(C, 2, 1);
        thenExpectMetadataIndentAndOrder(D, 4, 2);
        thenExpectMetadataIndentAndOrder(E, 5, 0);
        thenExpectMetadataIndentAndOrder(F, 3, 2);
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

        thenExpectMetadataIndentAndOrder(A, 0, 0);
        thenExpectMetadataIndentAndOrder(B, 4, 0);
        thenExpectMetadataIndentAndOrder(C, 1, 1);
        thenExpectMetadataIndentAndOrder(D, 2, 2);
        thenExpectMetadataIndentAndOrder(E, 3, 0);
        thenExpectMetadataIndentAndOrder(F, 5, 0);
    }


    // --- helpers

    /** moveTo = null => move to end */
    private void whenTriggerMove(Task target, Task moveTo) {
        gtasksTaskListUpdater.moveTo("1", target.getId(), moveTo == null ? -1 : moveTo.getId());
    }

    private void thenExpectMetadataIndentAndOrder(Task task, int order, int indent) {
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
        return new Task[] {
            A = createTask("A", 0, 0),
            B = createTask("B", 1, 1),
            C = createTask("C", 2, 1),
            D = createTask("D", 3, 2),
            E = createTask("E", 4, 0),
            F = createTask("F", 5, 0),
        };
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
