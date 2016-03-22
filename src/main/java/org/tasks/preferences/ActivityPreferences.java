package org.tasks.preferences;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.view.Window;

import org.tasks.R;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.todoroo.andlib.utility.AndroidUtilities.preLollipop;

@Singleton
public class ActivityPreferences extends Preferences {

    private final Activity activity;

    @Inject
    public ActivityPreferences(Activity activity, PermissionChecker permissionChecker) {
        super(activity, permissionChecker);
        this.activity = activity;
    }

    public void applyThemeAndStatusBarColor() {
        applyTheme();
        applyStatusBarColor();
    }

    public void applyTheme() {
        applyTheme(getTheme());
    }

    public void applyDialogTheme() {
        applyTheme(getDialogTheme());
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
}
