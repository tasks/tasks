package com.todoroo.astrid.ui;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.opencrx.OpencrxCoreUtils;
import com.todoroo.astrid.producteev.ProducteevUtilities;

/**
 * Control Set for setting task importance
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ImportanceControlSet implements TaskEditControlSet {
    private final List<CompoundButton> buttons = new LinkedList<CompoundButton>();
    private final int[] colors;

    public ImportanceControlSet(Activity activity, int containerId) {
        LinearLayout layout = (LinearLayout)activity.findViewById(containerId);
        colors = Task.getImportanceColors(activity.getResources());

        int min = Task.IMPORTANCE_MOST;
        int max = Task.IMPORTANCE_LEAST;
        if(ProducteevUtilities.INSTANCE.isLoggedIn() || OpencrxCoreUtils.INSTANCE.isLoggedIn())
            max = 5;

        for(int i = min; i <= max; i++) {
            final ToggleButton button = new ToggleButton(activity);
            button.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

            StringBuilder label = new StringBuilder();
            if(ProducteevUtilities.INSTANCE.isLoggedIn() || OpencrxCoreUtils.INSTANCE.isLoggedIn())
                label.append(5 - i).append("\n\u2605"); //$NON-NLS-1$
            else {
                for(int j = Task.IMPORTANCE_LEAST; j >= i; j--)
                    label.append('!');
            }

            button.setTextColor(colors[i]);
            button.setTextOff(label);
            button.setTextOn(label);

            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    setImportance((Integer)button.getTag());
                }
            });
            button.setTag(i);

            buttons.add(button);
            layout.addView(button);
        }
    }

    public void setImportance(Integer i) {
        for(CompoundButton b : buttons) {
            if(b.getTag() == i) {
                b.setTextSize(24);
                b.setChecked(true);
                b.setBackgroundResource(R.drawable.btn_selected);
            } else {
                b.setTextSize(16);
                b.setChecked(false);
                b.setBackgroundResource(android.R.drawable.btn_default);
            }
        }
    }

    public Integer getImportance() {
        for(CompoundButton b : buttons)
            if(b.isChecked())
                return (Integer) b.getTag();
        return null;
    }

    @Override
    public void readFromTask(Task task) {
        setImportance(task.getValue(Task.IMPORTANCE));
    }

    @Override
    public String writeToModel(Task task) {
        if(getImportance() != null)
            task.setValue(Task.IMPORTANCE, getImportance());
        return null;
    }
}