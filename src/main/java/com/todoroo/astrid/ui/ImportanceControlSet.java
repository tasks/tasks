/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.tasks.R;

import java.util.LinkedList;
import java.util.List;

import static org.tasks.preferences.ResourceResolver.getResource;

/**
 * Control Set for setting task importance
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ImportanceControlSet extends TaskEditControlSetBase {
    private final List<CompoundButton> buttons = new LinkedList<>();
    private final int[] colors;
    private final List<ImportanceChangedListener> listeners = new LinkedList<>();

    private static final int TEXT_SIZE = 18;

    public interface ImportanceChangedListener {
        void importanceChanged(int i);
    }

    public ImportanceControlSet(Activity activity) {
        super(activity, R.layout.control_set_importance);
        colors = Task.getImportanceColors(activity.getResources());
    }

    public void setImportance(Integer i) {
        for(CompoundButton b : buttons) {
            if(b.getTag() == i) {
                b.setChecked(true);
                b.setBackgroundResource(getResource(activity, R.attr.importance_background_selected));
            } else {
                b.setChecked(false);
                b.setBackgroundResource(0);
            }
        }

        for (ImportanceChangedListener l : listeners) {
            l.importanceChanged(i);
        }
    }

    public Integer getImportance() {
        for(CompoundButton b : buttons) {
            if (b.isChecked()) {
                return (Integer) b.getTag();
            }
        }
        return null;
    }

    public void addListener(ImportanceChangedListener listener) {
        listeners.add(listener);
    }

    @Override
    protected void afterInflate() {
        LinearLayout container = (LinearLayout) getView().findViewById(R.id.importance_container);

        int min = Task.IMPORTANCE_MOST;
        int max = Task.IMPORTANCE_LEAST;

        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        for(int i = max; i >= min; i--) {
            final ToggleButton button = new ToggleButton(activity);
            LinearLayout.LayoutParams params;

            int dimension = 25;
            params = new LinearLayout.LayoutParams((int) (metrics.density * dimension), (int) (metrics.density * dimension));
            button.setLayoutParams(params);

            StringBuilder label = new StringBuilder();
            if (i == max) {
                label.append('\u25CB');
            }
            for(int j = Task.IMPORTANCE_LEAST - 1; j >= i; j--) {
                label.append('!');
            }

            button.setTextColor(colors[i]);
            button.setTextOff(label);
            button.setTextOn(label);
            button.setPadding(0, 1, 0, 0);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setImportance((Integer)button.getTag());
                }
            });
            button.setTag(i);
            button.setTextSize(TEXT_SIZE);

            buttons.add(button);

            View padding = new View(activity);
            LinearLayout.LayoutParams paddingParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, 0);
            paddingParams.weight = 1.0f;
            padding.setLayoutParams(paddingParams);
            container.addView(padding);
            container.addView(button);
        }
    }

    @Override
    public void readFromTask(Task task) {
        super.readFromTask(task);
        setImportance(model.getImportance());
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_flag_24dp;
    }

    // Same as above because we need the setImportance listeners to fire even in
    // the case when the UI hasn't been created yet
    @Override
    protected void readFromTaskOnInitialize() {
        setImportance(model.getImportance());
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        if(getImportance() != null) {
            task.setImportance(getImportance());
        }
    }
}
