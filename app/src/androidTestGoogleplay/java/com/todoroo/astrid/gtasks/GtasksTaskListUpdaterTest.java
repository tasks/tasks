/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.support.test.runner.AndroidJUnit4;

import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@SuppressWarnings("nls")
@RunWith(AndroidJUnit4.class)
public class GtasksTaskListUpdaterTest extends InjectingTestCase {

    private static final int VALUE_UNSET = -1;

    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject GtasksListService gtasksListService;
    @Inject TaskDao taskDao;
    @Inject GoogleTaskDao googleTaskDao;

    @Test
    public void testBasicParentComputation() {
        Task[] tasks = givenTasksABCDE();

        whenCalculatingParentsAndSiblings();

        thenExpectParent(tasks[0], null);
        thenExpectParent(tasks[1], tasks[0]);
        thenExpectParent(tasks[2], tasks[0]);
        thenExpectParent(tasks[3], tasks[2]);
        thenExpectParent(tasks[4], null);
    }

    @Test
    public void testBasicSiblingComputation() {
        Task[] tasks = givenTasksABCDE();

        whenCalculatingParentsAndSiblings();

        thenExpectSibling(tasks[0], null);
        thenExpectSibling(tasks[1], null);
        thenExpectSibling(tasks[2], tasks[1]);
        thenExpectSibling(tasks[3], null);
        thenExpectSibling(tasks[4], tasks[0]);
    }

    @Ignore
    @Test
    public void testMetadataParentComputation() {
        Task[] tasks = givenTasksABCDE();

        thenExpectMetadataParent(tasks[0], null);
        thenExpectMetadataParent(tasks[1], tasks[0]);
        thenExpectMetadataParent(tasks[2], tasks[0]);
        thenExpectMetadataParent(tasks[3], tasks[2]);
        thenExpectMetadataParent(tasks[4], null);
    }

    @Test
    public void testMetadataOrderComputation() {
        Task[] tasks = givenTasksABCDE();

        thenExpectMetadataIndentAndOrder(tasks[0], 0, 0);
        thenExpectMetadataIndentAndOrder(tasks[1], 1, 1);
        thenExpectMetadataIndentAndOrder(tasks[2], 2, 1);
        thenExpectMetadataIndentAndOrder(tasks[3], 3, 2);
        thenExpectMetadataIndentAndOrder(tasks[4], 4, 0);
    }

    @Ignore
    @Test
    public void testNewTaskOrder() {
        givenTasksABCDE();

        Task newTask = createTask("F", VALUE_UNSET, 0);

        thenExpectMetadataIndentAndOrder(newTask, 5, 0);
    }


    // --- helpers

    private void thenExpectMetadataIndentAndOrder(Task task, long order, int indent) {
        GoogleTask metadata = googleTaskDao.getByTaskId(task.getId());
        assertNotNull("metadata was found", metadata);
        assertEquals("order", order, metadata.getOrder());
        assertEquals("indentation", indent, metadata.getIndent());
    }

    private void thenExpectMetadataParent(Task task, Task expectedParent) {
        GoogleTask metadata = googleTaskDao.getByTaskId(task.getId());
        long parent = metadata.getParent();
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
    public void setUp() {
        super.setUp();

        List<TaskList> items = new ArrayList<>();
        TaskList list = new TaskList();
        list.setId("1");
        list.setTitle("Tim's Tasks");
        items.add(list);
        gtasksListService.updateLists(items);
    }

    @Override
    protected void inject(TestComponent component) {
        component.inject(this);
    }

    private void whenCalculatingParentsAndSiblings() {
        createParentSiblingMaps();
    }

    void createParentSiblingMaps() {
        for(GtasksList list : gtasksListService.getLists()) {
            gtasksTaskListUpdater.updateParentSiblingMapsFor(list);
        }
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
        taskDao.save(task);
        GoogleTask metadata = new GoogleTask(task.getId(), "1");
        if(order != VALUE_UNSET) {
            metadata.setOrder(order);
        }
        if(indent != VALUE_UNSET) {
            metadata.setIndent(indent);
        }
        googleTaskDao.insert(metadata);
        return task;
    }
}
