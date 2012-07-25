/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.task;

import java.util.Date;

import android.database.Cursor;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.enums.Importance;



/** Fields that you would want to synchronize in the TaskModel */
public class TaskModelForSync extends AbstractTaskModel  {

    static String[] FIELD_LIST = new String[] {
        LegacyAbstractController.KEY_ROWID,
        NAME,
        IMPORTANCE,
        ESTIMATED_SECONDS,
        ELAPSED_SECONDS,
        DEFINITE_DUE_DATE,
        PREFERRED_DUE_DATE,
        HIDDEN_UNTIL,
        BLOCKING_ON,
        PROGRESS_PERCENTAGE,
        CREATION_DATE,
        COMPLETION_DATE,
        NOTES,
        REPEAT,
        LAST_NOTIFIED,
        NOTIFICATIONS,
        NOTIFICATION_FLAGS,
        FLAGS,
    };

    // --- constructors

    public TaskModelForSync() {
        super();
        setCreationDate(new Date());
    }

    public TaskModelForSync(Cursor cursor) {
        super(cursor);
        prefetchData(FIELD_LIST);
    }

    // --- getters and setters

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
    public int getProgressPercentage() {
        return super.getProgressPercentage();
    }

    @Override
    public Date getCreationDate() {
        return super.getCreationDate();
    }

    @Override
    public Date getCompletionDate() {
        return super.getCompletionDate();
    }

    @Override
    public Integer getElapsedSeconds() {
        return super.getElapsedSeconds();
    }

    @Override
    public Date getHiddenUntil() {
        return super.getHiddenUntil();
    }

    @Override
    public Importance getImportance() {
        return super.getImportance();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public String getNotes() {
        return super.getNotes();
    }

    @Override
    public Date getPreferredDueDate() {
        return super.getPreferredDueDate();
    }

    @Override
    public TaskIdentifier getBlockingOn() {
        return super.getBlockingOn();
    }

    @Override
    public RepeatInfo getRepeat() {
        return super.getRepeat();
    }

    @Override
    public Integer getNotificationIntervalSeconds() {
        return super.getNotificationIntervalSeconds();
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

    // --- setters

    @Override
    public void setDefiniteDueDate(Date definiteDueDate) {
        super.setDefiniteDueDate(definiteDueDate);
    }

    @Override
    public void setEstimatedSeconds(Integer estimatedSeconds) {
        super.setEstimatedSeconds(estimatedSeconds);
    }

    @Override
    public void setElapsedSeconds(int elapsedSeconds) {
        super.setElapsedSeconds(elapsedSeconds);
    }

    @Override
    public void setHiddenUntil(Date hiddenUntil) {
        super.setHiddenUntil(hiddenUntil);
    }

    @Override
    public void setImportance(Importance importance) {
        super.setImportance(importance);
    }

    @Override
    public void setName(String name) {
        super.setName(name);
    }

    @Override
    public void setNotes(String notes) {
        super.setNotes(notes);
    }

    @Override
    public void setPreferredDueDate(Date preferredDueDate) {
        super.setPreferredDueDate(preferredDueDate);
    }

    @Override
    public void setBlockingOn(TaskIdentifier blockingOn) {
        super.setBlockingOn(blockingOn);
    }

    @Override
    public void setRepeat(RepeatInfo taskRepeat) {
        super.setRepeat(taskRepeat);
    }

    @Override
    public void setCompletionDate(Date completionDate) {
        super.setCompletionDate(completionDate);
    }

    @Override
    public void setCreationDate(Date creationDate) {
        super.setCreationDate(creationDate);
    }

    @Override
    public void setProgressPercentage(int progressPercentage) {
        super.setProgressPercentage(progressPercentage);
    }

    @Override
    public void setNotificationIntervalSeconds(Integer intervalInSeconds) {
        super.setNotificationIntervalSeconds(intervalInSeconds);
    }

    @Override
    public void setFlags(int flags) {
        super.setFlags(flags);
    }
}

