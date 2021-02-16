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
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.gtasks.PlayServices
import org.tasks.location.*
import org.tasks.preferences.Preferences

@Module
@InstallIn(ActivityComponent::class, ViewModelComponent::class)
internal class LocationModule {
    @Provides
    @ViewModelScoped
    fun getPlaceSearchProvider(
            @ApplicationContext context: Context,
            preferences: Preferences,
            playServices: PlayServices,
            inventory: Inventory,
            mapboxSearchProvider: MapboxSearchProvider
    ): PlaceSearchProvider {
        return if (preferences.useGooglePlaces()
                && playServices.isPlayServicesAvailable
                && inventory.hasPro) {
            GooglePlacesSearchProvider(context)
        } else {
            mapboxSearchProvider
        }
    }

    @Provides
    @ActivityScoped
    fun getLocationProvider(@ApplicationContext context: Context): LocationProvider =
            PlayLocationProvider(context)

    @Provides
    @ActivityScoped
    fun getMapFragment(
            preferences: Preferences,
            @ApplicationContext context: Context
    ): MapFragment = when (preferences.getIntegerFromString(R.string.p_map_tiles, 0)) {
        1 -> OsmMapFragment(context)
        else -> GoogleMapFragment(context)
    }
}
