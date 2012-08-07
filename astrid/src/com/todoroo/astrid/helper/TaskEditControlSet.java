/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
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
        if (viewLayout == -1)
            initialized = true;
    }

    public View getView() {
        if (view == null && !initialized) {
            if (viewLayout != -1) {
                view = LayoutInflater.from(activity).inflate(viewLayout, null);
                afterInflate();
            }
            if (model != null)
                readFromTaskOnInitialize();
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
            readFromTaskOnInitialize();
    }


    /**
     * Called once to setup the ui with data from the task
     */
    protected abstract void readFromTaskOnInitialize();

    /**
     * Write data from control set to model
     * @return text appended to the toast
     */
    public String writeToModel(Task task) {
        if (initialized) {
            return writeToModelAfterInitialized(task);
        }
        return null;
    }

    /**
     * Write to model, if initialization logic has been called
     * @return toast text
     */
    protected abstract String writeToModelAfterInitialized(Task task);

    /**
     * Called when views need to be inflated
     */
    protected abstract void afterInflate();
}
