package org.tasks.themes;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.widget.DrawerLayout;

import org.tasks.R;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

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
    private final int style;
    private final int colorPrimary;
    private final int colorPrimaryDark;

    public ThemeColor(String name, int index, int colorPrimary, int colorPrimaryDark) {
        this.name = name;
        this.index = index;
        this.style = COLORS[index];
        this.colorPrimary = colorPrimary;
        this.colorPrimaryDark = colorPrimaryDark;
    }

    public void applyStatusBarColor(Activity activity) {
        if (atLeastLollipop()) {
            activity.getWindow().setStatusBarColor(getColorPrimaryDark());
        }
    }

    public void applyStatusBarColor(DrawerLayout drawerLayout) {
        if (atLeastLollipop()) {
            drawerLayout.setStatusBarBackgroundColor(getColorPrimaryDark());
        }
    }

    public void applyStyle(Context context) {
        applyStyle(context.getTheme());
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

    public int getColorPrimaryDark() {
        return colorPrimaryDark;
    }
}
