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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.database.Cursor;

import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.enums.Importance;



/** Fields that you would want to edit in the TaskModel */
public class TaskModelForList extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
        AbstractController.KEY_ROWID,
        NAME,
        IMPORTANCE,
        ELAPSED_SECONDS,
        ESTIMATED_SECONDS,
        TIMER_START,
        DEFINITE_DUE_DATE,
        PREFERRED_DUE_DATE,
        PROGRESS_PERCENTAGE,
        COMPLETION_DATE,
        HIDDEN_UNTIL,
    };

    /** Takes the incoming list of task models and weights it, removing hidden
     * tasks if desired. This mutates the list */
    static List<TaskModelForList> sortTaskList(List<TaskModelForList> list) {
        final HashMap<TaskModelForList, Integer> weights = new
            HashMap<TaskModelForList, Integer>();

        // first, load everything
        for(Iterator<TaskModelForList> i = list.iterator(); i.hasNext(); ) {
            TaskModelForList task = i.next();

            weights.put(task, task.getWeight());
        }

        // now sort
        Collections.sort(list, new Comparator<TaskModelForList>() {
            @Override
            public int compare(TaskModelForList a, TaskModelForList b) {
                return weights.get(a) - weights.get(b);
            }
        });

        return list;
    }

    /** Get the weighted score for this task. Smaller is more important */
    private int getWeight() {
        int weight = 0;

        // bubble completed tasks to the bottom
        if(isTaskCompleted()) {
            weight += Math.max(5e6 - (System.currentTimeMillis() -
                    getCompletionDate().getTime()) / 1000, 5000);
            return weight;
        }

        // importance
        weight += getImportance().ordinal() * 60;

        // estimated time left
        int secondsLeft = getEstimatedSeconds() - getElapsedSeconds();
        if(secondsLeft > 0)
            weight += secondsLeft / 120;

        // looming absolute deadline
        if(getDefiniteDueDate() != null) {
            int hoursLeft = (int) (getDefiniteDueDate().getTime() -
                    System.currentTimeMillis())/1000/3600;
            if(hoursLeft < 5*24)
                weight += (hoursLeft - 5*24);
        }

        // looming preferred deadline
        if(getPreferredDueDate() != null) {
            int hoursLeft = (int) (getPreferredDueDate().getTime() -
                    System.currentTimeMillis())/1000/3600;
            if(hoursLeft < 5*24)
                weight += (hoursLeft - 5*24)/2;
        }

        return weight;
    }

    @Override
    public boolean isHidden() {
        return super.isHidden();
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
    public int getTaskColorResource() {
        return super.getTaskColorResource();
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
}
