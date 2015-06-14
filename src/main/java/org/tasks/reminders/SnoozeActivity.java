package org.tasks.reminders;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.SnoozeCallback;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;

import org.tasks.activities.TimePickerActivity;
import org.tasks.injection.InjectingFragmentActivity;
import org.tasks.notifications.NotificationManager;

import javax.inject.Inject;

public class SnoozeActivity extends InjectingFragmentActivity implements SnoozeCallback, DialogInterface.OnCancelListener {

    private static final String FRAG_TAG_SNOOZE_DIALOG = "frag_tag_snooze_dialog";

    public static final String EXTRA_TASK_ID = "id";

    @Inject StartupService startupService;
    @Inject TaskService taskService;
    @Inject NotificationManager notificationManager;

    private long taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startupService.onStartupApplication(this);

        FragmentManager supportFragmentManager = getSupportFragmentManager();
        SnoozeDialog fragmentByTag = (SnoozeDialog) supportFragmentManager.findFragmentByTag(FRAG_TAG_SNOOZE_DIALOG);
        if (fragmentByTag == null) {
            fragmentByTag = new SnoozeDialog();
            fragmentByTag.show(supportFragmentManager, FRAG_TAG_SNOOZE_DIALOG);
        }
        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, 0L);
        fragmentByTag.setOnCancelListener(this);
        fragmentByTag.setSnoozeCallback(this);
    }

    @Override
    public void snoozeForTime(long time) {
        Task task = new Task();
        task.setId(taskId);
        task.setReminderSnooze(time);
        taskService.save(task);
        notificationManager.cancel(taskId);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SnoozeDialog.REQUEST_DATE_TIME) {
            if (resultCode == RESULT_OK && data != null) {
                snoozeForTime(data.getLongExtra(TimePickerActivity.EXTRA_TIMESTAMP, 0L));
            } else {
                finish();
            }
            overridePendingTransition(0, 0);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
