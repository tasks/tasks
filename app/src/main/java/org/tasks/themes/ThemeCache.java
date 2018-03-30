package org.tasks.themes;

import static android.support.v4.content.ContextCompat.getColor;
import static com.google.common.collect.ImmutableList.copyOf;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.app.AppCompatDelegate;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;

@ApplicationScope
public class ThemeCache {

  private final List<ThemeBase> themes = new ArrayList<>();
  private final List<ThemeColor> colors = new ArrayList<>();
  private final List<ThemeAccent> accents = new ArrayList<>();
  private final List<WidgetTheme> widgetThemes = new ArrayList<>();
  private final ThemeColor untaggedColor;

  @Inject
  public ThemeCache(@ForApplication Context context) {
    Resources resources = context.getResources();

    themes.add(
        new ThemeBase(
            context.getString(R.string.theme_light),
            0,
            getColor(context, R.color.grey_50),
            AppCompatDelegate.MODE_NIGHT_NO));
    themes.add(
        new ThemeBase(
            context.getString(R.string.theme_black),
            1,
            getColor(context, R.color.widget_background_black),
            AppCompatDelegate.MODE_NIGHT_YES));
    themes.add(
        new ThemeBase(
            context.getString(R.string.theme_dark),
            2,
            getColor(context, R.color.md_background_dark),
            AppCompatDelegate.MODE_NIGHT_YES));
    themes.add(
        new ThemeBase(
            context.getString(R.string.theme_wallpaper),
            3,
            getColor(context, R.color.black_38),
            AppCompatDelegate.MODE_NIGHT_YES));
    themes.add(
        new ThemeBase(
            context.getString(R.string.theme_day_night),
            4,
            getColor(context, R.color.grey_50),
            AppCompatDelegate.MODE_NIGHT_AUTO));

    String[] colorNames = resources.getStringArray(R.array.colors);
    for (int i = 0; i < ThemeColor.COLORS.length; i++) {
      Resources.Theme theme = new ContextThemeWrapper(context, ThemeColor.COLORS[i]).getTheme();
      colors.add(
          new ThemeColor(
              colorNames[i],
              i,
              resolveAttribute(theme, R.attr.colorPrimary),
              resolveAttribute(theme, R.attr.colorPrimaryDark),
              resolveAttribute(theme, R.attr.actionBarPrimaryText),
              resolveBoolean(theme, R.attr.dark_status_bar)));
    }
    String[] accentNames = resources.getStringArray(R.array.accents);
    for (int i = 0; i < ThemeAccent.ACCENTS.length; i++) {
      Resources.Theme theme = new ContextThemeWrapper(context, ThemeAccent.ACCENTS[i]).getTheme();
      accents.add(new ThemeAccent(accentNames[i], i, resolveAttribute(theme, R.attr.colorAccent)));
    }
    String[] widgetBackgroundNames = resources.getStringArray(R.array.widget_background);
    for (int i = 0; i < WidgetTheme.BACKGROUNDS.length; i++) {
      widgetThemes.add(
          new WidgetTheme(
              widgetBackgroundNames[i],
              i,
              getColor(context, WidgetTheme.BACKGROUNDS[i]),
              getColor(context, i == 0 ? R.color.black_87 : R.color.white_100),
              getColor(context, i == 0 ? R.color.black_54 : R.color.white_70)));
    }
    untaggedColor =
        new ThemeColor(
            null,
            19,
            getColor(context, R.color.tag_color_none_background),
            0,
            getColor(context, R.color.black_87),
            false);
  }

  private static int resolveAttribute(Resources.Theme theme, int attribute) {
    TypedValue typedValue = new TypedValue();
    theme.resolveAttribute(attribute, typedValue, true);
    return typedValue.data;
  }

  public WidgetTheme getWidgetTheme(int index) {
    return widgetThemes.get(index);
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

  private boolean resolveBoolean(Resources.Theme theme, int attribute) {
    TypedValue typedValue = new TypedValue();
    theme.resolveAttribute(attribute, typedValue, false);
    return typedValue.data != 0;
  }

  public List<ThemeAccent> getAccents() {
    return copyOf(accents);
  }

  public List<ThemeBase> getThemes() {
    return copyOf(themes);
  }

  public List<ThemeColor> getColors() {
    return copyOf(colors);
  }

  public List<WidgetTheme> getWidgetThemes() {
    return copyOf(widgetThemes);
  }
}
