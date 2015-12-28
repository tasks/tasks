package org.tasks.reminders;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;

import org.tasks.Broadcaster;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.intents.TaskIntents;
import org.tasks.notifications.NotificationManager;

import javax.inject.Inject;

public class NotificationActivity extends InjectingAppCompatActivity implements NotificationDialog.NotificationHandler {

    private static final String FRAG_TAG_NOTIFICATION_FRAGMENT = "frag_tag_notification_fragment";

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_TASK_ID = "extra_task_id";

    @Inject Broadcaster broadcaster;
    @Inject NotificationManager notificationManager;
    private long taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setup(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setup(intent);
    }

    private void setup(Intent intent) {

        taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L);

        FragmentManager fragmentManager = getFragmentManager();
        NotificationDialog fragment = (NotificationDialog) fragmentManager.findFragmentByTag(FRAG_TAG_NOTIFICATION_FRAGMENT);
        if (fragment == null) {
            fragment = new NotificationDialog();
            fragment.show(fragmentManager, FRAG_TAG_NOTIFICATION_FRAGMENT);
        }
        fragment.setTitle(intent.getStringExtra(EXTRA_TITLE));
    }

    @Override
    public void dismiss() {
        finish();
    }

    @Override
    public void edit() {
        TaskIntents
                .getEditTaskStack(this, null, taskId)
                .startActivities();
        notificationManager.cancel(taskId);
        finish();
    }

    @Override
    public void snooze() {
        finish();
        startActivity(new Intent(this, SnoozeActivity.class) {{
            setFlags(FLAG_ACTIVITY_NEW_TASK);
            putExtra(SnoozeActivity.EXTRA_TASK_ID, taskId);
        }});
    }

    @Override
    public void complete() {
        broadcaster.completeTask(taskId);
        finish();
    }
}
