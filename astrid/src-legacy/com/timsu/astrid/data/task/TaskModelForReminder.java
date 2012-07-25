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
public class TaskModelForReminder extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
        LegacyAbstractController.KEY_ROWID,
        NAME,
        NOTIFICATION_FLAGS,
        HIDDEN_UNTIL,
        TIMER_START,
        PROGRESS_PERCENTAGE,
    };

    // --- constructors

    public TaskModelForReminder(Cursor cursor) {
        super(cursor);

        prefetchData(FIELD_LIST);
    }

    // --- getters

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public Date getTimerStart() {
        return super.getTimerStart();
    }

    @Override
    public boolean isTaskCompleted() {
        return super.isTaskCompleted();
    }

    @Override
    public Date getHiddenUntil() {
        return super.getHiddenUntil();
    }

    @Override
    public int getNotificationFlags() {
        return super.getNotificationFlags();
    }

    // --- setters

    @Override
    public void setLastNotificationTime(Date date) {
        super.setLastNotificationTime(date);
    }
}
