package com.todoroo.astrid.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.timsu.astrid.data.TaskController;
import com.timsu.astrid.data.TaskModelForEdit;
import com.timsu.astrid.data.TaskModelForList;
import com.timsu.astrid.provider.TasksProvider;
import com.todoroo.astrid.test.DatabaseTestCase;

public class TaskProviderTests extends DatabaseTestCase {

    /** test that we can start things up and query for tasks */
    public void testSimpleTaskQuery() {
        // create some stuff
        TaskController tc = new TaskController(getContext());
        tc.open();
        TaskModelForEdit task = new TaskModelForEdit();
        task.setName("abc");
        tc.saveTask(task, false);
        task = new TaskModelForEdit();
        task.setName("def");
        tc.saveTask(task, false);
        assertEquals(2, tc.getAllTaskIdentifiers().size());

        // query it
        Uri tasks = Uri.withAppendedPath(TasksProvider.CONTENT_URI, "tasks");
        ContentResolver resolver = getContext().getContentResolver();
        Cursor c = resolver.query(tasks, null, null, null, null);
        assertEquals(2, c.getCount());
        c.moveToFirst();
        assertEquals("abc", c.getString(0));
        c.moveToNext();
        assertEquals("def", c.getString(0));

        // complete one
        Cursor c2 = tc.getAllTaskListCursor();
        c2.moveToFirst();
        TaskModelForList listTask = new TaskModelForList(c2);
        listTask.setProgressPercentage(TaskModelForList.COMPLETE_PERCENTAGE);
        tc.saveTask(listTask, false);
        tc.close();

        // should be gone
        c = resolver.query(tasks, new String[] { "name" }, null, null, null);
        assertEquals(1, c.getCount());
    }

    /** test that we can update a task */
    public void testSimpleTaskUpdate() {
        // create some stuff
        TaskController tc = new TaskController(getContext());
        tc.open();
        TaskModelForEdit task1 = new TaskModelForEdit();
        task1.setName("solve world peace");
        tc.saveTask(task1, false);
        TaskModelForEdit task2 = new TaskModelForEdit();
        task2.setName("solve P = NP");
        tc.saveTask(task2, false);
        assertEquals(2, tc.getAllTaskIdentifiers().size());
        tc.close();

        // query provider
        Uri tasks = Uri.withAppendedPath(TasksProvider.CONTENT_URI, "tasks");
        ContentResolver resolver = getContext().getContentResolver();
        Cursor c = resolver.query(tasks, null, null, null, null);
        assertEquals(2, c.getCount());
        c.moveToFirst();
        assertEquals(task1.getName(), c.getString(0));
        long id = c.getLong(5);
        assertEquals(task1.getTaskIdentifier().getId(), id);
        c.moveToNext();
        assertEquals(task2.getName(), c.getString(0));

        // complete first task
        ContentValues values = new ContentValues();
        values.put("completed", true);
        int result = resolver.update(tasks, values, "_id = " + id, null);

        // query
        c = resolver.query(tasks, null, null, null, null);
        assertEquals(1, c.getCount());
        c.moveToFirst();
        assertEquals(task2.getName(), c.getString(0));

        // uncomplete
        values.put("completed", false);
        resolver.update(tasks, values, "_id = ?", new String[] { Long.toString(id) });
        assertEquals(1, result);
        c = resolver.query(tasks, null, null, null, null);
        assertEquals(2, c.getCount());

    }

}
