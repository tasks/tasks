package org.tasks.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

import org.tasks.R;

import java.util.List;

import static android.support.v4.content.ContextCompat.getColor;

public class CheckBoxes {

    public static CheckBoxes newCheckBoxes(Context context) {
        return new CheckBoxes(context);
    }

    private static final int MAX_IMPORTANCE_INDEX = 3;

    private final List<Drawable> checkboxes;
    private final List<Drawable> repeatingCheckboxes;
    private final List<Drawable> completedCheckboxes;
    private final List<Integer> priorityColors;
    private final int[] priorityColorsArray;

    private CheckBoxes(Context context) {
        checkboxes = wrapDrawable(context, R.drawable.ic_check_box_outline_blank_24dp);
        repeatingCheckboxes = wrapDrawable(context, R.drawable.ic_repeat_24dp);
        completedCheckboxes = wrapDrawable(context, R.drawable.ic_check_box_24dp);
        priorityColors = ImmutableList.of(
                getColor(context, R.color.importance_1),
                getColor(context, R.color.importance_2),
                getColor(context, R.color.importance_3),
                getColor(context, R.color.importance_4));
        priorityColorsArray = Ints.toArray(priorityColors);
    }

    public List<Integer> getPriorityColors() {
        return priorityColors;
    }

    public int[] getPriorityColorsArray() {
        return priorityColorsArray;
    }

    List<Drawable> getCheckBoxes() {
        return checkboxes;
    }

    List<Drawable> getRepeatingCheckBoxes() {
        return repeatingCheckboxes;
    }

    List<Drawable> getCompletedCheckBoxes() {
        return completedCheckboxes;
    }

    private static List<Drawable> wrapDrawable(Context context, int resId) {
        return ImmutableList.of(
                getDrawable(context, resId, 0),
                getDrawable(context, resId, 1),
                getDrawable(context, resId, 2),
                getDrawable(context, resId, 3));
    }

    public Drawable getCompletedCheckbox(int importance) {
        return completedCheckboxes.get(Math.min(importance, MAX_IMPORTANCE_INDEX));
    }

    public Drawable getRepeatingCheckBox(int importance) {
        return repeatingCheckboxes.get(Math.min(importance, MAX_IMPORTANCE_INDEX));
    }

    public Drawable getCheckBox(int importance) {
        return checkboxes.get(Math.min(importance, MAX_IMPORTANCE_INDEX));
    }

    private static Drawable getDrawable(Context context, int resId, int importance) {
        Drawable original = ContextCompat.getDrawable(context, resId);
        Drawable wrapped = DrawableCompat.wrap(original.mutate());
        DrawableCompat.setTint(wrapped, getColor(context, getImportanceResId(importance)));
        return wrapped;
    }

    private static int getImportanceResId(int importance) {
        switch (importance) {
            case 0:
                return R.color.importance_1;
            case 1:
                return R.color.importance_2;
            case 2:
                return R.color.importance_3;
            default:
                return R.color.importance_4;
        }
    }
}
