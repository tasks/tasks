package com.todoroo.astrid.ui;

import android.app.Activity;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.service.TaskService;

/**
 * Control set for mapping a Property to an EditText
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditTextControlSet extends TaskEditControlSet {
    private final EditText editText;
    private final StringProperty property;
    protected Task model;
    protected CheckBox completeBox;

    @Autowired
    private TaskService taskService;


    public EditTextControlSet(Activity activity, int layout, StringProperty property, int editText) {
        super(activity, layout);
        this.property = property;
        this.editText = (EditText) getView().findViewById(editText);
        this.completeBox = (CheckBox) getView().findViewById(R.id.completeBox);
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public void readFromTask(Task task) {
        model = task;
        editText.setTextKeepState(task.getValue(property));
        completeBox.setChecked(task.isCompleted());
        completeBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                ScaleAnimation scaleAnimation = new ScaleAnimation(1.6f, 1.0f, 1.6f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(100);
                // set check box to actual action item state
                completeBox.startAnimation(scaleAnimation);
            }
        });
    }

    @Override
    public String writeToModel(Task task) {
        task.setValue(property, editText.getText().toString());
        boolean newState = completeBox.isChecked();
        if (newState != task.isCompleted()) {
            taskService.setComplete(task, newState);
        }
        return null;
    }

}