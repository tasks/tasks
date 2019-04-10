package org.tasks.injection;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import org.tasks.location.MapFragment;
import org.tasks.location.MapboxMapFragment;
import org.tasks.location.MapboxSearchProvider;
import org.tasks.location.PlaceSearchProvider;

@Module
public class LocationModule {
  @Provides
  @ActivityScope
  public PlaceSearchProvider getPlaceSearchProvider(@ForApplication Context context) {
    return new MapboxSearchProvider(context);
  }

  @Provides
  @ActivityScope
  public MapFragment getMapFragment(@ForApplication Context context) {
    return new MapboxMapFragment(context);
  }
}
