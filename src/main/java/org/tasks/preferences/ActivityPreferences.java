package org.tasks.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;

import com.todoroo.andlib.utility.AndroidUtilities;

import org.tasks.R;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastIceCreamSandwich;

@Singleton
public class ActivityPreferences extends Preferences {

    public static final int MIN_TABLET_WIDTH = 550;
    public static final int MIN_TABLET_HEIGHT = 800;

    private final Activity activity;

    @Inject
    public ActivityPreferences(Activity activity) {
        super(activity);
        this.activity = activity;
    }

    public boolean useTabletLayout() {
        return isTabletSized(context);
    }

    public void applyTheme() {
        applyTheme(R.style.Tasks);
    }

    public void applyDialogTheme() {
        applyTheme(R.style.Tasks_Dialog);
    }

    public void applyTranslucentDialogTheme() {
        applyTheme(R.style.ReminderDialog);
    }

    private void applyTheme(int theme) {
        activity.setTheme(theme);
        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    public int getEditDialogTheme() {
        return atLeastIceCreamSandwich() ? R.style.TEA_Dialog_Light_ICS : R.style.TEA_Dialog;
    }

    /**
     * Returns true if the screen is large or xtra large
     */
    public static boolean isTabletSized(Context context) {
        if (context.getPackageManager().hasSystemFeature("com.google.android.tv")) { //$NON-NLS-1$
            return true;
        }
        int size = context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;

        if (size == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            return true;
        } else if (size == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            float width = metrics.widthPixels / metrics.density;
            float height = metrics.heightPixels / metrics.density;

            float effectiveWidth = Math.min(width, height);
            float effectiveHeight = Math.max(width, height);

            return (effectiveWidth >= MIN_TABLET_WIDTH && effectiveHeight >= MIN_TABLET_HEIGHT);
        } else {
            return false;
        }
    }
}
