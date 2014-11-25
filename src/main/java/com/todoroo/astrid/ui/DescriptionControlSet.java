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

import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import static org.tasks.preferences.ResourceResolver.getResource;

public class DescriptionControlSet extends PopupControlSet {

    protected EditText editText;
    protected TextView notesPreview;
    protected ImageView image;

    public DescriptionControlSet(ActivityPreferences preferences, Activity activity) {
        super(preferences, activity, R.layout.control_set_description, R.layout.control_set_notes_display, R.string.TEA_note_label);
        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
    }

    @Override
    protected void refreshDisplayView() {
        String textToUse;
        if (initialized) {
            textToUse = editText.getText().toString();
        } else {
            textToUse = model.getNotes();
        }

        if (TextUtils.isEmpty(textToUse)) {
            notesPreview.setText(R.string.TEA_notes_empty);
            notesPreview.setTextColor(unsetColor);
            image.setImageResource(R.drawable.tea_icn_edit_gray);
        } else {
            notesPreview.setText(textToUse);
            notesPreview.setTextColor(themeColor);
            image.setImageResource(getResource(activity, R.attr.tea_icn_edit));
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
        editText.setTextKeepState(model.getNotes());
        notesPreview.setText(model.getNotes());
        linkifyDisplayView();
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        task.setNotes(editText.getText().toString());
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
}
