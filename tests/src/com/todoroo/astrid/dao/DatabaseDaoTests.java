package com.todoroo.astrid.dao;

import android.content.ContentValues;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;
import com.todoroo.astrid.test.DatabaseTestCase;

public class DatabaseDaoTests extends DatabaseTestCase {

    private TaskDao dao;
    private TaskOutstandingDao outstandingDao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        RemoteModelDao.setOutstandingEntryFlags(RemoteModelDao.OUTSTANDING_ENTRY_FLAG_RECORD_OUTSTANDING);
    }

    public void testFailedTransactionCreatesNoRows() {
        dao = new TaskDao() {
            @Override
            protected int createOutstandingEntries(long modelId, ContentValues modelSetValues) {
                super.createOutstandingEntries(modelId, modelSetValues);
                return -1;
            }
        };
        dao.setDatabase(database);

        outstandingDao = new TaskOutstandingDao();
        outstandingDao.setDatabase(database);

        Task t = new Task();
        t.setValue(Task.TITLE, "Should not appear");
        dao.createNew(t);

        TodorooCursor<Task> tasks = dao.query(Query.select(Task.ID));
        try {
            assertEquals(0, tasks.getCount());
        } finally {
            tasks.close();
        }

        TodorooCursor<TaskOutstanding> outstanding = outstandingDao.query(Query.select(TaskOutstanding.ID));
        try {
            assertEquals(0, outstanding.getCount());
        } finally {
            outstanding.close();
        }
    }

}
