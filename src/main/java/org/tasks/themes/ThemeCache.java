package org.tasks.themes;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import org.tasks.R;

import java.util.ArrayList;
import java.util.List;

public class ThemeCache {

    private final List<ThemeBase> themes = new ArrayList<>();
    private final List<ThemeColor> colors = new ArrayList<>();
    private final List<ThemeAccent> accents = new ArrayList<>();
    private final ThemeColor untaggedColor;

    public ThemeCache(Context context) {
        Resources resources = context.getResources();
        String[] themeNames = resources.getStringArray(R.array.themes);
        for (int i = 0 ; i < ThemeBase.THEMES.length ; i++) {
            Resources.Theme theme = new ContextThemeWrapper(context, ThemeBase.THEMES[i]).getTheme();
            themes.add(new ThemeBase(
                    themeNames[i],
                    i,
                    resolveAttribute(theme, R.attr.alertDialogTheme),
                    resolveAttribute(theme, R.attr.asContentBackground),
                    resolveAttribute(theme, R.attr.asTextColor),
                    resolveAttribute(theme, android.R.attr.textColorPrimary),
                    resolveAttribute(theme, android.R.attr.textColorSecondary),
                    resolveAttribute(theme, android.R.attr.textColorTertiary)));
        }
        String[] colorNames = resources.getStringArray(R.array.colors);
        for (int i = 0 ; i < ThemeColor.COLORS.length ; i++) {
            Resources.Theme theme = new ContextThemeWrapper(context, ThemeColor.COLORS[i]).getTheme();
            colors.add(new ThemeColor(
                    colorNames[i],
                    i,
                    resolveAttribute(theme, R.attr.colorPrimary),
                    resolveAttribute(theme, R.attr.colorPrimaryDark),
                    resolveAttribute(theme, R.attr.actionBarPrimaryText),
                    resolveBoolean(theme, R.attr.dark_status_bar)));
        }
        String[] accentNames = resources.getStringArray(R.array.accents);
        for (int i = 0 ; i < ThemeAccent.ACCENTS.length ; i++) {
            Resources.Theme theme = new ContextThemeWrapper(context, ThemeAccent.ACCENTS[i]).getTheme();
            accents.add(new ThemeAccent(
                    accentNames[i],
                    i,
                    resolveAttribute(theme, R.attr.colorAccent)));
        }
        untaggedColor = new ThemeColor(null, 19, resources.getColor(R.color.tag_color_none_background), 0, resources.getColor(R.color.black_87), false);
    }

    public ThemeBase getThemeBase(int index) {
        return themes.get(index);
    }

    public ThemeColor getThemeColor(int index) {
        return colors.get(index);
    }

    public ThemeAccent getThemeAccent(int index) {
        return accents.get(index);
    }

    public ThemeColor getUntaggedColor() {
        return untaggedColor;
    }

    private static int resolveAttribute(Resources.Theme theme, int attribute) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attribute, typedValue, true);
        return typedValue.data;
    }

    private boolean resolveBoolean(Resources.Theme theme, int attribute) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attribute, typedValue, false);
        return typedValue.data != 0;
    }
}
