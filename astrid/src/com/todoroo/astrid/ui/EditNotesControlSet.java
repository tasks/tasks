/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.ThemeService;

public class EditNotesControlSet extends PopupControlSet {

    protected EditText editText;
    protected TextView notesPreview;
    protected ImageView image;

    public EditNotesControlSet(Activity activity, int viewLayout, int displayViewLayout) {
        super(activity, viewLayout, displayViewLayout, R.string.TEA_note_label);
        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
    }

    @Override
    protected void refreshDisplayView() {
        String textToUse;
        if (initialized)
            textToUse = editText.getText().toString();
        else
            textToUse = model.getValue(Task.NOTES);

        if (TextUtils.isEmpty(textToUse)) {
            notesPreview.setText(R.string.TEA_notes_empty);
            notesPreview.setTextColor(unsetColor);
            image.setImageResource(R.drawable.tea_icn_edit_gray);
        } else {
            notesPreview.setText(textToUse);
            notesPreview.setTextColor(themeColor);
            image.setImageResource(ThemeService.getTaskEditDrawable(R.drawable.tea_icn_edit, R.drawable.tea_icn_edit_lightblue));
        }

        linkifyDisplayView();
    }

    private void linkifyDisplayView() {
        if(!TextUtils.isEmpty(notesPreview.getText())) {
            notesPreview.setLinkTextColor(Color.rgb(100, 160, 255));
            Linkify.addLinks(notesPreview, Linkify.ALL);
        }
    }

    @Override
    protected void afterInflate() {
        editText = (EditText) getView().findViewById(R.id.notes);
        notesPreview = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
    }

    @Override
    protected void additionalDialogSetup() {
        super.additionalDialogSetup();
        dialog.getWindow()
            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    protected void readFromTaskOnInitialize() {
        editText.setTextKeepState(model.getValue(Task.NOTES));
        notesPreview.setText(model.getValue(Task.NOTES));
        linkifyDisplayView();
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        task.setValue(Task.NOTES, editText.getText().toString());
        return null;
    }

    @Override
    protected boolean onOkClick() {
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        return super.onOkClick();
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
