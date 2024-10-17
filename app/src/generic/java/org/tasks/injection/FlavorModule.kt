package org.tasks.injection

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