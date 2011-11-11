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

    private final View view;

    public TaskEditControlSet(Activity activity, int viewLayout) {
        if (viewLayout != -1)
            this.view = LayoutInflater.from(activity).inflate(viewLayout, null);
        else
            this.view = null;
    }

    public View getView() {
        return view;
    }

    /**
     * Read data from model to update the control set
     */
    public abstract void readFromTask(Task task);

    /**
     * Write data from control set to model
     * @return text appended to the toast
     */
    public abstract String writeToModel(Task task);

}