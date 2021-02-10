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
            inventory: Inventory): PlaceSearchProvider {
        return if (preferences.useGooglePlaces()
                && playServices.isPlayServicesAvailable
                && inventory.hasPro) {
            GooglePlacesSearchProvider(context)
        } else {
            MapboxSearchProvider(context)
        }
    }

    @Provides
    @ActivityScoped
    fun getLocationProvider(@ApplicationContext context: Context): LocationProvider =
            PlayLocationProvider(context)

    @Provides
    @ActivityScoped
    fun getMapFragment(@ApplicationContext context: Context): MapFragment =
            GoogleMapFragment(context)
}