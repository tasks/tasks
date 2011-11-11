package com.todoroo.astrid.ui;

import android.app.Activity;
import android.widget.EditText;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;

/**
 * Control set for mapping a Property to an EditText
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditTextControlSet extends TaskEditControlSet {
    private final EditText editText;
    private final StringProperty property;

    public EditTextControlSet(Activity activity, int layout, StringProperty property, int editText) {
        super(activity, layout);
        this.property = property;
        this.editText = (EditText) getView().findViewById(editText);
    }

    @Override
    public void readFromTask(Task task) {
        editText.setTextKeepState(task.getValue(property));
    }

    @Override
    public String writeToModel(Task task) {
        task.setValue(property, editText.getText().toString());
        return null;
    }
}