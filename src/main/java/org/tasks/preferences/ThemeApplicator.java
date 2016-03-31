package org.tasks.preferences;

import android.app.Activity;
import android.graphics.PixelFormat;

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
    }

    public void applyTheme() {
        Theme appTheme = themeManager.getAppTheme();
        applyTheme(appTheme.getAppThemeResId());
    }

    public void applyDialogTheme() {
        Theme appTheme = themeManager.getAppTheme();
        applyTheme(appTheme.getDialogThemeResId());
    }

    private void applyTheme(int theme) {
        activity.setTheme(theme);
        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    private void applyStatusBarColor() {
        if (atLeastLollipop()) {
            Theme appTheme = themeManager.getAppTheme();
            activity.getWindow().setStatusBarColor(appTheme.getPrimaryDarkColor());
        }
    }
}
