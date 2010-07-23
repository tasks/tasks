/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.data.task;

import java.util.Date;

import android.database.Cursor;

import com.timsu.astrid.data.enums.Importance;



/** Fields that you would want to edit in the TaskModel */
public class TaskModelForEdit extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
        NAME,
        IMPORTANCE,
        ESTIMATED_SECONDS,
        ELAPSED_SECONDS,
        DEFINITE_DUE_DATE,
        PREFERRED_DUE_DATE,
        HIDDEN_UNTIL,
        BLOCKING_ON,
        NOTIFICATIONS,
        NOTIFICATION_FLAGS,
        LAST_NOTIFIED,
        PROGRESS_PERCENTAGE,
        NOTES,
        REPEAT,
        CALENDAR_URI,
    };

    // --- constructors

    public TaskModelForEdit() {
        super();
        setCreationDate(new Date());
    }

    public TaskModelForEdit(TaskIdentifier identifier, Cursor cursor) {
        super(identifier, cursor);
    }

    // --- getters and setters

    @Override
    public boolean isTaskCompleted() {
        return super.isTaskCompleted();
    }

    @Override
    public Integer getNotificationIntervalSeconds() {
        return super.getNotificationIntervalSeconds();
    }

    @Override
    public void setNotificationIntervalSeconds(Integer intervalInSeconds) {
        super.setNotificationIntervalSeconds(intervalInSeconds);
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
    public int getProgressPercentage() {
        return super.getProgressPercentage();
    }

    @Override
    public TaskIdentifier getBlockingOn() {
        return super.getBlockingOn();
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
    public RepeatInfo getRepeat() {
        return super.getRepeat();
    }

    @Override
    public String getCalendarUri() {
        return super.getCalendarUri();
    }

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
    public void setNotificationFlags(int flags) {
        super.setNotificationFlags(flags);
    }

    @Override
    public void setRepeat(RepeatInfo taskRepeat) {
        super.setRepeat(taskRepeat);
    }

    @Override
    public void setCalendarUri(String uri) {
        super.setCalendarUri(uri);
    }
}
