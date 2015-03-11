/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.service.TaskService;

import org.tasks.Broadcaster;
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

    public static final String TOKEN_ID = "id"; //$NON-NLS-1$

    @Inject Broadcaster broadcaster;
    @Inject TaskService taskService;
    @Inject Preferences preferences;

    @Override
    protected void initializeData() {
        displayNotificationPopup();
        super.initializeData();
    }

    private void displayNotificationPopup() {
        String title = extras.getString(Notifications.EXTRAS_TEXT);
        long taskId = extras.getLong(TOKEN_ID);
        new ReminderDialog(preferences, broadcaster, taskService, (AstridActivity) getActivity(), taskId, title).show();
    }
}
