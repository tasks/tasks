package org.tasks.preferences;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.widget.WidgetConfigActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ThemeManager {

    private static final int[] THEMES = new int[] {
            R.style.LightOverride,
            R.style.BaseBlack,
            R.style.DarkOverride
    };

    private static final int[] COLORS = new int[] {
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

    private static final int[] ACCENTS = new int[] {
            R.style.BlueGreyAccent,
            R.style.RedAccent,
            R.style.PinkAccent,
            R.style.PurpleAccent,
            R.style.DeepPurpleAccent,
            R.style.IndigoAccent,
            R.style.BlueAccent,
            R.style.LightBlueAccent,
            R.style.CyanAccent,
            R.style.TealAccent,
            R.style.GreenAccent,
            R.style.LightGreenAccent,
            R.style.LimeAccent,
            R.style.YellowAccent,
            R.style.AmberAccent,
            R.style.OrangeAccent,
            R.style.DeepOrangeAccent
    };

    private final Context context;
    private final Preferences preferences;
    private final String[] themeNames;
    private final String[] colorNames;
    private final String[] accentNames;

    @Inject
    public ThemeManager(@ForApplication Context context, Preferences preferences) {
        this.context = context;
        this.preferences = preferences;
        Resources resources = context.getResources();
        themeNames = resources.getStringArray(R.array.themes);
        colorNames = resources.getStringArray(R.array.colors);
        accentNames = resources.getStringArray(R.array.accents);
    }

    public boolean isDarkTheme() {
        return preferences.getInt(R.string.p_theme, 0) > 0;
    }

    public LayoutInflater getThemedLayoutInflater() {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(context, getBaseTheme().getResId());
        applyThemeToContext(wrapper);
        return (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void applyThemeToContext(Context context) {
        Resources.Theme theme = context.getTheme();
        theme.applyStyle(getColorTheme().getResId(), true);
        theme.applyStyle(getAccentTheme().getResId(), true);
    }

    public Theme getBaseTheme() {
        return getBaseTheme(preferences.getInt(R.string.p_theme, 0));
    }

    public Theme getColorTheme() {
        return getColor(preferences.getInt(R.string.p_theme_color, 0));
    }

    public Theme getAccentTheme() {
        return getAccent(preferences.getInt(R.string.p_theme_accent, 1));
    }

    public Theme getBaseTheme(int themeIndex) {
        return new Theme(context, themeIndex, THEMES[themeIndex], themeNames[themeIndex]);
    }

    public Theme getColor(int themeIndex) {
        return new Theme(context, themeIndex, COLORS[themeIndex], colorNames[themeIndex]);
    }

    public Theme getAccent(int accentIndex) {
        return new Theme(context, accentIndex, ACCENTS[accentIndex], accentNames[accentIndex]);
    }

    public Theme getWidgetTheme(int widgetId) {
        return getBaseTheme(preferences.getInt(WidgetConfigActivity.PREF_THEME + widgetId, 0));
    }

    public Theme getWidgetColor(int widgetId) {
        return getColor(preferences.getInt(WidgetConfigActivity.PREF_COLOR + widgetId, 0));
    }

    public int getDialogThemeResId() {
        return getBaseTheme().getDialogThemeResId();
    }
}
