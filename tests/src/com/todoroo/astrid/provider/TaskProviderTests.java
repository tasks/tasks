package com.todoroo.astrid.provider;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import com.timsu.astrid.provider.TasksProvider;
import com.todoroo.andlib.test.TodorooTestCase;

public class TaskProviderTests extends TodorooTestCase {

    /** test that we can start things up and query for tasks */
    public void testSimpleTaskQuery() {
        ContentResolver resolver = getContext().getContentResolver();

        Uri tasks = Uri.withAppendedPath(TasksProvider.CONTENT_URI, "tasks");
        Cursor c = resolver.query(tasks, new String[] { "name" }, null, null, null);
        assertTrue(c.getCount() >= 0);
    }

}
