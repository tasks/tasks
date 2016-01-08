package org.tasks.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.View;
import android.widget.RadioGroup;

import com.google.common.primitives.Ints;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.tasks.R;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PriorityControlSet extends TaskEditControlSetBase {

    private final List<Integer> colors;
    private final List<ImportanceChangedListener> listeners = new LinkedList<>();
    private RadioGroup radioGroup;

    public interface ImportanceChangedListener {
        void importanceChanged(int i);
    }

    public PriorityControlSet(Activity activity) {
        super(activity, R.layout.control_set_priority);
        colors = Ints.asList(Task.getImportanceColors(activity.getResources()));
        Collections.reverse(colors);
    }

    public void notifyImportanceChange(Integer i) {
        for (ImportanceChangedListener l : listeners) {
            l.importanceChanged(i);
        }
    }

    private Integer getImportance(int checkedId) {
        return getImportance(getView().findViewById(checkedId));
    }

    private Integer getImportance(View view) {
        return Integer.parseInt((String) view.getTag());
    }

    public void addListener(ImportanceChangedListener listener) {
        listeners.add(listener);
    }

    @Override
    protected void afterInflate() {
        final View view = getView();
        radioGroup = (RadioGroup) view.findViewById(R.id.importance_group);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                notifyImportanceChange(getImportance(checkedId));
            }
        });
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            AppCompatRadioButton radioButton = (AppCompatRadioButton) radioGroup.getChildAt(i);
            radioButton.setSupportButtonTintList(new ColorStateList(new int[][]{
                    new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}},
                    new int[]{colors.get(i), colors.get(i)}));
        }
    }

    @Override
    public void readFromTask(Task task) {
        super.readFromTask(task);
        setSelected(model.getImportance());
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_flag_24dp;
    }

    @Override
    protected void readFromTaskOnInitialize() {
        setSelected(model.getImportance());
    }

    private void setSelected(int importance) {
        if (radioGroup == null) {
            return;
        }

        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            AppCompatRadioButton radioButton = (AppCompatRadioButton) radioGroup.getChildAt(i);
            if (importance == getImportance(radioButton)) {
                radioButton.setChecked(true);
            }
        }
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        task.setImportance(getImportance(radioGroup.getCheckedRadioButtonId()));
    }
}
