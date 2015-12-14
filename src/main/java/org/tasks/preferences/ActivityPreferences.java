package org.tasks.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.Window;

import org.tasks.R;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.todoroo.andlib.utility.AndroidUtilities.preLollipop;

@Singleton
public class ActivityPreferences extends Preferences {

    public static final int MIN_TABLET_WIDTH = 550;
    public static final int MIN_TABLET_HEIGHT = 800;

    private final Activity activity;

    @Inject
    public ActivityPreferences(Activity activity, DeviceInfo deviceInfo, PermissionChecker permissionChecker) {
        super(activity, deviceInfo, permissionChecker);
        this.activity = activity;
    }

    public boolean useTabletLayout() {
        return isTabletSized(context);
    }

    public void applyThemeAndStatusBarColor() {
        applyTheme();
        applyStatusBarColor();
    }

    public void applyTheme() {
        applyTheme(isDarkTheme() ? R.style.TasksDark : R.style.Tasks);
    }

    public void applyDialogTheme() {
        applyTheme(isDarkTheme() ? R.style.TasksDialogDark : R.style.TasksDialog);
    }

    public void applyStatusBarColor() {
        applyStatusBarColor(isDarkTheme() ? android.R.color.black : R.color.primary_dark);
    }

    public void applyLightStatusBarColor() {
        applyStatusBarColor(R.color.primary_dark);
    }

    private void applyStatusBarColor(int color) {
        if (preLollipop()) {
            return;
        }
        Window window = activity.getWindow();
        Resources resources = activity.getResources();
        window.setStatusBarColor(resources.getColor(color));
    }

    public void applyTranslucentDialogTheme() {
        applyTheme(R.style.ReminderDialog);
    }

    private void applyTheme(int theme) {
        activity.setTheme(theme);
        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    public int getTheme() {
        return isDarkTheme() ? R.style.TasksDark : R.style.Tasks;
    }

    public int getDialogTheme() {
        return isDarkTheme() ? R.style.TasksDialogDark : R.style.TasksDialog;
    }

    public boolean isDarkTheme() {
        return getBoolean(R.string.p_use_dark_theme, false);
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
