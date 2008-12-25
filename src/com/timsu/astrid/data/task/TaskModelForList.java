package com.timsu.astrid.data.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
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
        HIDDEN_UNTIL,
    };

    static List<TaskModelForList> createTaskModelList(Cursor cursor,
            boolean hideHidden) {
        ArrayList<TaskModelForList> list = new ArrayList<TaskModelForList>();
        final HashMap<TaskModelForList, Integer> weights = new
            HashMap<TaskModelForList, Integer>();

        // first, load everything
        for(int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToNext();
            TaskModelForList task = new TaskModelForList(cursor);

            // hide tasks
            if(hideHidden) {
                if(task.getHiddenUntil() != null &&
                    task.getHiddenUntil().getTime() > System.currentTimeMillis())
                continue;
            }

            list.add(task);
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

        // bubble completed tasks to the bottom
        if(isTaskCompleted())
            weight += 1000;

        return weight;
    }

    // --- constructors

    public TaskModelForList(Cursor cursor) {
        super(cursor);

        // prefetch every field - we can't lazy load with more than 1
        getElapsedSeconds();
        getDefiniteDueDate();
        getEstimatedSeconds();
        getHiddenUntil();
        getImportance();
        getName();
        getPreferredDueDate();
        getProgressPercentage();
        getTimerStart();
    }

    // --- getters and setters

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
}
