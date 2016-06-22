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

    private final Context context;
    private final Preferences preferences;
    private final String[] themeNames;
    private final String[] colorNames;
    private final String[] accentNames;

    @Inject
    public ThemeManager(@ForApplication Context context, Preferences preferences) {
        this.context = context;
        this.preferences = preferences;
        themeNames = context.getResources().getStringArray(R.array.themes);
        colorNames = context.getResources().getStringArray(R.array.colors);
        accentNames = context.getResources().getStringArray(R.array.accents);
    }

    public boolean isDarkTheme() {
        return preferences.getInt(R.string.p_theme, 0) > 0;
    }

    public LayoutInflater getThemedLayoutInflater() {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(context, getBaseTheme().getResId());
        Resources.Theme theme = wrapper.getTheme();
        theme.applyStyle(getColorTheme().getResId(), true);
        theme.applyStyle(getAccentTheme().getResId(), true);
        return (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        return new Theme(context, themeIndex, getThemeResId(themeIndex), themeNames[themeIndex]);
    }

    public Theme getColor(int themeIndex) {
        return new Theme(context, themeIndex, getColorResId(themeIndex), colorNames[themeIndex]);
    }

    public Theme getAccent(int accentIndex) {
        return new Theme(context, accentIndex, getAccentResId(accentIndex), accentNames[accentIndex]);
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

    private int getThemeResId(int index) {
        switch (index) {
            case 1:
                return R.style.BaseBlack;
            case 2:
                return R.style.DarkOverride;
            default:
                return R.style.LightOverride;
        }
    }

    private int getColorResId(int index) {
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

    private int getAccentResId(int index) {
        switch (index) {
            case 1:
                return R.style.RedAccent;
            case 2:
                return R.style.PinkAccent;
            case 3:
                return R.style.PurpleAccent;
            case 4:
                return R.style.DeepPurpleAccent;
            case 5:
                return R.style.IndigoAccent;
            case 6:
                return R.style.BlueAccent;
            case 7:
                return R.style.LightBlueAccent;
            case 8:
                return R.style.CyanAccent;
            case 9:
                return R.style.TealAccent;
            case 10:
                return R.style.GreenAccent;
            case 11:
                return R.style.LightGreenAccent;
            case 12:
                return R.style.LimeAccent;
            case 13:
                return R.style.YellowAccent;
            case 14:
                return R.style.AmberAccent;
            case 15:
                return R.style.OrangeAccent;
            case 16:
                return R.style.DeepOrangeAccent;
            case 0:
            default:
                return R.style.BlueGreyAccent;
        }
    }
}
