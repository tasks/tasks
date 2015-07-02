package org.tasks.activities;

import android.content.DialogInterface;
import android.os.Bundle;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.ui.ProgressDialogAsyncTask;

import javax.inject.Inject;

public class DeleteAllCalendarEventsActivity extends InjectingAppCompatActivity {

    @Inject TaskService taskService;
    @Inject GCalHelper gcalHelper;
    @Inject DialogBuilder dialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialogBuilder.newMessageDialog(R.string.EPr_manage_delete_all_gcal_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new ProgressDialogAsyncTask(DeleteAllCalendarEventsActivity.this, dialogBuilder) {
                            @Override
                            protected Integer doInBackground(Void... params) {
                                int deletedEventCount = 0;
                                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.CALENDAR_URI).where(
                                        Task.CALENDAR_URI.isNotNull()));
                                try {
                                    int length = cursor.getCount();
                                    for (int i = 0; i < length; i++) {
                                        cursor.moveToNext();
                                        Task task = new Task(cursor);
                                        if (gcalHelper.deleteTaskEvent(task)) {
                                            deletedEventCount++;
                                        }
                                    }
                                } finally {
                                    cursor.close();
                                }
                                // mass update the CALENDAR_URI here,
                                // since the GCalHelper doesnt save it due to performance-reasons
                                Task template = new Task();
                                template.setCalendarUri(""); //$NON-NLS-1$
                                taskService.update(Task.CALENDAR_URI.isNotNull(), template);
                                return deletedEventCount;
                            }

                            @Override
                            protected int getResultResource() {
                                return R.string.EPr_manage_delete_all_gcal_status;
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
