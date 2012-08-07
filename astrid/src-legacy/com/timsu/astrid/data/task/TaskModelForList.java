/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.task;

import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.enums.Importance;



/** Fields that you would want to edit in the TaskModel */
public class TaskModelForList extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
        LegacyAbstractController.KEY_ROWID,
        NAME,
        IMPORTANCE,
        ELAPSED_SECONDS,
        ESTIMATED_SECONDS,
        TIMER_START,
        DEFINITE_DUE_DATE,
        PREFERRED_DUE_DATE,
        NOTIFICATIONS,
        PROGRESS_PERCENTAGE,
        COMPLETION_DATE,
        CREATION_DATE,
        HIDDEN_UNTIL,
        NOTES,
        REPEAT,
        FLAGS,
    };

    // pre-load the cache for our column keys
    static {
        HashMap<String, Integer> indexCache = new HashMap<String, Integer>();
        columnIndexCache.put(TaskModelForList.class, indexCache);
        for(int i = 0; i < FIELD_LIST.length; i++)
            indexCache.put(FIELD_LIST[i], i);
    }

    /** Get the weighted score for this task. Smaller is more important */
    public int getTaskWeight() {
        int weight = 0;

        // bubble tasks with timers to the top
        if(getTimerStart() != null)
            weight -= 10000;

        // importance
        weight += getImportance().ordinal() * 80;

        // looming absolute deadline
        if(getDefiniteDueDate() != null) {
            int hoursLeft = (int) ((getDefiniteDueDate().getTime() -
                    System.currentTimeMillis())/1000/3600);
            if(hoursLeft < 5*24)
                weight += (hoursLeft - 5*24);
            weight -= 20;
        }

        // looming preferred deadline
        if(getPreferredDueDate() != null) {
            int hoursLeft = (int) ((getPreferredDueDate().getTime() -
                    System.currentTimeMillis())/1000/3600);
            if(hoursLeft < 5*24)
                weight += (hoursLeft - 5*24)/2;
            weight -= 10;
        }

        // bubble completed tasks to the bottom
        if(isTaskCompleted()) {
            if(getCompletionDate() == null)
                weight += 1e6;
            else
                weight = (int)Math.max(10000 + (System.currentTimeMillis() -
                    getCompletionDate().getTime()) / 1000, 10000);
            return weight;
        }

        return weight;
    }

    @Override
    public boolean isHidden() {
        return super.isHidden();
    }

    /** map of cached display labels */
    private final HashMap<Integer, String> displayLabels = new HashMap<Integer, String>();

    public String getCachedLabel(int key) {
        return displayLabels.get(key);
    }
    public void putCachedLabel(int key, String value) {
        displayLabels.put(key, value);
    }
    public void clearCache() {
        displayLabels.clear();
    }

    // --- constructors

    public TaskModelForList(Cursor cursor) {
        super(cursor);

        prefetchData(FIELD_LIST);
    }

    // --- exposed getters and setters

    @Override
    public boolean isTaskCompleted() {
        return super.isTaskCompleted();
    }

    @Override
    public int getTaskColorResource(Context context) {
        return super.getTaskColorResource(context);
    }

    @Override
    public Integer getElapsedSeconds() {
        return super.getElapsedSeconds();
    }

    public static int getCompletedPercentage() {
        return COMPLETE_PERCENTAGE;
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
    public Importance getImportance() {
        return super.getImportance();
    }

    @Override
    public String getName() {
        return super.getName();
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
    public Date getTimerStart() {
        return super.getTimerStart();
    }

    @Override
    public Date getCompletionDate() {
        return super.getCompletionDate();
    }

    @Override
    public String getNotes() {
        return super.getNotes();
    }

    @Override
	public Integer getNotificationIntervalSeconds() {
		return super.getNotificationIntervalSeconds();
	}

	@Override
    public RepeatInfo getRepeat() {
        return super.getRepeat();
    }

	@Override
	public Date getCreationDate() {
	    return super.getCreationDate();
	}

	@Override
	public int getFlags() {
		return super.getFlags();
	}

    // --- setters

    @Override
    public void setProgressPercentage(int progressPercentage) {
        super.setProgressPercentage(progressPercentage);
    }

    @Override
    public void setTimerStart(Date timerStart) {
        super.setTimerStart(timerStart);
    }

    @Override
    public void stopTimerAndUpdateElapsedTime() {
        super.stopTimerAndUpdateElapsedTime();
    }

    public static String getNameField() {
        return NAME;
    }

    @Override
    public void setPreferredDueDate(Date preferredDueDate) {
        super.setPreferredDueDate(preferredDueDate);
    }

    @Override
    public void setDefiniteDueDate(Date definiteDueDate) {
        super.setDefiniteDueDate(definiteDueDate);
    }

    @Override
    public void setImportance(Importance importance) {
    	super.setImportance(importance);
    }

    @Override
    public void setHiddenUntil(Date hiddenUntil) {
        super.setHiddenUntil(hiddenUntil);
    }

    @Override
    public void setPostponeCount(int postponeCount) {
        super.setPostponeCount(postponeCount);
    }
}
