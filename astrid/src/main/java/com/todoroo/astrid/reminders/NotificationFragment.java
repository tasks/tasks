/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.view.View;

import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

/**
 * This activity is launched when a user opens up a notification from the
 * tray. It launches the appropriate activity based on the passed in parameters.
 *
 * @author timsu
 *
 */
public class NotificationFragment extends TaskListFragment {

    // --- constants

    /** task id from notification */
    public static final String TOKEN_ID = "id"; //$NON-NLS-1$

    // --- implementation

    @Inject TaskService taskService;
    @Inject Preferences preferences;

    @Override
    protected void initializeData() {
        displayNotificationPopup();
        super.initializeData();
    }

    /**
     * Set up the UI for this activity
     */
    private void displayNotificationPopup() {
        // hide quick add
        getView().findViewById(R.id.taskListFooter).setVisibility(View.GONE);

        String title = extras.getString(Notifications.EXTRAS_TEXT);
        long taskId = extras.getLong(TOKEN_ID);
        new ReminderDialog(preferences, taskService, (AstridActivity) getActivity(), taskId, title).show();
    }
}
