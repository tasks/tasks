package org.tasks.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import javax.inject.Inject;
import org.tasks.injection.ApplicationScope;

@ApplicationScope
public class WidgetCheckBoxes {

  private final CheckBoxes checkBoxes;
  private final Bitmap[] incomplete = new Bitmap[4];
  private final Bitmap[] repeating = new Bitmap[4];
  private final Bitmap[] completed = new Bitmap[4];

  @Inject
  public WidgetCheckBoxes(CheckBoxes checkBoxes) {
    this.checkBoxes = checkBoxes;
  }

  private static Bitmap convertToBitmap(Drawable d) {
    Bitmap bitmap =
        Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    d.draw(canvas);
    return bitmap;
  }

  public Bitmap getCompletedCheckbox(int importance) {
    if (completed[importance] == null) {
      completed[importance] = convertToBitmap(checkBoxes.getCompletedCheckbox(importance));
    }
    return completed[importance];
  }

  public Bitmap getRepeatingCheckBox(int importance) {
    if (repeating[importance] == null) {
      repeating[importance] = convertToBitmap(checkBoxes.getRepeatingCheckBox(importance));
    }
    return repeating[importance];
  }

  public Bitmap getCheckBox(int importance) {
    if (incomplete[importance] == null) {
      incomplete[importance] = convertToBitmap(checkBoxes.getCheckBox(importance));
    }
    return incomplete[importance];
  }
}
