package org.tasks.preferences;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.PixelFormat;

import org.tasks.R;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class ThemeApplicator {
    private final Activity activity;
    private final ThemeManager themeManager;

    @Inject
    public ThemeApplicator(Activity activity, ThemeManager themeManager) {
        this.activity = activity;
        this.themeManager = themeManager;
    }

    public void applyThemeAndStatusBarColor() {
        applyTheme();
        applyStatusBarColor();
        applyTaskDescription(activity.getString(R.string.app_name));
    }

    public void applyTheme() {
        applyTheme(themeManager.getBaseTheme());
    }

    public void applyTaskDescription(String description) {
        if (atLeastLollipop()) {
            Theme colorTheme = themeManager.getColorTheme();
            activity.setTaskDescription(new ActivityManager.TaskDescription(description, null, colorTheme.getPrimaryColor()));
        }
    }

    private void applyTheme(Theme theme) {
        activity.setTheme(theme.getResId());
        themeManager.applyThemeToContext(activity);
        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    private void applyStatusBarColor() {
        if (atLeastLollipop()) {
            Theme appTheme = themeManager.getColorTheme();
            activity.getWindow().setStatusBarColor(appTheme.getPrimaryDarkColor());
        }
    }
}
