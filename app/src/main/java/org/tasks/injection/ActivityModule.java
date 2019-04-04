package org.tasks.injection;

import android.app.Activity;
import android.content.Context;
import dagger.Module;
import dagger.Provides;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.gtasks.PlayServices;
import org.tasks.location.GoogleMapFragment;
import org.tasks.location.GooglePlacesSearchProvider;
import org.tasks.location.MapFragment;
import org.tasks.location.MapboxMapFragment;
import org.tasks.location.MapboxSearchProvider;
import org.tasks.location.PlaceSearchProvider;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeBase;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

@Module
public class ActivityModule {

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
  public Context getActivityContext() {
    return activity;
  }

  @Provides
  @ActivityScope
  public ThemeBase getThemeBase(ThemeCache themeCache, Preferences preferences) {
    return themeCache.getThemeBase(preferences.getInt(R.string.p_theme, 0));
  }

  @Provides
  @ActivityScope
  public ThemeColor getThemeColor(ThemeCache themeCache, Preferences preferences) {
    return themeCache.getThemeColor(preferences.getInt(R.string.p_theme_color, 7));
  }

  @Provides
  @ActivityScope
  public ThemeAccent getThemeAccent(ThemeCache themeCache, Preferences preferences) {
    return themeCache.getThemeAccent(preferences.getInt(R.string.p_theme_accent, 1));
  }

  @Provides
  @ActivityScope
  public PlaceSearchProvider getPlaceSearchProvider(
      Preferences preferences, Inventory inventory, PlayServices playServices) {
    return preferences.useGooglePlaces()
            && playServices.isPlayServicesAvailable()
            && inventory.hasPro()
        ? new GooglePlacesSearchProvider(activity)
        : new MapboxSearchProvider(activity);
  }

  @Provides
  @ActivityScope
  public MapFragment getMapFragment(Preferences preferences) {
    return preferences.useGoogleMaps()
        ? new GoogleMapFragment(activity)
        : new MapboxMapFragment(activity);
  }
}
