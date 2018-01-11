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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@SuppressWarnings("nls")
@RunWith(AndroidJUnit4.class)
public class GtasksTaskMovingTest extends InjectingTestCase {

    private static final int VALUE_UNSET = -1;

    @Inject GtasksListService gtasksListService;
    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject TaskDao taskDao;
    @Inject GoogleTaskDao googleTaskDao;

    private Task A, B, C, D, E, F;
    private GtasksList list;

    /* Starting State:
     *
     * A
     *  B
     *  C
     *   D
     * E
     * F
     */

    @Test
    public void testMoveDownFromListBottom() {
        givenTasksABCDEF();

        whenTriggerMove(F, null);

        thenExpectMetadataOrderAndIndent(E, 4, 0);
        thenExpectMetadataOrderAndIndent(F, 5, 0);
    }

    @Test
    public void testMoveDownToListBottom() {
        givenTasksABCDEF();

        whenTriggerMove(E, null);

        thenExpectMetadataOrderAndIndent(E, 5, 0);
        thenExpectMetadataOrderAndIndent(F, 4, 0);
    }

    @Test
    public void testMoveUpSimple() {
        givenTasksABCDEF();

        whenTriggerMove(F, E);

        thenExpectMetadataOrderAndIndent(E, 5, 0);
        thenExpectMetadataOrderAndIndent(F, 4, 0);
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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
        gtasksTaskListUpdater.moveTo(list, target.getId(), moveTo == null ? -1 : moveTo.getId());
    }

    private void thenExpectMetadataOrderAndIndent(Task task, long order, int indent) {
        GoogleTask metadata = googleTaskDao.getByTaskId(task.getId());
        assertNotNull("metadata was found", metadata);
        assertEquals("order", order, metadata.getOrder());
        assertEquals("indentation", indent, metadata.getIndent());
    }

    @Override
    public void setUp() {
        super.setUp();

        List<TaskList> items = new ArrayList<>();
        TaskList taskList = new TaskList();
        taskList.setId("1");
        taskList.setTitle("Tim's Tasks");
        items.add(taskList);
        gtasksListService.updateLists(items);

        list = gtasksListService.getLists().get(0);
    }

    @Override
    protected void inject(TestComponent component) {
        component.inject(this);
    }

    /**
     * A
     *  B
     *  C
     *   D
     * E
     * F
     */
    private void givenTasksABCDEF() {
        A = createTask("A", 0, 0);
        B = createTask("B", 1, 1);
        C = createTask("C", 2, 1);
        D = createTask("D", 3, 2);
        E = createTask("E", 4, 0);
        F = createTask("F", 5, 0);
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
