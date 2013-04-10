package com.todoroo.astrid.actfm.sync;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.actfm.sync.messages.ConstructOutstandingTableFromMasterTable;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.TaskAttachmentOutstandingDao;
import com.todoroo.astrid.dao.TaskOutstandingDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskAttachmentOutstanding;
import com.todoroo.astrid.data.TaskOutstanding;

public class EmptyTitleOutstandingEntryMigration {

    private static final String ERROR_TAG = "empty-title-migrate"; //$NON-NLS-1$

    @Autowired
    private TaskOutstandingDao taskOutstandingDao;

    @Autowired
    private TaskAttachmentDao taskAttachmentDao;

    @Autowired
    private TaskAttachmentOutstandingDao taskAttachmentOutstandingDao;

    public EmptyTitleOutstandingEntryMigration() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public void performMigration() {
        TodorooCursor<TaskOutstanding> outstandingWithTitle = null;
        try {
            outstandingWithTitle = taskOutstandingDao
                    .query(Query.select(TaskOutstanding.TASK_ID, Task.UUID)
                            .join(Join.left(Task.TABLE, Task.ID.eq(TaskOutstanding.TASK_ID)))
                            .where(Criterion.and(TaskOutstanding.COLUMN_STRING.eq(Task.TITLE.name),
                                    Criterion.and(TaskOutstanding.VALUE_STRING.isNotNull(), TaskOutstanding.VALUE_STRING.neq("")))) //$NON-NLS-1$
                                    .groupBy(TaskOutstanding.TASK_ID));
                List<Long> ids = new ArrayList<Long>();
                List<String> uuids = new ArrayList<String>();
                for (outstandingWithTitle.moveToFirst(); !outstandingWithTitle.isAfterLast(); outstandingWithTitle.moveToNext()) {
                    try {
                        ids.add(outstandingWithTitle.get(TaskOutstanding.TASK_ID));
                        uuids.add(outstandingWithTitle.get(Task.UUID));
                    } catch (Exception e) {
                        Log.e(ERROR_TAG, "Error reading from cursor", e); //$NON-NLS-1$
                    }
                }

                taskOutstandingDao.deleteWhere(Criterion.and(TaskOutstanding.TASK_ID.in(ids.toArray(new Long[ids.size()])),
                        TaskOutstanding.COLUMN_STRING.eq(Task.TITLE.name),
                        Criterion.or(TaskOutstanding.VALUE_STRING.isNull(), TaskOutstanding.VALUE_STRING.eq("")))); //$NON-NLS-1$

                new ConstructOutstandingTableFromMasterTable<TaskAttachment, TaskAttachmentOutstanding>(NameMaps.TABLE_ID_ATTACHMENTS,
                        taskAttachmentDao, taskAttachmentOutstandingDao, TaskAttachment.CREATED_AT).execute(TaskAttachment.TASK_UUID.in(uuids.toArray(new String[uuids.size()])));
        } catch (Exception e) {
            Log.e(ERROR_TAG, "Unhandled exception", e); //$NON-NLS-1$
        } finally {
            if (outstandingWithTitle != null)
                outstandingWithTitle.close();
        }
    }

}
