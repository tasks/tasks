package org.tasks.reminders;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.SnoozeCallback;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;

import org.tasks.time.DateTime;
import org.tasks.activities.DateAndTimePickerActivity;
import org.tasks.activities.TimePickerActivity;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.notifications.NotificationManager;

import javax.inject.Inject;

public class SnoozeActivity extends InjectingAppCompatActivity implements SnoozeCallback, DialogInterface.OnCancelListener {

    private static final String FRAG_TAG_SNOOZE_DIALOG = "frag_tag_snooze_dialog";
    private static final String EXTRA_PICKING_DATE_TIME = "extra_picking_date_time";
    private static final int REQUEST_DATE_TIME = 10101;

    public static final String EXTRA_TASK_ID = "id";

    @Inject StartupService startupService;
    @Inject TaskService taskService;
    @Inject NotificationManager notificationManager;

    private long taskId;
    private boolean pickingDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setup(getIntent(), savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setup(intent, null);
    }

    private void setup(Intent intent, Bundle savedInstanceState) {
        taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L);

        if (savedInstanceState != null) {
            pickingDateTime = savedInstanceState.getBoolean(EXTRA_PICKING_DATE_TIME, false);
            if (pickingDateTime) {
                return;
            }
        }

        startupService.onStartupApplication(this);

        FragmentManager fragmentManager = getFragmentManager();
        SnoozeDialog fragmentByTag = (SnoozeDialog) fragmentManager.findFragmentByTag(FRAG_TAG_SNOOZE_DIALOG);
        if (fragmentByTag == null) {
            fragmentByTag = new SnoozeDialog();
            fragmentByTag.show(fragmentManager, FRAG_TAG_SNOOZE_DIALOG);
        }
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_PICKING_DATE_TIME, pickingDateTime);
    }

    @Override
    public void pickDateTime() {
        pickingDateTime = true;

        startActivityForResult(new Intent(this, DateAndTimePickerActivity.class) {{
            putExtra(DateAndTimePickerActivity.EXTRA_TIMESTAMP, new DateTime().plusMinutes(30).getMillis());
        }}, REQUEST_DATE_TIME);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DATE_TIME) {
            if (resultCode == RESULT_OK && data != null) {
                snoozeForTime(data.getLongExtra(TimePickerActivity.EXTRA_TIMESTAMP, 0L));
            } else {
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
