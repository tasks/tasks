package com.timsu.astrid.data.task;

import java.util.Date;

import android.database.Cursor;

import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.enums.Importance;



/** Fields that you would want to see in the TaskView activity */
public class TaskModelForView extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
        AbstractController.KEY_ROWID,
        NAME,
        IMPORTANCE,
        PROGRESS_PERCENTAGE,
        ESTIMATED_SECONDS,
        ELAPSED_SECONDS,
        TIMER_START,
        DEFINITE_DUE_DATE,
        PREFERRED_DUE_DATE,
        NOTES,
    };

    // --- constructors

    public TaskModelForView(TaskIdentifier identifier, Cursor cursor) {
        super(identifier, cursor);
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
    public int getProgressPercentage() {
        return super.getProgressPercentage();
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
    public Date getTimerStart() {
        return super.getTimerStart();
    }

    @Override
    public void setTimerStart(Date timerStart) {
        super.setTimerStart(timerStart);
    }

    @Override
    public void setProgressPercentage(int progressPercentage) {
        super.setProgressPercentage(progressPercentage);
    }

    @Override
    public void stopTimerAndUpdateElapsedTime() {
        super.stopTimerAndUpdateElapsedTime();
    }
}
