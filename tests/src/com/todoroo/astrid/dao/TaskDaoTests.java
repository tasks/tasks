package com.todoroo.astrid.dao;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.test.data.Property;
import com.todoroo.andlib.test.data.TodorooCursor;
import com.todoroo.andlib.test.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao.TaskSql;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

public class TaskDaoTests extends DatabaseTestCase {

    public static Property<?>[] IDS = new Property<?>[] { Task.ID };

    public static Property<?>[] TITLES = new Property<?>[] { Task.ID,
            Task.TITLE };

    @Autowired
    TaskDao taskDao;

    /**
     * Test basic task creation, fetch, and save
     */
    public void testTaskCreation() throws Exception {
        TodorooCursor<Task> cursor = taskDao.fetch(database,
                IDS, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // create task "happy"
        Task task = new Task();
        task.setValue(Task.TITLE, "happy");
        assertTrue(taskDao.save(database, task, false));
        cursor = taskDao.fetch(database, IDS, null, null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();
        long happyId = task.getId();
        assertNotSame(Task.NO_ID, happyId);
        task = taskDao.fetch(database, TITLES, happyId);
        assertEquals("happy", task.getValue(Task.TITLE));

        // create task "sad"
        task = new Task();
        task.setValue(Task.TITLE, "sad");
        assertTrue(taskDao.save(database, task, false));
        cursor = taskDao.fetch(database, IDS, null, null, null);
        assertEquals(2, cursor.getCount());
        cursor.close();

        // rename sad to melancholy
        long sadId = task.getId();
        assertNotSame(Task.NO_ID, sadId);
        task.setValue(Task.TITLE, "melancholy");
        assertTrue(taskDao.save(database, task, false));
        cursor = taskDao.fetch(database, IDS, null, null, null);
        assertEquals(2, cursor.getCount());
        cursor.close();

        // check state
        task = taskDao.fetch(database, TITLES, happyId);
        assertEquals("happy", task.getValue(Task.TITLE));
        task = taskDao.fetch(database, TITLES, sadId);
        assertEquals("melancholy", task.getValue(Task.TITLE));
    }

    /**
     * Test various task fetch conditions
     */
    public void testTaskConditions() throws Exception {
        // create normal task
        Task task = new Task();
        task.setValue(Task.TITLE, "normal");
        assertTrue(taskDao.save(database, task, false));
        
        // create blank task
        task = new Task();
        task.setValue(Task.TITLE, "");
        assertTrue(taskDao.save(database, task, false));

        // create hidden task
        task = new Task();
        task.setValue(Task.TITLE, "hidden");
        task.setValue(Task.HIDDEN_UNTIL, DateUtilities.now() + 10000);
        assertTrue(taskDao.save(database, task, false));

        // create task with deadlines
        task = new Task();
        task.setValue(Task.TITLE, "deadlineInFuture");
        task.setValue(Task.DUE_DATE, DateUtilities.now() + 10000);
        assertTrue(taskDao.save(database, task, false));
        
        task = new Task();
        task.setValue(Task.TITLE, "deadlineInPast");
        task.setValue(Task.DUE_DATE, DateUtilities.now() - 10000);
        assertTrue(taskDao.save(database, task, false));

        // create completed task
        task = new Task();
        task.setValue(Task.TITLE, "completed");
        task.setValue(Task.COMPLETION_DATE, DateUtilities.now() - 10000);
        assertTrue(taskDao.save(database, task, false));

        // check has no name
        TodorooCursor<Task> cursor = taskDao.fetch(database,
                TITLES, TaskSql.hasNoName(), null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToNext();
        assertEquals("", cursor.getString(1));
        cursor.close();

        // check has deadlines
        cursor = taskDao.fetch(database, TITLES, TaskSql
                .hasDeadlines(), Task.DUE_DATE + " ASC", null);
        assertEquals(2, cursor.getCount());
        cursor.moveToNext();
        assertEquals("deadlineInPast", cursor.getString(1));
        cursor.moveToNext();
        assertEquals("deadlineInFuture", cursor.getString(1));
        cursor.close();

        // check is active
        cursor = taskDao.fetch(database, TITLES, TaskSql
                .isActive(), null, null);
        assertEquals(5, cursor.getCount());
        cursor.close();

        // check due before / after
        cursor = taskDao.fetch(database, TITLES, TaskSql
                .dueBefore(DateUtilities.now()), null, null);
        cursor.moveToNext();
        assertEquals(1, cursor.getCount());
        cursor.close();
        cursor = taskDao.fetch(database, TITLES, TaskSql
                .dueAfter(DateUtilities.now()), null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();
        
        // check completed before
        cursor = taskDao.fetch(database, TITLES, TaskSql
                .completedBefore(DateUtilities.now()), null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();
        
        // check is visible
        cursor = taskDao.fetch(database, TITLES, TaskSql
                .isVisible(DateUtilities.now()), null, null);
        assertEquals(5, cursor.getCount());
        cursor.close();
    }

    /**
     * Test task deletion
     */
    public void testTDeletion() throws Exception {
        TodorooCursor<Task> cursor = taskDao.fetch(database,
                IDS, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // create task "happy"
        Task task = new Task();
        task.setValue(Task.TITLE, "happy");
        assertTrue(taskDao.save(database, task, false));
        cursor = taskDao.fetch(database, IDS, null, null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();

        // delete
        long happyId = task.getId();
        assertTrue(taskDao.delete(database, happyId));
        cursor = taskDao.fetch(database, IDS, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    /**
     * Test save without prior create doesn't work
     */
    public void testSaveWithoutCreate() throws Exception {
        TodorooCursor<Task> cursor;

        // try to save task "happy"
        Task task = new Task();
        task.setValue(Task.TITLE, "happy");
        task.setValue(Task.ID, 1L);

        assertFalse(taskDao.save(database, task, false));

        cursor = taskDao.fetch(database, IDS, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    /**
     * Test passing invalid task indices to various things
     */
    public void testInvalidIndex() throws Exception {
        TodorooCursor<Task> cursor;

        cursor = taskDao.fetch(database, IDS, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        assertNull(taskDao.fetch(database, IDS, 1));

        assertFalse(taskDao.delete(database, 1));

        // make sure db still works
        cursor = taskDao.fetch(database, IDS, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }
    
    // TODO check eventing
}

