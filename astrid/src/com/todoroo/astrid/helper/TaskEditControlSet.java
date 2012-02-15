package com.todoroo.astrid.helper;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import com.todoroo.astrid.data.Task;

// --- interface

/**
 * Interface for working with controls that alter task data
 */
public abstract class TaskEditControlSet {

    protected final Activity activity;
    private final int viewLayout;
    private View view;
    protected Task model;
    protected boolean initialized = false;

    public TaskEditControlSet(Activity activity, int viewLayout) {
        this.activity = activity;
        this.viewLayout = viewLayout;
    }

    public View getView() {
        if (view == null && !initialized) {
            if (viewLayout != -1) {
                view = LayoutInflater.from(activity).inflate(viewLayout, null);
                afterInflate();
            }
            if (model != null)
                readFromTaskPrivate();
            this.initialized = true;
        }
        return view;
    }

    public View getDisplayView() {
        return getView();
    }

    /**
     * Read data from model to update the control set
     */
    public void readFromTask(Task task) {
        this.model = task;
        if (initialized)
            readFromTaskPrivate();
    }


    /**
     * Called once to setup the ui with data from the task
     */
    protected abstract void readFromTaskPrivate();

    /**
     * Write data from control set to model
     * @return text appended to the toast
     */
    public String writeToModel(Task task) {
        if (initialized) {
            return writeToModelPrivate(task);
        }
        return null;
    }

    protected abstract String writeToModelPrivate(Task task);

    protected abstract void afterInflate();
}