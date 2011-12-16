package com.todoroo.astrid.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
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
        dialog.getWindow()
              .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    @Override
    protected void refreshDisplayView() {
        notesPreview.setText("");
        notesPreview.setText(editText.getText());
        linkifyDisplayView();
    }

    private void linkifyDisplayView() {
        if(!TextUtils.isEmpty(notesPreview.getText())) {
            notesPreview.setLinkTextColor(Color.rgb(100, 160, 255));
            Linkify.addLinks(notesPreview, Linkify.ALL);
        }
    }

    @Override
    public void readFromTask(Task task) {
        editText.setTextKeepState(task.getValue(Task.NOTES));
        notesPreview.setText(task.getValue(Task.NOTES));
        linkifyDisplayView();
    }

    @Override
    public String writeToModel(Task task) {
        task.setValue(Task.NOTES, editText.getText().toString());
        return null;
    }

    @Override
    protected void onOkClick() {
        super.onOkClick();
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    @Override
    protected void onCancelClick() {
        super.onCancelClick();
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public boolean hasNotes() {
        return !TextUtils.isEmpty(editText.getText());
    }

}
