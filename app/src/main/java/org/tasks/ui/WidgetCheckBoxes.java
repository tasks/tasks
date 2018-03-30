package org.tasks.ui;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.util.List;
import javax.inject.Inject;
import org.tasks.injection.ApplicationScope;
import timber.log.Timber;

@ApplicationScope
public class WidgetCheckBoxes {

  private final List<Bitmap> checkboxes;
  private final List<Bitmap> repeatingCheckboxes;
  private final List<Bitmap> completedCheckboxes;

  @Inject
  public WidgetCheckBoxes(CheckBoxes checkBoxes) {
    Timber.d("Initializing widget checkboxes");
    checkboxes = convertToBitmap(checkBoxes.getCheckBoxes());
    repeatingCheckboxes = convertToBitmap(checkBoxes.getRepeatingCheckBoxes());
    completedCheckboxes = convertToBitmap(checkBoxes.getCompletedCheckBoxes());
  }

  private static List<Bitmap> convertToBitmap(final List<Drawable> drawables) {
    return newArrayList(
        transform(
            drawables,
            drawable -> {
              if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                if (bitmapDrawable.getBitmap() != null) {
                  return bitmapDrawable.getBitmap();
                }
              }

              Bitmap bitmap =
                  drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0
                      ? Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                      : Bitmap.createBitmap(
                          drawable.getIntrinsicWidth(),
                          drawable.getIntrinsicHeight(),
                          Bitmap.Config.ARGB_8888);

              Canvas canvas = new Canvas(bitmap);
              drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
              drawable.draw(canvas);
              return bitmap;
            }));
  }

  public Bitmap getCompletedCheckbox(int importance) {
    return completedCheckboxes.get(importance);
  }

  public Bitmap getRepeatingCheckBox(int importance) {
    return repeatingCheckboxes.get(importance);
  }

  public Bitmap getCheckBox(int importance) {
    return checkboxes.get(importance);
  }
}
