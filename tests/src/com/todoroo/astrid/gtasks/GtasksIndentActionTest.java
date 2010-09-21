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

public class GtasksIndentActionTest extends DatabaseTestCase {

    @Autowired private GtasksMetadataService gtasksMetadataService;

    private Task task;

    public void testIndentWithoutMetadata() {
        givenTask(taskWithoutMetadata());

        whenTrigger(new GtasksIncreaseIndentAction());

        thenExpectIndentationLevel(1);
    }

    public void testIndentWithMetadata() {
        givenTask(taskWithMetadata(0));

        whenTrigger(new GtasksIncreaseIndentAction());

        thenExpectIndentationLevel(1);
    }

    public void testDeindentWithMetadata() {
        givenTask(taskWithMetadata(1));

        whenTrigger(new GtasksDecreaseIndentAction());

        thenExpectIndentationLevel(0);
    }

    public void testDeindentWithoutMetadata() {
        givenTask(taskWithoutMetadata());

        whenTrigger(new GtasksDecreaseIndentAction());

        thenExpectIndentationLevel(0);
    }

    public void testDeindentWhenAlreadyZero() {
        givenTask(taskWithMetadata(0));

        whenTrigger(new GtasksDecreaseIndentAction());

        thenExpectIndentationLevel(0);
    }

    // --- helpers

    private Task taskWithMetadata(int indentation) {
        Task task = new Task();
        PluginServices.getTaskService().save(task);
        Metadata metadata = GtasksMetadata.createEmptyMetadata(task.getId());
        metadata.setValue(GtasksMetadata.INDENTATION, indentation);
        metadata.setValue(Metadata.TASK, task.getId());
        PluginServices.getMetadataService().save(metadata);
        return task;
    }

    private void thenExpectIndentationLevel(int expected) {
        Metadata metadata = gtasksMetadataService.getTaskMetadata(task.getId());
        assertNotNull("task has metadata", metadata);
        int indentation = metadata.getValue(GtasksMetadata.INDENTATION);
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
