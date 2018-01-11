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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@SuppressWarnings("nls")
@RunWith(AndroidJUnit4.class)
public class GtasksIndentActionTest extends InjectingTestCase {

    @Inject GtasksListService gtasksListService;
    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject TaskDao taskDao;
    @Inject GoogleTaskDao googleTaskDao;

    private Task task;
    private GtasksList storeList;

    @Test
    public void testIndentWithoutMetadata() {
        givenTask(taskWithoutMetadata());

        whenIncreaseIndent();

        // should not crash
    }

    @Test
    public void disabled_testIndentWithMetadataButNoOtherTasks() {
        givenTask(taskWithMetadata(0, 0));

        whenIncreaseIndent();

        thenExpectIndentationLevel(0);
    }

    @Test
    public void testIndentWithMetadata() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 0));

        whenIncreaseIndent();

        thenExpectIndentationLevel(1);
    }

    @Test
    public void testDeindentWithMetadata() {
        givenTask(taskWithMetadata(0, 1));

        whenDecreaseIndent();

        thenExpectIndentationLevel(0);
    }

    @Test
    public void testDeindentWithoutMetadata() {
        givenTask(taskWithoutMetadata());

        whenDecreaseIndent();

        // should not crash
    }

    @Test
    public void testDeindentWhenAlreadyZero() {
        givenTask(taskWithMetadata(0, 0));

        whenDecreaseIndent();

        thenExpectIndentationLevel(0);
    }

    @Test
    public void testIndentWithChildren() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 0));
        Task child = taskWithMetadata(2, 1);

        whenIncreaseIndent();

        thenExpectIndentationLevel(1);
        thenExpectIndentationLevel(child, 2);
    }

    @Test
    public void testDeindentWithChildren() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 1));
        Task child = taskWithMetadata(2, 2);

        whenDecreaseIndent();

        thenExpectIndentationLevel(0);
        thenExpectIndentationLevel(child, 1);
    }

    @Test
    public void testIndentWithSiblings() {
        taskWithMetadata(0, 0);
        givenTask(taskWithMetadata(1, 0));
        Task sibling = taskWithMetadata(2, 0);

        whenIncreaseIndent();

        thenExpectIndentationLevel(1);
        thenExpectIndentationLevel(sibling, 0);
    }

    @Test
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
        gtasksTaskListUpdater.indent(storeList, task.getId(), 1);
    }

    private void whenDecreaseIndent() {
        gtasksTaskListUpdater.indent(storeList, task.getId(), -1);
    }

    @Override
    public void setUp() {
        super.setUp();

        List<TaskList> items = new ArrayList<>();
        TaskList list = new TaskList();
        list.setId("list");
        list.setTitle("Test Tasks");
        items.add(list);
        gtasksListService.updateLists(items);

        storeList = gtasksListService.getLists().get(0);
    }

    @Override
    protected void inject(TestComponent component) {
        component.inject(this);
    }

    private Task taskWithMetadata(long order, int indentation) {
        Task newTask = new Task();
        taskDao.save(newTask);
        GoogleTask metadata = new GoogleTask(newTask.getId(), "list");
        metadata.setIndent(indentation);
        metadata.setOrder(order);
        googleTaskDao.insert(metadata);
        return newTask;
    }

    private void thenExpectIndentationLevel(int expected) {
        thenExpectIndentationLevel(task, expected);
    }

    private void thenExpectIndentationLevel(Task targetTask, int expected) {
        GoogleTask metadata = googleTaskDao.getByTaskId(targetTask.getId());
        assertNotNull("task has metadata", metadata);
        int indentation = metadata.getIndent();
        assertTrue("indentation: " + indentation,
                indentation == expected);
    }

    private void givenTask(Task taskToTest) {
        task = taskToTest;
    }

    private Task taskWithoutMetadata() {
        Task task = new Task();
        taskDao.save(task);
        return task;
    }
}
