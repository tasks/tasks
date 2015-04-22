package org.tasks.reminders;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.SnoozeCallback;
import com.todoroo.astrid.reminders.SnoozeDialog;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.injection.InjectingActivity;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

public class SnoozeActivity extends InjectingActivity {

    public static final String TASK_ID = "id";

    @Inject StartupService startupService;
    @Inject ActivityPreferences preferences;
    @Inject TaskService taskService;
    @Inject NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences.applyTranslucentDialogTheme();
        startupService.onStartupApplication(this);
        long taskId = getIntent().getLongExtra(TASK_ID, 0L);
        snooze(taskId);
    }

    private void snooze(final long taskId) {
        SnoozeCallback callback = new SnoozeCallback() {
            @Override
            public void snoozeForTime(long time) {
                setResult(RESULT_OK);
                Task task = new Task();
                task.setId(taskId);
                task.setReminderSnooze(time);
                taskService.save(task);
                notificationManager.cancel(taskId);
                finish();
            }
        };
        SnoozeDialog sd = new SnoozeDialog(this, callback);
        new AlertDialog.Builder(this)
                .setTitle(R.string.rmd_NoA_snooze)
                .setView(sd)
                .setPositiveButton(android.R.string.ok, sd)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                })
                .show().setOwnerActivity(this);
    }
}
