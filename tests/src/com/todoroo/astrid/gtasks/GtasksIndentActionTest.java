package com.todoroo.astrid.gtasks;

import android.content.BroadcastReceiver;
import android.content.Intent;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksIndentAction.GtasksDecreaseIndentAction;
import com.todoroo.astrid.gtasks.GtasksIndentAction.GtasksIncreaseIndentAction;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.gtasks.GoogleTaskListInfo;

public class GtasksIndentActionTest extends DatabaseTestCase {

    @Autowired private GtasksMetadataService gtasksMetadataService;
    @Autowired private GtasksListService gtasksListService;

    private Task task;

    public void testIndentWithoutMetadata() {
        givenTask(taskWithoutMetadata());

        whenTrigger(new GtasksIncreaseIndentAction());

        // should not crash
    }

    public void testIndentWithMetadataButNoOtherTasks() {
        givenTask(taskWithMetadata(0, 0));

        whenTrigger(new GtasksIncreaseIndentAction());

        thenExpectIndentationLevel(0);
    }

    public void testIndentWithMetadata() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 0));

        whenTrigger(new GtasksIncreaseIndentAction());

        thenExpectIndentationLevel(1);
    }

    public void testDeindentWithMetadata() {
        givenTask(taskWithMetadata(0, 1));

        whenTrigger(new GtasksDecreaseIndentAction());

        thenExpectIndentationLevel(0);
    }

    public void testDeindentWithoutMetadata() {
        givenTask(taskWithoutMetadata());

        whenTrigger(new GtasksDecreaseIndentAction());

        // should not crash
    }

    public void testDeindentWhenAlreadyZero() {
        givenTask(taskWithMetadata(0, 0));

        whenTrigger(new GtasksDecreaseIndentAction());

        thenExpectIndentationLevel(0);
    }

    public void testIndentWithChildren() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 0));
        Task child = taskWithMetadata(2, 1);

        whenTrigger(new GtasksIncreaseIndentAction());

        thenExpectIndentationLevel(1);
        thenExpectIndentationLevel(child, 2);
    }

    public void testDeindentWithChildren() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 1));
        Task child = taskWithMetadata(2, 2);

        whenTrigger(new GtasksDecreaseIndentAction());

        thenExpectIndentationLevel(0);
        thenExpectIndentationLevel(child, 1);
    }

    public void testIndentWithSiblings() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 0));
        Task sibling = taskWithMetadata(2, 0);

        whenTrigger(new GtasksIncreaseIndentAction());

        thenExpectIndentationLevel(1);
        thenExpectIndentationLevel(sibling, 0);
    }

    public void testIndentWithChildrensChildren() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 0));
        Task child = taskWithMetadata(2, 1);
        Task grandchild = taskWithMetadata(3, 2);

        whenTrigger(new GtasksIncreaseIndentAction());

        thenExpectIndentationLevel(1);
        thenExpectIndentationLevel(child, 2);
        thenExpectIndentationLevel(grandchild, 3);
    }

    // --- helpers

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        GoogleTaskListInfo[] lists = new GoogleTaskListInfo[1];
        GoogleTaskListInfo list = new GoogleTaskListInfo("list", "Test Tasks");
        lists[0] = list;
        gtasksListService.updateLists(lists);
    }

    private Task taskWithMetadata(int order, int indentation) {
        Task task = new Task();
        PluginServices.getTaskService().save(task);
        Metadata metadata = GtasksMetadata.createEmptyMetadata(task.getId());
        metadata.setValue(GtasksMetadata.INDENT, indentation);
        metadata.setValue(GtasksMetadata.ORDER, order);
        metadata.setValue(GtasksMetadata.LIST_ID, "list");
        metadata.setValue(Metadata.TASK, task.getId());
        PluginServices.getMetadataService().save(metadata);
        return task;
    }

    private void thenExpectIndentationLevel(int expected) {
        thenExpectIndentationLevel(task, expected);
    }

    private void thenExpectIndentationLevel(Task targetTask, int expected) {
        Metadata metadata = gtasksMetadataService.getTaskMetadata(targetTask.getId());
        assertNotNull("task has metadata", metadata);
        int indentation = metadata.getValue(GtasksMetadata.INDENT);
        assertTrue("indentation: " + indentation,
                indentation == expected);
    }

    private void whenTrigger(BroadcastReceiver action) {
        Intent intent = new Intent(AstridApiConstants.ACTION_TASK_CONTEXT_MENU);
        intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
        action.onReceive(getContext(), intent);
    }

    private void givenTask(Task taskToTest) {
        task = taskToTest;
    }

    private Task taskWithoutMetadata() {
        Task task = new Task();
        PluginServices.getTaskService().save(task);
        return task;
    }

}
