package org.tasks.injection;

import android.app.Activity;
import android.content.Context;
import dagger.Module;
import dagger.Provides;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ColorProvider;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeBase;
import org.tasks.themes.ThemeColor;

@Module
class ActivityModule {

  private final Activity activity;

  public ActivityModule(Activity activity) {
    this.activity = activity;
  }

  @Provides
  public Activity getActivity() {
    return activity;
  }

  @Provides
  @ForActivity
  Context getActivityContext() {
    return activity;
  }

  @Provides
  @ActivityScope
  ThemeBase getThemeBase(Preferences preferences, Inventory inventory) {
    return ThemeBase.getThemeBase(preferences, inventory, activity.getIntent());
  }

  @Provides
  @ActivityScope
  public ThemeColor getThemeColor(ColorProvider colorProvider, Preferences preferences) {
    return colorProvider.getThemeColor(preferences.getDefaultThemeColor(), true);
  }

  @Provides
  @ActivityScope
  ThemeAccent getThemeAccent(ColorProvider colorProvider, Preferences preferences) {
    return colorProvider.getThemeAccent(preferences.getInt(R.string.p_theme_accent, 1));
  }
}
