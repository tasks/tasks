package org.tasks.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.common.base.Function;

import org.tasks.injection.ForApplication;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

public class WidgetCheckBoxes {

    private static boolean initialized;
    private static List<Bitmap> checkboxes;
    private static List<Bitmap> repeatingCheckboxes;
    private static List<Bitmap> completedCheckboxes;

    @Inject
    public WidgetCheckBoxes(@ForApplication Context context) {
        if (!initialized) {
            CheckBoxes checkBoxes = new CheckBoxes(context);
            Timber.d("Initializing widget checkboxes");
            checkboxes = convertToBitmap(checkBoxes.getCheckBoxes());
            repeatingCheckboxes = convertToBitmap(checkBoxes.getRepeatingCheckBoxes());
            completedCheckboxes = convertToBitmap(checkBoxes.getCompletedCheckBoxes());
            initialized = true;
        }
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

    private static List<Bitmap> convertToBitmap(final List<Drawable> drawables) {
        return newArrayList(transform(drawables, new Function<Drawable, Bitmap>() {
            @Nullable
            @Override
            public Bitmap apply(Drawable drawable) {
                if (drawable instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                    if (bitmapDrawable.getBitmap() != null) {
                        return bitmapDrawable.getBitmap();
                    }
                }

                Bitmap bitmap = drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0
                        ? Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                        : Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                return bitmap;
            }
        }));
    }
}
