package org.tasks.injection

import org.tasks.PlatformConfiguration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tasks.location.Geocoder
import org.tasks.location.GeocoderNominatim
import org.tasks.location.LocationService
import org.tasks.location.LocationServiceAndroid
import org.tasks.location.MapFragment
import org.tasks.location.OsmMapFragment
import org.tasks.wear.WearRefresher

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    fun getPlatformConfiguration() = PlatformConfiguration(
        supportsTasksOrg = true,
        supportsCaldav = true,
        supportsGoogleTasks = true,
        supportsMicrosoft = true,
        supportsOpenTasks = true,
        supportsEteSync = true,
        supportsBackupImport = true,
        supportsGeofences = true,
        supportsCalendarEvents = true,
        isLibre = true,
        supportsWidgets = true,
    )

    @Provides
    fun getLocationService(service: LocationServiceAndroid): LocationService = service

    @Provides
    fun getMapFragment(osm: OsmMapFragment): MapFragment = osm

    @Provides
    fun getGeocoder(nominatim: GeocoderNominatim): Geocoder = nominatim

    @Provides
    fun getWearRefresher(): WearRefresher = object : WearRefresher {
        override suspend fun refresh() = Unit
    }
}