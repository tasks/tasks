package com.todoroo.astrid.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.TextViewWithMeasureListener.OnTextMeasureListener;

public class EditNotesControlSet extends PopupControlSet {

    protected EditText editText;
    protected TextViewWithMeasureListener notesPreview;
    private LinearLayout notesBody;

    public EditNotesControlSet(Activity activity, int viewLayout, int displayViewLayout) {
        super(activity, viewLayout, displayViewLayout, R.string.TEA_note_label);
    }

    @Override
    protected void refreshDisplayView() {
        CharSequence textToUse;
        if (initialized)
            textToUse = editText.getText();
        else
            textToUse = model.getValue(Task.NOTES);

        notesPreview.setText(textToUse);
        setupGravity();
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
        notesPreview = (TextViewWithMeasureListener) getDisplayView().findViewById(R.id.display_row_edit);
        notesBody = (LinearLayout) getDisplayView().findViewById(R.id.notes_body);
        notesPreview.setOnTextSizeChangedListener(new OnTextMeasureListener() {
            @Override
            public void onTextSizeChanged() {
                setupGravity();
            }
        });
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

    private void setupGravity() {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        if (hasNotes() && notesPreview.getLineCount() > 2) {
            notesBody.setGravity(Gravity.TOP);
            notesBody.setPadding( notesBody.getPaddingLeft(), (int) (metrics.density * 8),  notesBody.getPaddingRight(), (int) (metrics.density * 8));
        } else {
            notesBody.setGravity(Gravity.CENTER_VERTICAL);
            notesBody.setPadding( notesBody.getPaddingLeft(), 0,  notesBody.getPaddingRight(), 0);
        }
    }

}
