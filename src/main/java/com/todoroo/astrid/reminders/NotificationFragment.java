/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.app.Dialog;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskListFragment;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.reminders.SnoozeActivity;

import javax.inject.Inject;

/**
 * This activity is launched when a user opens up a notification from the
 * tray. It launches the appropriate activity based on the passed in parameters.
 *
 * @author timsu
 *
 */
public class NotificationFragment extends TaskListFragment {

    public static final String TOKEN_ID = "id"; //$NON-NLS-1$

    @Inject Broadcaster broadcaster;

    @Override
    protected void initializeData() {
        displayNotificationPopup();
        super.initializeData();
    }

    private void displayNotificationPopup() {
        final String title = extras.getString(Notifications.EXTRAS_TITLE);
        final long taskId = extras.getLong(TOKEN_ID);
        final AstridActivity activity = (AstridActivity) getActivity();

        new Dialog(activity, R.style.ReminderDialog) {{
            setContentView(R.layout.astrid_reminder_view_portrait);
            findViewById(R.id.speech_bubble_container).setVisibility(View.GONE);

            // set up listeners
            findViewById(R.id.dismiss).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    dismiss();
                }
            });

            findViewById(R.id.reminder_snooze).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    dismiss();
                    activity.startActivity(new Intent(activity, SnoozeActivity.class) {{
                        putExtra(SnoozeActivity.TASK_ID, taskId);
                    }});
                }
            });

            findViewById(R.id.reminder_complete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    broadcaster.completeTask(taskId);
                    dismiss();
                }
            });

            findViewById(R.id.reminder_edit).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    activity.onTaskListItemClicked(taskId);
                }
            });

            ((TextView) findViewById(R.id.reminder_title)).setText(activity.getString(R.string.rmd_NoA_dlg_title) + " " + title);

            setOwnerActivity(activity);
        }}.show();
    }
}
