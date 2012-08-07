/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.task;

import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.alerts.AlertController;



/** Fields that you would want to read or edit in the onTaskSave and onTaskComplete
 * event handlers */
public class TaskModelForHandlers extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
        LegacyAbstractController.KEY_ROWID,
        REPEAT,
        DEFINITE_DUE_DATE,
        PREFERRED_DUE_DATE,
        HIDDEN_UNTIL,
        PROGRESS_PERCENTAGE,
        ESTIMATED_SECONDS,
        LAST_NOTIFIED,
        NOTIFICATIONS,
        NOTIFICATION_FLAGS,
        FLAGS,
    };

    /**
     * This method updates the task to reflect a new repeat iteration. It moves
     * back the due dates and updates other task properties accordingly.
     *
     * @param context
     * @param taskController
     * @param repeatInfo
     */
    @SuppressWarnings("deprecation")
    public void repeatTaskBy(Context context, TaskController taskController,
            RepeatInfo repeatInfo) {

        // move dates back
        if(getDefiniteDueDate() != null)
            setDefiniteDueDate(repeatInfo.shiftDate(getDefiniteDueDate()));
        if(getHiddenUntil() != null)
            setHiddenUntil(repeatInfo.shiftDate(getHiddenUntil()));
        if(getPreferredDueDate() != null)
            setPreferredDueDate(repeatInfo.shiftDate(getPreferredDueDate()));
        setProgressPercentage(0);

        // set elapsed time to 0... yes, we're losing data
        setElapsedSeconds(0);

        // if no deadlines set, create one (so users don't get confused)
        if(getDefiniteDueDate() == null && getPreferredDueDate() == null)
            setPreferredDueDate(repeatInfo.shiftDate(new Date()));

        // shift fixed alerts
        AlertController alertController = new AlertController(context);
        alertController.open();
        List<Date> alerts = alertController.getTaskAlerts(getTaskIdentifier());
        alertController.removeAlerts(getTaskIdentifier());
        for(int i = 0; i < alerts.size(); i++) {
            Date newAlert = repeatInfo.shiftDate(alerts.get(i));
            alertController.addAlert(getTaskIdentifier(), newAlert);
            alerts.set(i, newAlert);
        }

        // reset periodic alerts
        setLastNotificationTime(null);

//        ReminderService.updateAlarm(context, taskController, alertController, this);
        alertController.close();
    }

    // --- constructors

    public TaskModelForHandlers(Cursor cursor, ContentValues setValues) {
        super(cursor);
        this.setValues = setValues;
    }

    // --- getters and setters

    @Override
    public RepeatInfo getRepeat() {
        return super.getRepeat();
    }

    @Override
    public Integer getNotificationIntervalSeconds() {
        return super.getNotificationIntervalSeconds();
    }

    @Override
    public boolean isTaskCompleted() {
        return super.isTaskCompleted();
    }

    @Override
    public Date getDefiniteDueDate() {
        return super.getDefiniteDueDate();
    }

    @Override
    public Integer getEstimatedSeconds() {
        return super.getEstimatedSeconds();
    }

    @Override
    public Date getHiddenUntil() {
        return super.getHiddenUntil();
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

    @Override
    public int getFlags() {
        return super.getFlags();
    }

    @Override
    public void setDefiniteDueDate(Date definiteDueDate) {
        super.setDefiniteDueDate(definiteDueDate);
    }

    @Override
    public void setPreferredDueDate(Date preferredDueDate) {
        super.setPreferredDueDate(preferredDueDate);
    }

    @Override
    public void setHiddenUntil(Date hiddenUntil) {
        super.setHiddenUntil(hiddenUntil);
    }

}
