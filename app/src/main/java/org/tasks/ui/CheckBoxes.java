package org.tasks.ui;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;

@ApplicationScope
public class CheckBoxes {

  private static final int MIN_PRIORITY = 0;
  private static final int MAX_PRIORITY = 3;

  private final Drawable[] checkboxes = new Drawable[4];
  private final Drawable[] repeatingCheckboxes = new Drawable[4];
  private final Drawable[] completedCheckboxes = new Drawable[4];
  private final Context context;
  private final int[] priorityColors;

  @Inject
  public CheckBoxes(@ForApplication Context context) {
    this.context = context;
    priorityColors =
        new int[] {
          getColor(context, R.color.priority_1),
          getColor(context, R.color.priority_2),
          getColor(context, R.color.priority_3),
          getColor(context, R.color.priority_4)
        };
  }

  private static Drawable getDrawable(Context context, @DrawableRes int resId, int priority) {
    Drawable original = ContextCompat.getDrawable(context, resId);
    Drawable wrapped = DrawableCompat.wrap(original.mutate());
    DrawableCompat.setTint(wrapped, getColor(context, getPriorityResId(priority)));
    return wrapped;
  }

  private static int getPriorityResId(int priority) {
    switch (priority) {
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
    return priorityColors[Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, priority))];
  }

  public int[] getPriorityColors() {
    return priorityColors;
  }

  public Drawable getCompletedCheckbox(int priority) {
    priority = Math.min(priority, MAX_PRIORITY);
    if (completedCheckboxes[priority] == null) {
      completedCheckboxes[priority] =
          getDrawable(context, R.drawable.ic_outline_check_box_24px, priority);
    }
    return completedCheckboxes[priority];
  }

  public Drawable getRepeatingCheckBox(int priority) {
    priority = Math.min(priority, MAX_PRIORITY);
    if (repeatingCheckboxes[priority] == null) {
      repeatingCheckboxes[priority] =
          getDrawable(context, R.drawable.ic_outline_repeat_24px, priority);
    }
    return repeatingCheckboxes[priority];
  }

  public Drawable getCheckBox(int priority) {
    priority = Math.min(priority, MAX_PRIORITY);
    if (checkboxes[priority] == null) {
      checkboxes[priority] =
          getDrawable(context, R.drawable.ic_outline_check_box_outline_blank_24px, priority);
    }
    return checkboxes[priority];
  }
}
