package org.tasks.injection

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.android.scopes.ViewModelScoped
import org.tasks.location.*

@Module
@InstallIn(ActivityComponent::class, ViewModelComponent::class)
class LocationModule {
    @Provides
    @ViewModelScoped
    fun getPlaceSearchProvider(mapboxSearchProvider: PlaceSearchMapbox): PlaceSearch =
            mapboxSearchProvider

    @Provides
    @ActivityScoped
    fun getLocationProvider(provider: AndroidLocationProvider): LocationProvider = provider

    @Provides
    @ActivityScoped
    fun getMapFragment(@ApplicationContext context: Context): MapFragment {
        return OsmMapFragment(context)
    }
}