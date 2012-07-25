/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import java.util.ArrayList;
import java.util.List;

import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

@SuppressWarnings("nls")
public class GtasksTaskListUpdaterTest extends DatabaseTestCase {

    @Autowired private GtasksTaskListUpdater gtasksTaskListUpdater;
    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksMetadataService gtasksMetadataService;

    public void testBasicParentComputation() {
        Task[] tasks = givenTasksABCDE();

        whenCalculatingParentsAndSiblings();

        thenExpectParent(tasks[0], null);
        thenExpectParent(tasks[1], tasks[0]);
        thenExpectParent(tasks[2], tasks[0]);
        thenExpectParent(tasks[3], tasks[2]);
        thenExpectParent(tasks[4], null);
    }

    public void testBasicSiblingComputation() {
        Task[] tasks = givenTasksABCDE();

        whenCalculatingParentsAndSiblings();

        thenExpectSibling(tasks[0], null);
        thenExpectSibling(tasks[1], null);
        thenExpectSibling(tasks[2], tasks[1]);
        thenExpectSibling(tasks[3], null);
        thenExpectSibling(tasks[4], tasks[0]);
    }

    public void testMetadataParentComputation() {
        Task[] tasks = givenTasksABCDE();

        whenCalculatingOrder();

        thenExpectMetadataParent(tasks[0], null);
        thenExpectMetadataParent(tasks[1], tasks[0]);
        thenExpectMetadataParent(tasks[2], tasks[0]);
        thenExpectMetadataParent(tasks[3], tasks[2]);
        thenExpectMetadataParent(tasks[4], null);
    }

    public void testMetadataOrderComputation() {
        Task[] tasks = givenTasksABCDE();

        whenCalculatingOrder();

        thenExpectMetadataIndentAndOrder(tasks[0], 0, 0);
        thenExpectMetadataIndentAndOrder(tasks[1], 1, 1);
        thenExpectMetadataIndentAndOrder(tasks[2], 2, 1);
        thenExpectMetadataIndentAndOrder(tasks[3], 3, 2);
        thenExpectMetadataIndentAndOrder(tasks[4], 4, 0);
    }

    public void testNewTaskOrder() {
        givenTasksABCDE();

        Task newTask = createTask("F", GtasksMetadata.VALUE_UNSET, 0);
        whenCalculatingOrder();

        thenExpectMetadataIndentAndOrder(newTask, 5, 0);
    }


    // --- helpers

    private void thenExpectMetadataIndentAndOrder(Task task, long order, int indent) {
        Metadata metadata = gtasksMetadataService.getTaskMetadata(task.getId());
        assertNotNull("metadata was found", metadata);
        assertEquals("order", order, metadata.getValue(GtasksMetadata.ORDER).longValue());
        assertEquals("indentation", indent, (int)metadata.getValue(GtasksMetadata.INDENT));
    }

    private void thenExpectMetadataParent(Task task, Task expectedParent) {
        Metadata metadata = gtasksMetadataService.getTaskMetadata(task.getId());
        long parent = metadata.getValue(GtasksMetadata.PARENT_TASK);
        if(expectedParent == null)
            assertEquals("Task " + task.getValue(Task.TITLE) + " parent none", 0, parent);
        else
            assertEquals("Task " + task.getValue(Task.TITLE) + " parent " +
                    expectedParent.getValue(Task.TITLE), expectedParent.getId(), parent);
    }

    private void thenExpectSibling(Task task, Task expectedSibling) {
        long sibling = gtasksTaskListUpdater.siblings.get(task.getId());
        if(expectedSibling == null)
            assertEquals("Task " + task.getValue(Task.TITLE) + " sibling null", 0L, sibling);
        else
            assertEquals("Task " + task.getValue(Task.TITLE) + " sibling " +
                    expectedSibling.getValue(Task.TITLE), expectedSibling.getId(), sibling);
    }

    private void thenExpectParent(Task task, Task expectedParent) {
        long parent = gtasksTaskListUpdater.parents.get(task.getId());
        if(expectedParent == null)
            assertEquals("Task " + task.getValue(Task.TITLE) + " parent null", 0L, parent);
        else
            assertEquals("Task " + task.getValue(Task.TITLE) + " parent " +
                    expectedParent.getValue(Task.TITLE), expectedParent.getId(), parent);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        TaskLists lists = new TaskLists();
        List<TaskList> items = new ArrayList<TaskList>();
        TaskList list = new TaskList();
        list.setId("1");
        list.setTitle("Tim's Tasks");
        items.add(list);
        lists.setItems(items);
        gtasksListService.updateLists(lists);
    }

    private void whenCalculatingParentsAndSiblings() {
        gtasksTaskListUpdater.createParentSiblingMaps();
    }

    private void whenCalculatingOrder() {
        for(StoreObject list : gtasksListService.getLists())
            gtasksTaskListUpdater.correctMetadataForList(list.getValue(GtasksList.REMOTE_ID));
    }


    /**
     * A
     *  B
     *  C
     *   D
     * E
     */
    private Task[] givenTasksABCDE() {
        return new Task[] {
            createTask("A", 0, 0),
            createTask("B", 1, 1),
            createTask("C", 2, 1),
            createTask("D", 3, 2),
            createTask("E", 4, 0),
        };
    }


    private Task createTask(String title, long order, int indent) {
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

}//*/
