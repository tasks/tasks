package com.todoroo.astrid.gtasks;

import java.util.ArrayList;
import java.util.List;

import com.google.api.services.tasks.v1.model.TaskList;
import com.google.api.services.tasks.v1.model.TaskLists;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

@SuppressWarnings("nls")
public class GtasksIndentActionTest extends DatabaseTestCase {

    @Autowired private GtasksMetadataService gtasksMetadataService;
    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksTaskListUpdater gtasksTaskListUpdater;

    private Task task;

    public void testIndentWithoutMetadata() {
        givenTask(taskWithoutMetadata());

        whenIncreaseIndent();

        // should not crash
    }

    public void testIndentWithMetadataButNoOtherTasks() {
        givenTask(taskWithMetadata(0, 0));

        whenIncreaseIndent();

        thenExpectIndentationLevel(0);
    }

    public void testIndentWithMetadata() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 0));

        whenIncreaseIndent();

        thenExpectIndentationLevel(1);
    }

    public void testDeindentWithMetadata() {
        givenTask(taskWithMetadata(0, 1));

        whenDecreaseIndent();

        thenExpectIndentationLevel(0);
    }

    public void testDeindentWithoutMetadata() {
        givenTask(taskWithoutMetadata());

        whenDecreaseIndent();

        // should not crash
    }

    public void testDeindentWhenAlreadyZero() {
        givenTask(taskWithMetadata(0, 0));

        whenDecreaseIndent();

        thenExpectIndentationLevel(0);
    }

    public void testIndentWithChildren() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 0));
        Task child = taskWithMetadata(2, 1);

        whenIncreaseIndent();

        thenExpectIndentationLevel(1);
        thenExpectIndentationLevel(child, 2);
    }

    public void testDeindentWithChildren() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 1));
        Task child = taskWithMetadata(2, 2);

        whenDecreaseIndent();

        thenExpectIndentationLevel(0);
        thenExpectIndentationLevel(child, 1);
    }

    public void testIndentWithSiblings() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 0));
        Task sibling = taskWithMetadata(2, 0);

        whenIncreaseIndent();

        thenExpectIndentationLevel(1);
        thenExpectIndentationLevel(sibling, 0);
    }

    public void testIndentWithChildrensChildren() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 0));
        Task child = taskWithMetadata(2, 1);
        Task grandchild = taskWithMetadata(3, 2);

        whenIncreaseIndent();

        thenExpectIndentationLevel(1);
        thenExpectIndentationLevel(child, 2);
        thenExpectIndentationLevel(grandchild, 3);
    }

    // --- helpers

    private void whenIncreaseIndent() {
        gtasksTaskListUpdater.indent(task.getId(), 1);
    }

    private void whenDecreaseIndent() {
        gtasksTaskListUpdater.indent(task.getId(), -1);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        TaskLists lists = new TaskLists();
        List<TaskList> items = new ArrayList<TaskList>();
        TaskList list = new TaskList();
        list.id = "list";
        list.title = "Test Tasks";
        items.add(list);
        lists.items = items;
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

    private void givenTask(Task taskToTest) {
        task = taskToTest;
    }

    private Task taskWithoutMetadata() {
        Task task = new Task();
        PluginServices.getTaskService().save(task);
        return task;
    }

}//*/
