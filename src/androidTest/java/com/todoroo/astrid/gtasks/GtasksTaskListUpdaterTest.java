/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.test.DatabaseTestCase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

@SuppressWarnings("nls")
public class GtasksTaskListUpdaterTest extends DatabaseTestCase {

    private static final int VALUE_UNSET = -1;

    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject GtasksListService gtasksListService;
    @Inject GtasksMetadataService gtasksMetadataService;
    @Inject MetadataDao metadataDao;
    @Inject TaskService taskService;
    @Inject GtasksMetadata gtasksMetadata;

    public void testBasicParentComputation() {
        Task[] tasks = givenTasksABCDE();

        whenCalculatingParentsAndSiblings();

        thenExpectParent(tasks[0], null);
        thenExpectParent(tasks[1], tasks[0]);
        thenExpectParent(tasks[2], tasks[0]);
        thenExpectParent(tasks[3], tasks[2]);
        thenExpectParent(tasks[4], null);
    }

    public void disabled_testBasicSiblingComputation() {
        Task[] tasks = givenTasksABCDE();

        whenCalculatingParentsAndSiblings();

        thenExpectSibling(tasks[0], null);
        thenExpectSibling(tasks[1], null);
        thenExpectSibling(tasks[2], tasks[1]);
        thenExpectSibling(tasks[3], null);
        thenExpectSibling(tasks[4], tasks[0]);
    }

    public void disabled_testMetadataParentComputation() {
        Task[] tasks = givenTasksABCDE();

        whenCalculatingOrder();

        thenExpectMetadataParent(tasks[0], null);
        thenExpectMetadataParent(tasks[1], tasks[0]);
        thenExpectMetadataParent(tasks[2], tasks[0]);
        thenExpectMetadataParent(tasks[3], tasks[2]);
        thenExpectMetadataParent(tasks[4], null);
    }

    public void disabled_testMetadataOrderComputation() {
        Task[] tasks = givenTasksABCDE();

        whenCalculatingOrder();

        thenExpectMetadataIndentAndOrder(tasks[0], 0, 0);
        thenExpectMetadataIndentAndOrder(tasks[1], 1, 1);
        thenExpectMetadataIndentAndOrder(tasks[2], 2, 1);
        thenExpectMetadataIndentAndOrder(tasks[3], 3, 2);
        thenExpectMetadataIndentAndOrder(tasks[4], 4, 0);
    }

    public void disabled_testNewTaskOrder() {
        givenTasksABCDE();

        Task newTask = createTask("F", VALUE_UNSET, 0);
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
            assertEquals("Task " + task.getTitle() + " parent none", 0, parent);
        else
            assertEquals("Task " + task.getTitle() + " parent " +
                    expectedParent.getTitle(), expectedParent.getId(), parent);
    }

    private void thenExpectSibling(Task task, Task expectedSibling) {
        long sibling = gtasksTaskListUpdater.siblings.get(task.getId());
        if(expectedSibling == null)
            assertEquals("Task " + task.getTitle() + " sibling null", 0L, sibling);
        else
            assertEquals("Task " + task.getTitle() + " sibling " +
                    expectedSibling.getTitle(), expectedSibling.getId(), sibling);
    }

    private void thenExpectParent(Task task, Task expectedParent) {
        long parent = gtasksTaskListUpdater.parents.get(task.getId());
        if(expectedParent == null)
            assertEquals("Task " + task.getTitle() + " parent null", 0L, parent);
        else
            assertEquals("Task " + task.getTitle() + " parent " +
                    expectedParent.getTitle(), expectedParent.getId(), parent);
    }

    @Override
    protected void setUp() {
        super.setUp();

        TaskLists lists = new TaskLists();
        List<TaskList> items = new ArrayList<>();
        TaskList list = new TaskList();
        list.setId("1");
        list.setTitle("Tim's Tasks");
        items.add(list);
        lists.setItems(items);
        gtasksListService.updateLists(lists);
    }

    private void whenCalculatingParentsAndSiblings() {
        createParentSiblingMaps();
    }

    void createParentSiblingMaps() {
        for(GtasksList list : gtasksListService.getLists()) {
            gtasksTaskListUpdater.updateParentSiblingMapsFor(list);
        }
    }

    private void whenCalculatingOrder() {
        for(GtasksList list : gtasksListService.getLists())
            gtasksTaskListUpdater.correctMetadataForList(list.getRemoteId());
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
        task.setTitle(title);
        taskService.save(task);
        Metadata metadata = gtasksMetadata.createEmptyMetadata(task.getId());
        metadata.setValue(GtasksMetadata.LIST_ID, "1");
        if(order != VALUE_UNSET)
            metadata.setValue(GtasksMetadata.ORDER, order);
        if(indent != VALUE_UNSET)
            metadata.setValue(GtasksMetadata.INDENT, indent);
        metadataDao.persist(metadata);
        return task;
    }

}//*/
