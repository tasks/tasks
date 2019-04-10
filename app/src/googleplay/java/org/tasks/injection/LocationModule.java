package org.tasks.injection;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import org.tasks.billing.Inventory;
import org.tasks.gtasks.PlayServices;
import org.tasks.location.GoogleMapFragment;
import org.tasks.location.GooglePlacesSearchProvider;
import org.tasks.location.MapFragment;
import org.tasks.location.MapboxMapFragment;
import org.tasks.location.MapboxSearchProvider;
import org.tasks.location.PlaceSearchProvider;
import org.tasks.preferences.Preferences;

@Module
public class LocationModule {

  @Provides
  @ActivityScope
  public PlaceSearchProvider getPlaceSearchProvider(
      @ForApplication Context context,
      Preferences preferences,
      PlayServices playServices,
      Inventory inventory) {
    return preferences.useGooglePlaces()
            && playServices.isPlayServicesAvailable()
            && inventory.hasPro()
        ? new GooglePlacesSearchProvider(context)
        : new MapboxSearchProvider(context);
  }

  @Provides
  @ActivityScope
  public MapFragment getMapFragment(@ForApplication Context context, Preferences preferences) {
    return preferences.useGoogleMaps()
        ? new GoogleMapFragment(context)
        : new MapboxMapFragment(context);
  }
}
