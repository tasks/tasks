package org.tasks.injection

import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tasks.gtasks.PlayServices
import org.tasks.location.*

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    fun getLocationService(service: LocationServiceGooglePlay): LocationService = service

    @Provides
    fun getMapFragment(
            playServices: PlayServices,
            osm: Lazy<OsmMapFragment>,
            google: Lazy<GoogleMapFragment>,
    ): MapFragment = if (playServices.isPlayServicesAvailable) google.get() else osm.get()
}