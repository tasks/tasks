package com.todoroo.astrid.ui;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
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
    protected CheckableImageView completeBox;
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
        this.completeBox = (CheckableImageView) getView().findViewById(R.id.completeBox);
    }

    @Override
    protected void readFromTaskOnInitialize() {
        editText.setTextKeepState(model.getValue(property));
        completeBox.setChecked(model.isCompleted());
        completeBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ScaleAnimation scaleAnimation = new ScaleAnimation(1.5f, 1.0f, 1.5f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(100);
                // set check box to actual action item state
                completeBox.startAnimation(scaleAnimation);
            }
        });
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        task.setValue(property, editText.getText().toString());
        boolean newState = completeBox.isChecked();
        if (newState != task.isCompleted()) {
            taskService.setComplete(task, newState);
        }
        return null;
    }

}