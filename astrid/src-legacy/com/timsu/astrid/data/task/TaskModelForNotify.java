/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.task;

import java.util.Date;

import android.database.Cursor;

import com.timsu.astrid.data.LegacyAbstractController;



/** Fields that you would want to see in the TaskView activity */
public class TaskModelForNotify extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
        LegacyAbstractController.KEY_ROWID,
        ESTIMATED_SECONDS,
        NOTIFICATIONS,
        NOTIFICATION_FLAGS,
        LAST_NOTIFIED,
        HIDDEN_UNTIL,
        PROGRESS_PERCENTAGE,
        DEFINITE_DUE_DATE,
        PREFERRED_DUE_DATE,
    };

    // --- constructors

    public TaskModelForNotify(Cursor cursor) {
        super(cursor);

        prefetchData(FIELD_LIST);
    }

    // --- getters

    @Override
    public Integer getEstimatedSeconds() {
        return super.getEstimatedSeconds();
    }

    @Override
    public boolean isTaskCompleted() {
        return super.isTaskCompleted();
    }

    @Override
    public Integer getNotificationIntervalSeconds() {
        return super.getNotificationIntervalSeconds();
    }

    @Override
    public Date getHiddenUntil() {
        return super.getHiddenUntil();
    }

    @Override
    public Date getDefiniteDueDate() {
        return super.getDefiniteDueDate();
    }

    @Override
    public Date getPreferredDueDate() {
        return super.getPreferredDueDate();
    }

    @Override
    public int getNotificationFlags() {
        return super.getNotificationFlags();
    }

    @Override
    public Date getLastNotificationDate() {
        return super.getLastNotificationDate();
    }

    // --- setters

    @Override
    public void setLastNotificationTime(Date date) {
        super.setLastNotificationTime(date);
    }
}
