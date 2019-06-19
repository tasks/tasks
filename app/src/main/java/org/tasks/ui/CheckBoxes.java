package org.tasks.ui;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.todoroo.astrid.data.Task;
import org.tasks.R;

public class CheckBoxes {

  private CheckBoxes() {}

  private static Drawable getDrawable(Context context, @DrawableRes int resId, int priority) {
    Drawable original = ContextCompat.getDrawable(context, resId);
    Drawable wrapped = DrawableCompat.wrap(original.mutate());
    DrawableCompat.setTint(wrapped, getColor(context, getPriorityResId(priority)));
    return wrapped;
  }

  private static int getPriorityResId(int priority) {
    if (priority <= 0) {
      return R.color.priority_1;
    } else if (priority == 1) {
      return R.color.priority_2;
    } else if (priority == 2) {
      return R.color.priority_3;
    } else {
      return R.color.priority_4;
    }
  }

  private static Bitmap convertToBitmap(Drawable d) {
    Bitmap bitmap =
        Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    d.draw(canvas);
    return bitmap;
  }

  public static Bitmap getCheckBoxBitmap(Context context, Task task) {
    return convertToBitmap(getCheckBox(context, task));
  }

  public static Drawable getCheckBox(Context context, Task task) {
    return getCheckBox(context, task.isCompleted(), task.isRecurring(), task.getPriority());
  }

  public static Drawable getCheckBox(
      Context context, boolean complete, boolean repeating, int priority) {
    if (complete) {
      return getDrawable(context, R.drawable.ic_outline_check_box_24px, priority);
    } else if (repeating) {
      return getDrawable(context, R.drawable.ic_outline_repeat_24px, priority);
    } else {
      return getDrawable(context, R.drawable.ic_outline_check_box_outline_blank_24px, priority);
    }
  }

  public static int getPriorityColor(Context context, int priority) {
    return getColor(context, getPriorityResId(priority));
  }
}
