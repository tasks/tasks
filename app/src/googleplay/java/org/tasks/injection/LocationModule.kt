package org.tasks.injection

import android.app.Application
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
            context: Application,
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
    fun getMapFragment(context: Application, preferences: Preferences): MapFragment {
        return if (preferences.useGoogleMaps()) {
            GoogleMapFragment(context)
        } else {
            MapboxMapFragment(context)
        }
    }
}