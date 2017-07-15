/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.support.test.runner.AndroidJUnit4;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.TestComponent;

import java.util.List;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class TaskDaoTests extends DatabaseTestCase {

    public static Property<?>[] IDS = new Property<?>[] { Task.ID };

    public static Property<?>[] TITLES = new Property<?>[] { Task.ID,
            Task.TITLE };

    @Inject TaskDao taskDao;

    /**
     * Test basic task creation, fetch, and save
     */
    @Test
    public void testTaskCreation() {
        assertEquals(0, taskDao.toList(Query.select(IDS)).size());

        // create task "happy"
        Task task = new Task();
        task.setTitle("happy");
        taskDao.save(task);
        assertEquals(1, taskDao.toList(Query.select(IDS)).size());
        long happyId = task.getId();
        assertNotSame(Task.NO_ID, happyId);
        task = taskDao.fetch(happyId, TITLES);
        assertEquals("happy", task.getTitle());

        // create task "sad"
        task = new Task();
        task.setTitle("sad");
        taskDao.save(task);
        assertEquals(2, taskDao.toList(Query.select(IDS)).size());

        // rename sad to melancholy
        long sadId = task.getId();
        assertNotSame(Task.NO_ID, sadId);
        task.setTitle("melancholy");
        taskDao.save(task);
        assertEquals(2, taskDao.toList(Query.select(IDS)).size());

        // check state
        task = taskDao.fetch(happyId, TITLES);
        assertEquals("happy", task.getTitle());
        task = taskDao.fetch(sadId,TITLES);
        assertEquals("melancholy", task.getTitle());
    }

    /**
     * Test various task fetch conditions
     */
    @Test
    public void testTaskConditions() {
        // create normal task
        Task task = new Task();
        task.setTitle("normal");
        taskDao.save(task);

        // create blank task
        task = new Task();
        task.setTitle("");
        taskDao.save(task);

        // create hidden task
        task = new Task();
        task.setTitle("hidden");
        task.setHideUntil(DateUtilities.now() + 10000);
        taskDao.save(task);

        // create task with deadlines
        task = new Task();
        task.setTitle("deadlineInFuture");
        task.setDueDate(DateUtilities.now() + 10000);
        taskDao.save(task);

        task = new Task();
        task.setTitle("deadlineInPast");
        task.setDueDate(DateUtilities.now() - 10000);
        taskDao.save(task);

        // create completed task
        task = new Task();
        task.setTitle("completed");
        task.setCompletionDate(DateUtilities.now() - 10000);
        taskDao.save(task);

        // check has no name
        List<Task> tasks = taskDao.toList(Query.select(TITLES).where(TaskCriteria.hasNoTitle()));
        assertEquals(1, tasks.size());
        assertEquals("", tasks.get(0).getTitle());

        // check is active
        assertEquals(5, taskDao.toList(Query.select(TITLES).where(TaskCriteria.isActive())).size());

        // check is visible
        assertEquals(5, taskDao.toList(Query.select(TITLES).where(TaskCriteria.isVisible())).size());
    }

    /**
     * Test task deletion
     */
    @Test
    public void testTDeletion() {
        assertEquals(0, taskDao.toList(Query.select(IDS)).size());

        // create task "happy"
        Task task = new Task();
        task.setTitle("happy");
        taskDao.save(task);
        assertEquals(1, taskDao.toList(Query.select(IDS)).size());

        // delete
        long happyId = task.getId();
        assertTrue(taskDao.delete(happyId));
        assertEquals(0, taskDao.toList(Query.select(IDS)).size());
    }

    /**
     * Test save without prior create doesn't work
     */
    @Test
    public void testSaveWithoutCreate() {
        // try to save task "happy"
        Task task = new Task();
        task.setTitle("happy");
        task.setID(1L);

        taskDao.save(task);

        assertEquals(0, taskDao.toList(Query.select(IDS)).size());
    }

    /**
     * Test passing invalid task indices to various things
     */
    @Test
    public void testInvalidIndex() {
        assertEquals(0, taskDao.toList(Query.select(IDS)).size());

        assertNull(taskDao.fetch(1, IDS));

        assertFalse(taskDao.delete(1));

        // make sure db still works
        assertEquals(0, taskDao.toList(Query.select(IDS)).size());
    }

    @Override
    protected void inject(TestComponent component) {
        component.inject(this);
    }

    // TODO check eventing
}

