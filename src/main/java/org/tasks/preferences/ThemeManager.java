package org.tasks.preferences;

import android.content.Context;

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
    private final String[] accentNames;

    @Inject
    public ThemeManager(@ForApplication Context context, Preferences preferences) {
        this.context = context;
        this.preferences = preferences;
        themeNames = context.getResources().getStringArray(R.array.themes);
        accentNames = context.getResources().getStringArray(R.array.accents);
    }

    public Theme getAppTheme() {
        return getTheme(preferences.getInt(R.string.p_theme, 0));
    }

    public Theme getAccentColor() {
        return getAccent(preferences.getInt(R.string.p_theme_accent, 1));
    }

    public Theme getTheme(int themeIndex) {
        return new Theme(context, themeIndex, getThemeResId(themeIndex), themeNames[themeIndex]);
    }

    public Theme getAccent(int accentIndex) {
        return new Theme(context, accentIndex, getAccentResId(accentIndex), accentNames[accentIndex]);
    }

    public Theme getWidgetTheme(int widgetId) {
        int defaultTheme = preferences.useDarkWidgetTheme(widgetId) ? 1 : 0;
        return getTheme(preferences.getInt(WidgetConfigActivity.PREF_THEME + widgetId, defaultTheme));
    }

    public int getDialogThemeResId() {
        return getAppTheme().getDialogThemeResId();
    }

    private int getThemeResId(int index) {
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
