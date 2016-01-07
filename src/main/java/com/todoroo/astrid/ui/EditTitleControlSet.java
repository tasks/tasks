/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.repeats.RepeatControlSet.RepeatChangedListener;
import com.todoroo.astrid.service.TaskService;

import org.tasks.ui.CheckBoxes;
import org.tasks.ui.PriorityControlSet.ImportanceChangedListener;

/**
 * Control set for mapping a Property to an EditText
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditTitleControlSet implements TaskEditControlSet, ImportanceChangedListener, RepeatChangedListener {

    private final EditText editText;
    private CheckableImageView completeBox;

    private final CheckBoxes checkBoxes;
    private boolean isRepeating;
    private int importanceValue;
    private Task model;
    private final TaskService taskService;

    public EditTitleControlSet(TaskService taskService, final Activity activity, final EditText editText, CheckableImageView completeBox) {
        this.checkBoxes = new CheckBoxes(activity);
        this.editText = editText;
        this.completeBox = completeBox;
        this.taskService = taskService;

        editText.setHorizontallyScrolling(false);
        editText.setMaxLines(Integer.MAX_VALUE);
        editText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    AndroidUtilities.hideSoftInputForViews(activity, editText);
                    return true;
                }
                return false;
            }
        });
        editText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.setCursorVisible(true);
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    editText.setCursorVisible(false);
                }
                return false;
            }
        });
    }

    protected void readFromTaskOnInitialize() {
        editText.setTextKeepState(model.getTitle());
        completeBox.setChecked(model.isCompleted());
        completeBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // set check box to actual action item state
                updateCompleteBox();
            }
        });
    }

    @Override
    public void importanceChanged(int i) {
        importanceValue = i;
        updateCompleteBox();
    }


    @Override
    public void repeatChanged(boolean repeat) {
        isRepeating = repeat;
        updateCompleteBox();
    }

    @Override
    public void readFromTask(Task task) {
        this.model = task;
        readFromTaskOnInitialize();
        isRepeating = !TextUtils.isEmpty(task.getRecurrence());
        importanceValue = model.getImportance();
    }

    private void updateCompleteBox() {
        boolean checked = completeBox.isChecked();

        if (checked) {
            completeBox.setImageDrawable(checkBoxes.getCompletedCheckbox(importanceValue));
        } else if (isRepeating) {
            completeBox.setImageDrawable(checkBoxes.getRepeatingCheckBox(importanceValue));
        } else {
            completeBox.setImageDrawable(checkBoxes.getCheckBox(importanceValue));
        }

        if (checked) {
            editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            editText.setPaintFlags(editText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    @Override
    public void writeToModel(Task task) {
        task.setTitle(editText.getText().toString());
        boolean newState = completeBox.isChecked();
        if (newState != task.isCompleted()) {
            taskService.setComplete(task, newState);
        }
    }

    @Override
    public int getIcon() {
        return -1;
    }

    @Override
    public View getView() {
        throw new RuntimeException();
    }
}
