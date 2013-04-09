package com.todoroo.astrid.actfm.sync;

import java.util.ArrayList;
import java.util.List;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.TaskOutstandingDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;

public class EmptyTitleOutstandingEntryMigration {

    @Autowired
    private TaskOutstandingDao taskOutstandingDao;

    public void performMigration() {
        try {
            TodorooCursor<TaskOutstanding> outstandingWithTitle = taskOutstandingDao
                    .query(Query.select(TaskOutstanding.TASK_ID)
                            .where(Criterion.and(TaskOutstanding.COLUMN_STRING.eq(Task.TITLE.name),
                                    Criterion.or(TaskOutstanding.VALUE_STRING.isNotNull(), TaskOutstanding.VALUE_STRING.neq("")))) //$NON-NLS-1$
                                    .groupBy(TaskOutstanding.TASK_ID));
                List<Long> ids = new ArrayList<Long>();
                for (outstandingWithTitle.moveToFirst(); !outstandingWithTitle.isAfterLast(); outstandingWithTitle.moveToNext()) {
                    try {
                        ids.add(outstandingWithTitle.get(TaskOutstanding.TASK_ID));
                    } catch (Exception e) {
                        //
                    }
                }

                taskOutstandingDao.deleteWhere(Criterion.and(TaskOutstanding.TASK_ID.in(ids.toArray(new Long[ids.size()])),
                        TaskOutstanding.COLUMN_STRING.eq(Task.TITLE.name),
                        Criterion.or(TaskOutstanding.VALUE_STRING.isNull(), TaskOutstanding.VALUE_STRING.eq("")))); //$NON-NLS-1$
        } catch (Exception e) {
            //
        }
    }

}
