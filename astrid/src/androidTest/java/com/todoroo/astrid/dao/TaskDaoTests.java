/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

import java.util.List;

import javax.inject.Inject;

public class TaskDaoTests extends DatabaseTestCase {

    public static Property<?>[] IDS = new Property<?>[] { Task.ID };

    public static Property<?>[] TITLES = new Property<?>[] { Task.ID,
            Task.TITLE };

    @Inject TaskDao taskDao;

    /**
     * Test basic task creation, fetch, and save
     */
    public void disabled_testTaskCreation() {
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
    public void disabled_testTaskConditions() {
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
    public void disabled_testTDeletion() {
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
    public void disabled_testSaveWithoutCreate() {
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
    public void disabled_testInvalidIndex() {
        assertEquals(0, taskDao.toList(Query.select(IDS)).size());

        assertNull(taskDao.fetch(1, IDS));

        assertFalse(taskDao.delete(1));

        // make sure db still works
        assertEquals(0, taskDao.toList(Query.select(IDS)).size());
    }

    // TODO check eventing
}

