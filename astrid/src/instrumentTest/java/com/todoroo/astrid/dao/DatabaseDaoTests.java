package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

public class DatabaseDaoTests extends DatabaseTestCase {

    private TaskDao dao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        RemoteModelDao.setOutstandingEntryFlags(RemoteModelDao.OUTSTANDING_ENTRY_FLAG_RECORD_OUTSTANDING);
    }

    public void testFailedTransactionCreatesNoRows() {
        dao = new TaskDao();
        dao.setDatabase(database);

        Task t = new Task();
        t.setValue(Task.TITLE, "Should not appear");
        dao.createNew(t);

        TodorooCursor<Task> tasks = dao.query(Query.select(Task.ID));
        try {
            assertEquals(0, tasks.getCount());
        } finally {
            tasks.close();
        }
    }

}
