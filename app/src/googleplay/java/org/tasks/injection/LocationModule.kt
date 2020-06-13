package org.tasks.injection

import android.content.Context
import dagger.Module
import dagger.Provides
import org.tasks.billing.Inventory
import org.tasks.gtasks.PlayServices
import org.tasks.location.*
import org.tasks.preferences.Preferences

@Module
internal class LocationModule {
    @Provides
    @ActivityScope
    fun getPlaceSearchProvider(
            @ApplicationContext context: Context,
            preferences: Preferences,
            playServices: PlayServices,
            inventory: Inventory): PlaceSearchProvider {
        return if (preferences.useGooglePlaces()
                && playServices.isPlayServicesAvailable
                && inventory.hasPro()) {
            GooglePlacesSearchProvider(context)
        } else {
            MapboxSearchProvider(context)
        }
    }

    @Provides
    @ActivityScope
    fun getMapFragment(@ApplicationContext context: Context, preferences: Preferences): MapFragment {
        return if (preferences.useGoogleMaps()) {
            GoogleMapFragment(context)
        } else {
            MapboxMapFragment(context)
        }
    }
}