package com.todoroo.astrid.reminders;

import android.app.Notification;
import android.content.Context;

import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;

/**
 * Notification manager that provides notifications and adds an
 * extra method for verification.
 *
 * @author timsu
 *
 */
public class AssertiveNotificationManager extends AndroidNotificationManager {

    Notification notification = null;

    public AssertiveNotificationManager(Context context) {
        super(context);

    }

    @Override
    public void notify(int id, Notification notification) {
        super.notify(id, notification);
        this.notification = notification;
    }

    public void assertNotified() {
        if(notification == null)
            throw new AssertionError("Notification was not triggered");
    }

    public void assertNotNotified() {
        if(notification != null)
            throw new AssertionError("Notification was triggered");
    }

    public Notification getNotification() {
        return notification;
    }

    public void clear() {
        notification = null;
    }
}