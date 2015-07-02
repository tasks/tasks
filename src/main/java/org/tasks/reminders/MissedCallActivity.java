package org.tasks.reminders;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.intents.TaskIntents;
import org.tasks.notifications.NotificationManager;

import javax.inject.Inject;

public class MissedCallActivity extends InjectingAppCompatActivity implements DialogInterface.OnDismissListener, MissedCallDialog.MissedCallHandler {

    private static final String FRAG_TAG_MISSED_CALL_FRAGMENT = "frag_tag_missed_call_fragment";

    public static final String EXTRA_NUMBER = "number"; //$NON-NLS-1$
    public static final String EXTRA_NAME = "name"; //$NON-NLS-1$
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_CALL_NOW = "extra_call_now";
    public static final String EXTRA_CALL_LATER = "extra_call_later";

    @Inject TaskService taskService;
    @Inject NotificationManager notificationManager;

    private String name;
    private String number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        name = intent.getStringExtra(EXTRA_NAME);
        number = intent.getStringExtra(EXTRA_NUMBER);

        if (intent.getBooleanExtra(EXTRA_CALL_NOW, false)) {
            callNow();
        } else if (intent.getBooleanExtra(EXTRA_CALL_LATER, false)) {
            callLater();
        } else {
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            MissedCallDialog fragment = (MissedCallDialog) supportFragmentManager.findFragmentByTag(FRAG_TAG_MISSED_CALL_FRAGMENT);
            if (fragment == null) {
                fragment = new MissedCallDialog();
                fragment.setTitle(intent.getStringExtra(EXTRA_TITLE));
                fragment.show(supportFragmentManager, FRAG_TAG_MISSED_CALL_FRAGMENT);
            }
            fragment.setOnDismissListener(this);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override
    public void callNow() {
        Intent call = new Intent(Intent.ACTION_VIEW);
        call.setData(Uri.parse("tel:" + number)); //$NON-NLS-1$
        startActivity(call);
        cancelNotificationAndFinish();
    }

    @Override
    public void callLater() {
        Task task = new Task() {{
            setTitle(TextUtils.isEmpty(name)
                    ? getString(R.string.MCA_task_title_no_name, number)
                    : getString(R.string.MCA_task_title_name, name, number));
        }};
        taskService.save(task);
        TaskIntents
                .getEditTaskStack(this, null, task.getId())
                .startActivities();
        cancelNotificationAndFinish();
    }

    @Override
    public void ignore() {
        cancelNotificationAndFinish();
    }

    private void cancelNotificationAndFinish() {
        notificationManager.cancel(number.hashCode());
        finish();
    }
}
