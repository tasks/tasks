/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.ui.CheckBoxes;
import org.tasks.ui.TaskEditControlFragment;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Control set for mapping a Property to an EditText
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditTitleControlSet extends TaskEditControlFragment {

    private static final String EXTRA_COMPLETE = "extra_complete";
    private static final String EXTRA_TITLE = "extra_title";
    private static final String EXTRA_REPEATING = "extra_repeating";
    private static final String EXTRA_PRIORITY = "extra_priority";

    @Inject TaskService taskService;

    @Bind(R.id.title) EditText editText;
    @Bind(R.id.completeBox) CheckableImageView completeBox;

    private CheckBoxes checkBoxes;
    private boolean isComplete;
    private boolean isRepeating;
    private int importanceValue;
    private boolean isNewTask;
    private String title;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        checkBoxes = new CheckBoxes(activity);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), null);
        ButterKnife.bind(this, view);
        if (savedInstanceState != null) {
            isComplete = savedInstanceState.getBoolean(EXTRA_COMPLETE);
            title = savedInstanceState.getString(EXTRA_TITLE);
            isRepeating = savedInstanceState.getBoolean(EXTRA_REPEATING);
            importanceValue = savedInstanceState.getInt(EXTRA_PRIORITY);
        }
        completeBox.setChecked(isComplete);
        editText.setTextKeepState(title);
        editText.setHorizontallyScrolling(false);
        editText.setMaxLines(Integer.MAX_VALUE);
        editText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    AndroidUtilities.hideSoftInputForViews(getActivity(), editText);
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
        updateCompleteBox();
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(EXTRA_COMPLETE, completeBox.isChecked());
        outState.putString(EXTRA_TITLE, getTitle());
        outState.putBoolean(EXTRA_REPEATING, isRepeating);
        outState.putInt(EXTRA_PRIORITY, importanceValue);
    }

    @OnClick(R.id.completeBox)
    void toggleComplete(View view) {
        updateCompleteBox();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (isNewTask) {
            editText.requestFocus();
            editText.setCursorVisible(true);
            getActivity().getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    public void setPriority(int priority) {
        importanceValue = priority;
        updateCompleteBox();
    }

    public void repeatChanged(boolean repeat) {
        isRepeating = repeat;
        updateCompleteBox();
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
    protected int getLayout() {
        return R.layout.control_set_title;
    }

    @Override
    public int getIcon() {
        return -1;
    }

    @Override
    public void initialize(boolean isNewTask, Task task) {
        this.isNewTask = isNewTask;

        isComplete = task.isCompleted();
        title = task.getTitle();
        isRepeating = !TextUtils.isEmpty(task.getRecurrence());
        importanceValue = task.getImportance();
    }

    @Override
    public void apply(Task task) {
        task.setTitle(getTitle());
        boolean newState = completeBox.isChecked();
        if (newState != task.isCompleted()) {
            taskService.setComplete(task, newState);
        }
    }

    public String getTitle() {
        return editText.getText().toString();
    }

    public void hideKeyboard() {
        AndroidUtilities.hideSoftInputForViews(getActivity(), editText);
        editText.setCursorVisible(false);
    }
}
