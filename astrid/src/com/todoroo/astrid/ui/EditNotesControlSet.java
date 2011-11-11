package com.todoroo.astrid.ui;

import android.app.Activity;
import android.widget.EditText;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;

public class EditNotesControlSet extends PopupControlSet {

    private final EditText editText;
    private final TextView notesPreview;

    public EditNotesControlSet(Activity activity, int viewLayout, int displayViewLayout) {
        super(activity, viewLayout, displayViewLayout, R.string.TEA_note_label);
        editText = (EditText) getView().findViewById(R.id.notes);
        notesPreview = (TextView) getDisplayView().findViewById(R.id.notes_display);
    }

    @Override
    protected void refreshDisplayView() {
        notesPreview.setText(editText.getText());
    }

    @Override
    public void readFromTask(Task task) {
        editText.setTextKeepState(task.getValue(Task.NOTES));
        notesPreview.setText(task.getValue(Task.NOTES));
    }

    @Override
    public String writeToModel(Task task) {
        task.setValue(Task.NOTES, editText.getText().toString());
        return null;
    }

}
