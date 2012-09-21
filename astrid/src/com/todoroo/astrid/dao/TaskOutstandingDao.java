package com.todoroo.astrid.dao;

import com.todoroo.astrid.data.TaskOutstanding;

public class TaskOutstandingDao extends OutstandingEntryDao<TaskOutstanding> {

    public TaskOutstandingDao() {
        super(TaskOutstanding.class);
    }

}
