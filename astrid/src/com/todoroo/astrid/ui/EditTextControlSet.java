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
    private EditText editText;
    private final StringProperty property;
    protected CheckBox completeBox;
    private final int editTextId;

    @Autowired
    private TaskService taskService;


    public EditTextControlSet(Activity activity, int layout, StringProperty property, int editText) {
        super(activity, layout);
        this.property = property;
        this.editTextId = editText;
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void afterInflate() {
        this.editText = (EditText) getView().findViewById(editTextId);
        this.completeBox = (CheckBox) getView().findViewById(R.id.completeBox);
    }

    @Override
    protected void readFromTaskPrivate() {
        editText.setTextKeepState(model.getValue(property));
        completeBox.setChecked(model.isCompleted());
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
    protected String writeToModelPrivate(Task task) {
        task.setValue(property, editText.getText().toString());
        boolean newState = completeBox.isChecked();
        if (newState != task.isCompleted()) {
            taskService.setComplete(task, newState);
        }
        return null;
    }

}