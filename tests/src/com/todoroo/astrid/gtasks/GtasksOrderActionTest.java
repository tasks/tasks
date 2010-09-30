package com.todoroo.astrid.gtasks;

import android.content.BroadcastReceiver;
import android.content.Intent;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksOrderAction.GtasksMoveDownAction;
import com.todoroo.astrid.gtasks.GtasksOrderAction.GtasksMoveUpAction;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.gtasks.GoogleTaskListInfo;

public class GtasksOrderActionTest extends DatabaseTestCase {

    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksMetadataService gtasksMetadataService;

    private Task A, B, C, D, E, F;

    public void testMoveUpFromListTop() {
        givenTasksABCDEF();

        whenTrigger(A, new GtasksMoveUpAction());

        thenExpectMetadataIndentAndOrder(A, 0, 0);
        thenExpectMetadataIndentAndOrder(B, 1, 1);
    }

    public void testMoveDownFromListBottom() {
        givenTasksABCDEF();

        whenTrigger(F, new GtasksMoveDownAction());

        thenExpectMetadataIndentAndOrder(E, 4, 0);
        thenExpectMetadataIndentAndOrder(F, 5, 0);
    }

    public void testMoveDownSimple() {
        givenTasksABCDEF();

        whenTrigger(E, new GtasksMoveDownAction());

        thenExpectMetadataIndentAndOrder(E, 5, 0);
        thenExpectMetadataIndentAndOrder(F, 4, 00);
    }

    public void testMoveUpSimple() {
        givenTasksABCDEF();

        whenTrigger(F, new GtasksMoveUpAction());

        thenExpectMetadataIndentAndOrder(E, 5, 0);
        thenExpectMetadataIndentAndOrder(F, 4, 00);
    }

    public void testMoveWithSubtasks() {
        givenTasksABCDEF();

        whenTrigger(C, new GtasksMoveUpAction());

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

        whenTrigger(C, new GtasksMoveDownAction());

        thenExpectOriginalIndentAndOrder();
    }

    public void testMoveDownThroughSubtasks() {
        givenTasksABCDEF();

        whenTrigger(B, new GtasksMoveDownAction());

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

        whenTrigger(B, new GtasksMoveUpAction());

        thenExpectOriginalIndentAndOrder();
    }

    public void testMoveAboveParent() {
        givenTasksABCDEF();

        whenTrigger(B, new GtasksMoveUpAction());

        /*
         * B
         * A
         *  C
         *   D
         */

        thenExpectMetadataIndentAndOrder(A, 1, 0);
        thenExpectMetadataIndentAndOrder(B, 0, 0);
        thenExpectMetadataIndentAndOrder(C, 2, 1);
    }

    public void testMoveDownFromChildren() {
        givenTasksABCDEF();

        whenTrigger(C, new GtasksMoveDownAction());

        /*
         * A
         *  B
         * E
         * C
         *  D
         */

        thenExpectMetadataIndentAndOrder(A, 0, 0);
        thenExpectMetadataIndentAndOrder(B, 1, 1);
        thenExpectMetadataIndentAndOrder(C, 3, 0);
        thenExpectMetadataIndentAndOrder(D, 4, 1);
        thenExpectMetadataIndentAndOrder(E, 2, 0);
    }

    public void testMoveDownIndentingTwice() {
        givenTasksABCDEF();

        whenTrigger(D, new GtasksMoveDownAction());

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


    // --- helpers

    private void whenTrigger(Task task, BroadcastReceiver action) {
        Intent intent = new Intent(AstridApiConstants.ACTION_TASK_CONTEXT_MENU);
        intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
        action.onReceive(getContext(), intent);
    }

    private void thenExpectMetadataIndentAndOrder(Task task, int order, int indent) {
        Metadata metadata = gtasksMetadataService.getTaskMetadata(task.getId());
        assertNotNull("metadata was found", metadata);
        assertEquals("order", order, (int)metadata.getValue(GtasksMetadata.ORDER));
        assertEquals("indentation", indent, (int)metadata.getValue(GtasksMetadata.INDENT));
    }

    private void thenExpectOriginalIndentAndOrder() {
        thenExpectMetadataIndentAndOrder(A, 0, 0);
        thenExpectMetadataIndentAndOrder(B, 1, 1);
        thenExpectMetadataIndentAndOrder(C, 2, 1);
        thenExpectMetadataIndentAndOrder(D, 3, 2);
        thenExpectMetadataIndentAndOrder(E, 4, 0);
        thenExpectMetadataIndentAndOrder(F, 5, 0);
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
