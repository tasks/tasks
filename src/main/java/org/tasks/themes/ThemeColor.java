package org.tasks.themes;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.res.Resources;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.tasks.R;
import org.tasks.ui.MenuColorizer;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastMarshmallow;

public class ThemeColor {

    public static final int[] COLORS = new int[] {
            R.style.BlueGrey,
            R.style.DarkGrey,
            R.style.Red,
            R.style.Pink,
            R.style.Purple,
            R.style.DeepPurple,
            R.style.Indigo,
            R.style.Blue,
            R.style.LightBlue,
            R.style.Cyan,
            R.style.Teal,
            R.style.Green,
            R.style.LightGreen,
            R.style.Lime,
            R.style.Yellow,
            R.style.Amber,
            R.style.Orange,
            R.style.DeepOrange,
            R.style.Brown,
            R.style.Grey
    };

    private final String name;
    private final int index;
    private final int actionBarTint;
    private final int style;
    private final int colorPrimary;
    private final int colorPrimaryDark;
    private final boolean isDark;

    public ThemeColor(String name, int index, int colorPrimary, int colorPrimaryDark, int actionBarTint, boolean isDark) {
        this.name = name;
        this.index = index;
        this.actionBarTint = actionBarTint;
        this.style = COLORS[index];
        this.colorPrimary = colorPrimary;
        this.colorPrimaryDark = colorPrimaryDark;
        this.isDark = isDark;
    }

    public void applyStatusBarColor(Activity activity) {
        if (atLeastLollipop()) {
            activity.getWindow().setStatusBarColor(getColorPrimaryDark());
        }
        if (atLeastMarshmallow()) {
            View decorView = activity.getWindow().getDecorView();
            int systemUiVisibility = applyLightStatusBarFlag(decorView.getSystemUiVisibility());
            decorView.setSystemUiVisibility(systemUiVisibility);
        }
    }

    public void applyStatusBarColor(DrawerLayout drawerLayout) {
        if (atLeastLollipop()) {
            drawerLayout.setStatusBarBackgroundColor(getColorPrimaryDark());
        }
        if (atLeastMarshmallow()) {
            int systemUiVisibility = applyLightStatusBarFlag(drawerLayout.getSystemUiVisibility());
            drawerLayout.setSystemUiVisibility(systemUiVisibility);
        }
    }

    private int applyLightStatusBarFlag(int flag) {
        return isDark
                ? flag | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                : flag & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    }

    public void applyStyle(Resources.Theme theme) {
        theme.applyStyle(style, true);
    }

    public void applyTaskDescription(Activity activity, String description) {
        if (atLeastLollipop()) {
            activity.setTaskDescription(new ActivityManager.TaskDescription(description, null, getPrimaryColor()));
        }
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public int getPrimaryColor() {
        return colorPrimary;
    }

    public int getActionBarTint() {
        return actionBarTint;
    }

    public int getColorPrimaryDark() {
        return colorPrimaryDark;
    }

    public void apply(Toolbar toolbar) {
        toolbar.setBackgroundColor(getPrimaryColor());
        MenuColorizer.colorToolbar(toolbar, actionBarTint);
    }
}
