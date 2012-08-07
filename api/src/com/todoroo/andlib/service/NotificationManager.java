/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;

import android.app.Notification;
import android.content.Context;

/**
 * Notification Manager stub
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public interface NotificationManager {

    public void cancel(int id);

    public void cancelAll();

    public void notify(int id, Notification notification);

    /**
     * Instantiation of notification manager that passes through to
     * Android's notification manager
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class AndroidNotificationManager implements NotificationManager {
        private final android.app.NotificationManager nm;
        public AndroidNotificationManager(Context context) {
            nm = (android.app.NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        public void cancel(int id) {
            nm.cancel(id);
        }

        public void cancelAll() {
            nm.cancelAll();
        }

        public void notify(int id, Notification notification) {
            nm.notify(id, notification);
        }
    }
}
