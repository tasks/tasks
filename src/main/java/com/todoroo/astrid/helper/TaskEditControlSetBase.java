/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.helper;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.todoroo.astrid.data.Task;

import org.tasks.R;

import static org.tasks.preferences.ResourceResolver.getData;

// --- interface

/**
 * Interface for working with controls that alter task data
 */
public abstract class TaskEditControlSetBase implements TaskEditControlSet {

    protected final Activity activity;
    private final int viewLayout;
    private boolean useTemplate;
    private View view;
    protected Task model;
    protected boolean initialized = false;
    protected final int themeColor;
    protected final int unsetColor;

    public TaskEditControlSetBase(Activity activity, int viewLayout) {
        this(activity, viewLayout, true);
    }

    public TaskEditControlSetBase(Activity activity, int viewLayout, boolean useTemplate) {
        this.activity = activity;
        this.viewLayout = viewLayout;
        this.useTemplate = useTemplate;
        if (viewLayout == -1) {
            initialized = true;
        }

        themeColor = getData(activity, R.attr.asTextColor);
        unsetColor = getData(activity, R.attr.asTextColorHint);
    }

    protected View inflateWithTemplate(int layout) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View template = layoutInflater.inflate(R.layout.control_set_template, null);
        LinearLayout content = (LinearLayout) template.findViewById(R.id.content);
        content.addView(layoutInflater.inflate(layout, null));
        return template;
    }

    @Override
    public View getView() {
        if (view == null && !initialized) {
            if (viewLayout != -1) {
                view = useTemplate ? inflateWithTemplate(viewLayout) : LayoutInflater.from(activity).inflate(viewLayout, null);
                afterInflate();
            }
            if (model != null) {
                readFromTaskOnInitialize();
            }
            this.initialized = true;
        }
        return view;
    }

    /**
     * Read data from model to update the control set
     */
    @Override
    public void readFromTask(Task task) {
        this.model = task;
        if (initialized) {
            readFromTaskOnInitialize();
        }
    }


    /**
     * Called once to setup the ui with data from the task
     */
    protected abstract void readFromTaskOnInitialize();

    /**
     * Write data from control set to model
     */
    @Override
    public void writeToModel(Task task) {
        if (initialized) {
            writeToModelAfterInitialized(task);
        }
    }

    /**
     * Write to model, if initialization logic has been called
     */
    protected abstract void writeToModelAfterInitialized(Task task);

    /**
     * Called when views need to be inflated
     */
    protected abstract void afterInflate();
}
