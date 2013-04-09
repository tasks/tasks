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
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.repeats.RepeatControlSet.RepeatChangedListener;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.ui.ImportanceControlSet.ImportanceChangedListener;

/**
 * Control set for mapping a Property to an EditText
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditTitleControlSet extends TaskEditControlSet implements ImportanceChangedListener, RepeatChangedListener {
    private EditText editText;
    protected CheckableImageView completeBox;
    private final int editTextId;

    private boolean isRepeating;
    private int importanceValue;

    @Autowired
    private TaskService taskService;

    public EditTitleControlSet(Activity activity, int layout, int editText) {
        super(activity, layout);
        this.editTextId = editText;
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void afterInflate() {
        this.editText = (EditText) getView().findViewById(editTextId);
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
        this.completeBox = (CheckableImageView) getView().findViewById(R.id.completeBox);
    }

    @Override
    protected void readFromTaskOnInitialize() {
        editText.setTextKeepState(model.getValue(Task.TITLE));
        completeBox.setChecked(model.isCompleted());
        completeBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ScaleAnimation scaleAnimation = new ScaleAnimation(1.5f, 1.0f, 1.5f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(100);
                // set check box to actual action item state
                completeBox.startAnimation(scaleAnimation);
                updateCompleteBox();
            }
        });
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        task.setValue(Task.TITLE, editText.getText().toString());
        boolean newState = completeBox.isChecked();
        if (newState != task.isCompleted()) {
            taskService.setComplete(task, newState);
        }
        return null;
    }

    @Override
    public void importanceChanged(int i, int color) {
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
        super.readFromTask(task);
        isRepeating = !TextUtils.isEmpty(task.getValue(Task.RECURRENCE));
        importanceValue = model.getValue(Task.IMPORTANCE);
    }


    private void updateCompleteBox() {
        boolean checked = completeBox.isChecked();
        int[] resourceArray = isRepeating ? (checked ? TaskAdapter.IMPORTANCE_REPEAT_RESOURCES_CHECKED : TaskAdapter.IMPORTANCE_REPEAT_RESOURCES)
                                          : (checked ? TaskAdapter.IMPORTANCE_RESOURCES_CHECKED : TaskAdapter.IMPORTANCE_RESOURCES);
        int valueToUse = importanceValue;
        if (valueToUse >= resourceArray.length)
            valueToUse = resourceArray.length - 1;
        if(valueToUse < resourceArray.length) {
            if (isRepeating) {
                completeBox.setImageResource(resourceArray[valueToUse]);
            } else {
                completeBox.setImageResource(resourceArray[valueToUse]);
            }
        }

        if (checked)
            editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else
            editText.setPaintFlags(editText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }

}
