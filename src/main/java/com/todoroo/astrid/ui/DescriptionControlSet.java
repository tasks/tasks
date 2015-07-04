/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.support.v4.app.FragmentActivity;
import android.widget.EditText;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.tasks.R;

public class DescriptionControlSet extends TaskEditControlSetBase {

    protected EditText editText;

    public DescriptionControlSet(FragmentActivity activity) {
        super(activity, R.layout.control_set_description);
    }

    @Override
    protected void afterInflate() {
        editText = (EditText) getView().findViewById(R.id.notes);
    }

    @Override
    protected void readFromTaskOnInitialize() {
        editText.setTextKeepState(model.getNotes());
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        task.setNotes(editText.getText().toString().trim());
    }

    @Override
    public int getIcon() {
        return R.attr.ic_action_list;
    }
}
