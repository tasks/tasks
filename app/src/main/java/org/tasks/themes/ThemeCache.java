package org.tasks.themes;

import static androidx.core.content.ContextCompat.getColor;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

@ApplicationScope
public class ThemeCache {

  public static final String EXTRA_THEME_OVERRIDE = "extra_theme_override";

  private final List<ThemeBase> themes = new ArrayList<>();
  private final ThemeColor untaggedColor;
  private final Preferences preferences;
  private final Inventory inventory;
  private final Context context;

  @Inject
  public ThemeCache(Preferences preferences, Inventory inventory, @ForApplication Context context) {
    this.preferences = preferences;
    this.inventory = inventory;
    this.context = context;
    Resources resources = context.getResources();

    themes.add(
        new ThemeBase(
            context.getString(R.string.theme_light),
            0,
            getColor(context, android.R.color.white),
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
            getColor(context, android.R.color.white),
            AppCompatDelegate.MODE_NIGHT_AUTO));
    themes.add(
        new ThemeBase(
            context.getString(R.string.theme_system_default),
            5,
            getColor(context, android.R.color.white),
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    untaggedColor =
        new ThemeColor(context, getColor(context, R.color.tag_color_none_background));
  }

  public ThemeBase getThemeBase() {
    return getThemeBase(null);
  }

  public ThemeBase getThemeBase(@Nullable Intent intent) {
    if (intent != null && intent.hasExtra(EXTRA_THEME_OVERRIDE)) {
      return getThemeBase(intent.getIntExtra(EXTRA_THEME_OVERRIDE, 5));
    }
    ThemeBase themeBase = getThemeBase(preferences.getThemeBase());
    return themeBase.isFree() || inventory.purchasedThemes() ? themeBase : getThemeBase(5);
  }

  public ThemeBase getThemeBase(int index) {
    return themes.get(index);
  }

  public ThemeColor getLauncherColor(int index) {
    return new ThemeColor(
        context, index, ContextCompat.getColor(context, ThemeColor.LAUNCHER_COLORS[index]), false);
  }

  public ThemeColor getUntaggedColor() {
    return untaggedColor;
  }
}
