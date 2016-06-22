package org.tasks.preferences;

import android.app.Activity;
import android.content.res.Resources;
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
        applyTheme(themeManager.getBaseTheme(), themeManager.getColorTheme(), themeManager.getAccentTheme());
    }

    private void applyTheme(Theme theme, Theme color, Theme accent) {
        activity.setTheme(theme.getResId());
        Resources.Theme activityTheme = activity.getTheme();
        activityTheme.applyStyle(color.getResId(), true);
        activityTheme.applyStyle(accent.getResId(), true);
        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    private void applyStatusBarColor() {
        if (atLeastLollipop()) {
            Theme appTheme = themeManager.getColorTheme();
            activity.getWindow().setStatusBarColor(appTheme.getPrimaryDarkColor());
        }
    }
}
