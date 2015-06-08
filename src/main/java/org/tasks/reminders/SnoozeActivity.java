package org.tasks.reminders;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.SnoozeCallback;
import com.todoroo.astrid.reminders.SnoozeDialog;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.injection.InjectingFragmentActivity;
import org.tasks.notifications.NotificationManager;

import javax.inject.Inject;

public class SnoozeActivity extends InjectingFragmentActivity implements SnoozeCallback, DialogInterface.OnDismissListener {

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
        fragmentByTag.setOnDismissListener(this);
        fragmentByTag.setTitle(getString(R.string.rmd_NoA_snooze));
        fragmentByTag.setSnoozeCallback(this);
    }

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

    @Override
    public void onDismiss(DialogInterface dialog) {
        setResult(RESULT_CANCELED);
        finish();
    }
}
