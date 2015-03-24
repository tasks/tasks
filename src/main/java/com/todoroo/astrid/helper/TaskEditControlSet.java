package com.todoroo.astrid.helper;

import android.view.View;

import com.todoroo.astrid.data.Task;

public interface TaskEditControlSet {
    View getView();

    View getDisplayView();

    void readFromTask(Task task);

    void writeToModel(Task task);
}
