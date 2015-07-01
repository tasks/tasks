package org.tasks.reminders;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import org.tasks.injection.InjectingAppCompatActivity;

public class NotificationActivity extends InjectingAppCompatActivity implements DialogInterface.OnDismissListener {

    private static final String FRAG_TAG_NOTIFICATION_FRAGMENT = "frag_tag_notification_fragment";

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_TASK_ID = "extra_task_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager supportFragmentManager = getSupportFragmentManager();
        NotificationDialog fragment = (NotificationDialog) supportFragmentManager.findFragmentByTag(FRAG_TAG_NOTIFICATION_FRAGMENT);
        if (fragment == null) {
            Intent intent = getIntent();
            fragment = new NotificationDialog();
            fragment.setTitle(intent.getStringExtra(EXTRA_TITLE));
            fragment.setTaskId(intent.getLongExtra(EXTRA_TASK_ID, 0L));
            fragment.show(supportFragmentManager, FRAG_TAG_NOTIFICATION_FRAGMENT);
        }
        fragment.setOnDismissListener(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
