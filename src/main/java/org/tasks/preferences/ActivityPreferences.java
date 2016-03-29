package org.tasks.preferences;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import org.tasks.R;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

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

    public String getThemeName() {
        int themeIndex = getInt(R.string.p_theme, 0);
        String[] themeNames = activity.getResources().getStringArray(R.array.themes);
        return themeNames[themeIndex];
    }

    public void applyTheme() {
        applyTheme(getTheme());
    }

    public void applyDialogTheme() {
        applyTheme(getDialogTheme());
    }

    private void applyTheme(int theme) {
        activity.setTheme(theme);
        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    private void applyStatusBarColor() {
        if (atLeastLollipop()) {
            activity.getWindow().setStatusBarColor(getPrimaryDarkColor());
        }
    }

    public int getTheme() {
        return getTheme(getInt(R.string.p_theme, -1));
    }

    public int getDialogTheme() {
        Context contextThemeWrapper = new ContextThemeWrapper(activity, getTheme());
        TypedValue typedValue = new TypedValue();
        contextThemeWrapper.getTheme().resolveAttribute(R.attr.alertDialogTheme, typedValue, true);
        return typedValue.data;
    }

    public int getDateTimePickerAccent() {
        Context contextThemeWrapper = new ContextThemeWrapper(activity, getTheme());
        TypedValue typedValue = new TypedValue();
        contextThemeWrapper.getTheme().resolveAttribute(R.attr.asDateTimePickerAccent, typedValue, true);
        return typedValue.data;
    }

    public int getPrimaryDarkColor() {
        return getColorAttribute(R.attr.colorPrimaryDark);
    }

    private int getColorAttribute(int attribute) {
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(attribute, typedValue, true);
        return typedValue.data;
    }

    public int getPrimaryColor(int themeIndex) {
        Context contextThemeWrapper = new ContextThemeWrapper(activity, getTheme(themeIndex));
        TypedValue typedValue = new TypedValue();
        contextThemeWrapper.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }

    public int getTheme(int index) {
        switch (index) {
            case 1:
                return R.style.Black;
            case 2:
                return R.style.Red;
            case 3:
                return R.style.Pink;
            case 4:
                return R.style.Purple;
            case 5:
                return R.style.DeepPurple;
            case 6:
                return R.style.Indigo;
            case 7:
                return R.style.Blue;
            case 8:
                return R.style.LightBlue;
            case 9:
                return R.style.Cyan;
            case 10:
                return R.style.Teal;
            case 11:
                return R.style.Green;
            case 12:
                return R.style.LightGreen;
            case 13:
                return R.style.Lime;
            case 14:
                return R.style.Yellow;
            case 15:
                return R.style.Amber;
            case 16:
                return R.style.Orange;
            case 17:
                return R.style.DeepOrange;
            case 18:
                return R.style.Brown;
            case 19:
                return R.style.Grey;
            case 0:
            default:
                return R.style.BlueGrey;
        }
    }
}
