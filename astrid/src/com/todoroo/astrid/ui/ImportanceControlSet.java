package com.todoroo.astrid.ui;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.opencrx.OpencrxCoreUtils;
import com.todoroo.astrid.producteev.ProducteevUtilities;

/**
 * Control Set for setting task importance
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ImportanceControlSet extends TaskEditControlSet {
    private final List<CompoundButton> buttons = new LinkedList<CompoundButton>();
    private final int[] colors;
    //private final int grayColor;
    private final List<ImportanceChangedListener> listeners = new LinkedList<ImportanceChangedListener>();

    public interface ImportanceChangedListener {
        public void importanceChanged(int i, int color);
    }

    public ImportanceControlSet(Activity activity, int layout) {
        super(activity, layout);
        LinearLayout container = (LinearLayout) getView().findViewById(R.id.importance_container);
        colors = Task.getImportanceColors(activity.getResources());

        int min = Task.IMPORTANCE_MOST;
        int max = Task.IMPORTANCE_LEAST;
        //grayColor = colors[max];
        if(ProducteevUtilities.INSTANCE.isLoggedIn() || OpencrxCoreUtils.INSTANCE.isLoggedIn())
            max = 5;
        //else
            //colors[max] = activity.getResources().getColor(android.R.color.white);

        for(int i = min; i <= max; i++) {
            final ToggleButton button = new ToggleButton(activity);
            LinearLayout.LayoutParams params;
            if (ProducteevUtilities.INSTANCE.isLoggedIn())
                params = new LinearLayout.LayoutParams(55, 55);
            else
                params = new LinearLayout.LayoutParams(60, 60);
            button.setLayoutParams(params);

            StringBuilder label = new StringBuilder();
            if(ProducteevUtilities.INSTANCE.isLoggedIn() || OpencrxCoreUtils.INSTANCE.isLoggedIn())
                label.append(5 - i).append("\n\u2605"); //$NON-NLS-1$
            else {
                if (i == max)
                    label.append('\u25CB');
                for(int j = Task.IMPORTANCE_LEAST - 1; j >= i; j--)
                    label.append('!');
            }

            button.setTextColor(colors[i]);
            button.setTextOff(label);
            button.setTextOn(label);
            button.setPadding(0, 1, 0, 0);

            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    setImportance((Integer)button.getTag());
                }
            });
            button.setTag(i);

            buttons.add(button);
            container.addView(button);
        }
    }

    public void setImportance(Integer i) {
        for(CompoundButton b : buttons) {
            if(b.getTag() == i) {
                b.setTextSize(getTextSize());
                b.setChecked(true);
                //if (i.intValue() == Task.IMPORTANCE_LEAST)
                //    b.setTextColor(grayColor);
                b.setBackgroundResource(R.drawable.importance_background_selected);
            } else {
                b.setTextSize(getTextSize());
                b.setChecked(false);
                b.setTextColor(colors[(Integer)b.getTag()]);
                b.setBackgroundResource(0);
            }
        }

        for (ImportanceChangedListener l : listeners) {
            l.importanceChanged(i, colors[i]);
        }
    }

    private int getTextSize() {
        if (ProducteevUtilities.INSTANCE.isLoggedIn())
            return 14;
        else
            return 24;
    }

    public Integer getImportance() {
        for(CompoundButton b : buttons)
            if(b.isChecked())
                return (Integer) b.getTag();
        return null;
    }

    public void addListener(ImportanceChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ImportanceChangedListener listener) {
        if (listeners.contains(listener))
            listeners.remove(listener);
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