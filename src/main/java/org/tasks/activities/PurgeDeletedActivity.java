package org.tasks.activities;

import android.content.DialogInterface;
import android.os.Bundle;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.ui.ProgressDialogAsyncTask;

import javax.inject.Inject;

public class PurgeDeletedActivity extends InjectingAppCompatActivity {

    @Inject TaskService taskService;
    @Inject TaskDeleter taskDeleter;
    @Inject GCalHelper gcalHelper;
    @Inject MetadataDao metadataDao;
    @Inject DialogBuilder dialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialogBuilder.newMessageDialog(R.string.EPr_manage_purge_deleted_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new ProgressDialogAsyncTask(PurgeDeletedActivity.this, dialogBuilder) {
                            @Override
                            protected Integer doInBackground(Void... params) {
                                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.TITLE, Task.CALENDAR_URI).where(
                                        Criterion.and(Task.DELETION_DATE.gt(0), Task.CALENDAR_URI.isNotNull())));
                                try {
                                    int length = cursor.getCount();
                                    for (int i = 0; i < length; i++) {
                                        cursor.moveToNext();
                                        Task task = new Task(cursor);
                                        gcalHelper.deleteTaskEvent(task);
                                    }
                                } finally {
                                    cursor.close();
                                }
                                int result = taskDeleter.purgeDeletedTasks();
                                metadataDao.removeDanglingMetadata();
                                return result;
                            }

                            @Override
                            protected int getResultResource() {
                                return R.string.EPr_manage_purge_deleted_status;
                            }
                        }.execute();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();
    }
}
