package org.tasks.ui;

import static android.support.v4.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;

@ApplicationScope
public class CheckBoxes {

  private static final int MAX_IMPORTANCE_INDEX = 3;

  private final List<Drawable> checkboxes;
  private final List<Drawable> repeatingCheckboxes;
  private final List<Drawable> completedCheckboxes;
  private final List<Integer> priorityColors;
  private final int[] priorityColorsArray;

  @Inject
  public CheckBoxes(@ForApplication Context context) {
    checkboxes = wrapDrawable(context, R.drawable.ic_check_box_outline_blank_24dp);
    repeatingCheckboxes = wrapDrawable(context, R.drawable.ic_repeat_24dp);
    completedCheckboxes = wrapDrawable(context, R.drawable.ic_check_box_24dp);
    priorityColors =
        ImmutableList.of(
            getColor(context, R.color.priority_1),
            getColor(context, R.color.priority_2),
            getColor(context, R.color.priority_3),
            getColor(context, R.color.priority_4));
    priorityColorsArray = Ints.toArray(priorityColors);
  }

  private static List<Drawable> wrapDrawable(Context context, int resId) {
    return ImmutableList.of(
        getDrawable(context, resId, 0),
        getDrawable(context, resId, 1),
        getDrawable(context, resId, 2),
        getDrawable(context, resId, 3));
  }

  private static Drawable getDrawable(Context context, int resId, int importance) {
    Drawable original = ContextCompat.getDrawable(context, resId);
    Drawable wrapped = DrawableCompat.wrap(original.mutate());
    DrawableCompat.setTint(wrapped, getColor(context, getPriorityResId(importance)));
    return wrapped;
  }

  private static int getPriorityResId(int importance) {
    switch (importance) {
      case 0:
        return R.color.priority_1;
      case 1:
        return R.color.priority_2;
      case 2:
        return R.color.priority_3;
      default:
        return R.color.priority_4;
    }
  }

  public int getPriorityColor(int priority) {
    return priorityColors.get(Math.max(0, Math.min(3, priority)));
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

  public Drawable getCompletedCheckbox(int importance) {
    return completedCheckboxes.get(Math.min(importance, MAX_IMPORTANCE_INDEX));
  }

  public Drawable getRepeatingCheckBox(int importance) {
    return repeatingCheckboxes.get(Math.min(importance, MAX_IMPORTANCE_INDEX));
  }

  public Drawable getCheckBox(int importance) {
    return checkboxes.get(Math.min(importance, MAX_IMPORTANCE_INDEX));
  }
}
