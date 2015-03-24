package org.tasks.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.injection.InjectingActivity;

import javax.inject.Inject;

public class DeleteAllCalendarEventsActivity extends InjectingActivity {

    @Inject TaskService taskService;
    @Inject GCalHelper gcalHelper;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DialogUtilities.okCancelDialog(
                this,
                getResources().getString(
                        R.string.EPr_manage_delete_all_gcal_message),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pd = DialogUtilities.runWithProgressDialog(DeleteAllCalendarEventsActivity.this, new Runnable() {
                            @Override
                            public void run() {
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
                                taskService.update(
                                        Task.CALENDAR_URI.isNotNull(),
                                        template);
                                showResult(R.string.EPr_manage_delete_all_gcal_status, deletedEventCount);
                            }
                        });
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
    }

    @Override
    protected void onPause() {
        DialogUtilities.dismissDialog(this, pd);

        super.onPause();
    }

    private void showResult(int resourceText, int result) {
        DialogUtilities.okDialog(this, getString(resourceText, result), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
    }
}
