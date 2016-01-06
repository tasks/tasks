package org.tasks.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;

import org.tasks.R;
import org.tasks.injection.ForApplication;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static java.util.Arrays.asList;

public class CheckBoxes {

    private static final int MAX_IMPORTANCE_INDEX = 3;

    private static boolean initialized;
    private static List<Drawable> checkboxes;
    private static List<Drawable> repeatingCheckboxes;
    private static List<Drawable> completedCheckboxes;

    @Inject
    public CheckBoxes(@ForApplication Context context) {
        if (!initialized) {
            Timber.d("Initializing checkboxes");
            checkboxes = wrapDrawable(context, R.drawable.ic_check_box_outline_blank_24dp);
            repeatingCheckboxes = wrapDrawable(context, R.drawable.ic_repeat_24dp);
            completedCheckboxes = wrapDrawable(context, R.drawable.ic_check_box_24dp);
            initialized = true;
        }
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
        return asList(
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
        Resources resources = context.getResources();
        Drawable original = resources.getDrawable(resId);
        Drawable wrapped = DrawableCompat.wrap(original.mutate());
        DrawableCompat.setTint(wrapped, resources.getColor(getImportanceResId(importance)));
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
