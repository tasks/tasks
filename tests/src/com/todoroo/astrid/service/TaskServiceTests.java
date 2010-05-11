package com.todoroo.astrid.service;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.test.data.TodorooCursor;
import com.todoroo.andlib.test.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.test.DatabaseTestCase;


public class TaskServiceTests extends DatabaseTestCase {

    @Autowired
    TaskService taskService;

    @Autowired
    TaskDao taskDao;
    
    /** 
     * Since a lot of the service-layer methods call through to the dao,
     * we don't need to do a lot of extensive testing here, just various
     * boundary conditions.
     */
    public void testTaskDaoExposedMethods() {
        Task task = new Task();
        task.setValue(Task.TITLE, "normal");
        assertTrue(taskService.save(task, false));
        
        // empty fetch w/ filter
        Filter filter = new Filter("bla", "bla", "WHERE 1", null);
        TodorooCursor<Task> cursor = 
            taskService.fetchFiltered(TaskService.idProperties(), filter);
        assertEquals(1, cursor.getCount());
        cursor.close();
        
        filter.sqlQuery = "WHERE " + Task.TITLE + " = \"bob\"";
        cursor = taskService.fetchFiltered(TaskService.idProperties(), filter);
        assertEquals(0, cursor.getCount());
        cursor.close();
        
        Task fetched = taskService.fetchById(TaskService.idProperties(), task.getId());
        assertNotNull(fetched);
        fetched = taskService.fetchById(TaskService.idProperties(), task.getId() + 1);
        assertNull(fetched);
        
        taskService.setComplete(task, true);
        assertTrue(task.isCompleted());
        
        fetched = taskService.fetchById(Task.PROPERTIES, task.getId());
        assertTrue(fetched.isCompleted());
        taskService.setComplete(task, false);
        fetched = taskService.fetchById(Task.PROPERTIES, task.getId());
        assertFalse(fetched.isCompleted());
        assertFalse(task.isCompleted());
        
        long id = task.getId();
        taskService.delete(id);
        fetched = taskService.fetchById(Task.PROPERTIES, id);
        assertNull(fetched);
    }
    
    /**
     * Test cleanup
     */
    public void testCleanup() throws Exception {
        TodorooCursor<Task> cursor;

        // save task with a name
        Task task1 = new Task();
        task1.setValue(Task.TITLE, "normal");
        assertTrue(taskService.save(task1, false));

        // save task without a name
        Task task2 = new Task();
        task2.setValue(Task.TITLE, "");
        task2.setValue(Task.HIDDEN_UNTIL, DateUtilities.now() + 10000);
        assertTrue(taskService.save(task2, false));

        // save task #2 without a name
        Task task3 = new Task();
        task3.setValue(Task.TITLE, "");
        task3.setValue(Task.DUE_DATE, DateUtilities.now() + 10000);
        assertTrue(taskService.save(task3, false));

        cursor = taskDao.fetch(database, Task.PROPERTIES, null, null);
        assertEquals(3, cursor.getCount());
        cursor.close();
        
        taskService.cleanup();
        
        cursor = taskDao.fetch(database, Task.PROPERTIES, null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();
    }

    /**
     * Test the sql invoked from a filter on new task creation
     */
    public void testInvokeNewTaskSql() {
        TodorooCursor<Task> cursor;
        Filter filter = new Filter("bla", "bla", "WHERE 1", null);
        
        Task task = new Task();
        task.setValue(Task.TITLE, "evils");
        assertTrue(taskService.save(task, false));
        
        cursor = taskDao.fetch(database, Task.PROPERTIES, null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToNext();
        task = new Task(cursor, Task.PROPERTIES);
        assertEquals("evils", task.getValue(Task.TITLE));
        cursor.close();
        
        filter.sqlForNewTasks = String.format("UPDATE %s set %s = \"good\"",
                Database.TASK_TABLE, Task.TITLE);
        taskService.invokeSqlForNewTask(filter, task);
        
        cursor = taskDao.fetch(database, Task.PROPERTIES, null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToNext();
        task = new Task(cursor, Task.PROPERTIES);
        assertEquals("good", task.getValue(Task.TITLE));
        cursor.close();
        
        filter.sqlForNewTasks = String.format("UPDATE %s set %s = \"yum\" WHERE %s = $ID",
                Database.TASK_TABLE, Task.TITLE, Task.ID);
        taskService.invokeSqlForNewTask(filter, task);
        
        cursor = taskDao.fetch(database, Task.PROPERTIES, null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToNext();
        task = new Task(cursor, Task.PROPERTIES);
        assertEquals("yum", task.getValue(Task.TITLE));
        cursor.close();
    }
    

}
