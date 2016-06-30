package org.tasks.themes;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;

import org.tasks.R;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class Theme {
    private final ThemeBase themeBase;
    private final ThemeColor themeColor;
    private final ThemeAccent themeAccent;

    @Inject
    public Theme(ThemeBase themeBase, ThemeColor themeColor, ThemeAccent themeAccent) {
        this.themeBase = themeBase;
        this.themeColor = themeColor;
        this.themeAccent = themeAccent;
    }

    public ThemeBase getThemeBase() {
        return themeBase;
    }

    public ThemeColor getThemeColor() {
        return themeColor;
    }

    public int getDialogStyle() {
        return themeBase.getDialogStyle();
    }

    public LayoutInflater getLayoutInflater(Context context) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(context, themeBase.getStyle());
        applyToContext(wrapper);
        return (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public ContextThemeWrapper getThemedDialog(Context context) {
        return new ContextThemeWrapper(context, getDialogStyle());
    }

    public void applyThemeAndStatusBarColor(Activity activity) {
        applyTheme(activity);
        themeColor.applyStatusBarColor(activity);
        applyTaskDescription(activity, activity.getString(R.string.app_name));
    }

    public void applyTheme(Activity activity) {
        activity.setTheme(themeBase.getStyle());
        applyToContext(activity);
        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    public void applyTaskDescription(Activity activity, String description) {
        if (atLeastLollipop()) {
            activity.setTaskDescription(new ActivityManager.TaskDescription(description, null, themeColor.getPrimaryColor()));
        }
    }

    public void applyToContext(Context context) {
        Resources.Theme theme = context.getTheme();
        theme.applyStyle(themeColor.getStyle(), true);
        theme.applyStyle(themeAccent.getStyle(), true);
    }
}
