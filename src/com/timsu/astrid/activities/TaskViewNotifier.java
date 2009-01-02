package com.timsu.astrid.activities;

import java.util.Random;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.timsu.astrid.utilities.Notifications;

public class TaskViewNotifier extends TaskView {

    // bundle tokens
    public static final String FROM_NOTIFICATION_TOKEN = "notify";
    public static final String NOTIF_FLAGS_TOKEN       = "notif_flags";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey(FROM_NOTIFICATION_TOKEN))
            showNotificationAlert();
    }

    /** Called when user clicks on a notification to get here */
    private void showNotificationAlert() {
        Resources r = getResources();

        // clear notifications
        Notifications.clearAllNotifications(this, model.getTaskIdentifier());

        String[] responses = r.getStringArray(R.array.reminder_responses);
        String response = responses[new Random().nextInt(responses.length)];
        new AlertDialog.Builder(this)
        .setTitle(R.string.taskView_notifyTitle)
        .setMessage(response)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(R.string.notify_yes, null)
        .setNegativeButton(R.string.notify_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setResult(RESULT_CANCELED);
                TaskList.shouldCloseInstance = true;
                finish();
            }
        })
        .show();
    }
}
