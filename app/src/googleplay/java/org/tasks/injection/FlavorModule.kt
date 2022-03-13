package org.tasks.injection

import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tasks.location.Geocoder
import org.tasks.location.GeocoderMapbox
import org.tasks.location.GoogleMapFragment
import org.tasks.location.LocationService
import org.tasks.location.LocationServiceAndroid
import org.tasks.location.LocationServiceGooglePlay
import org.tasks.location.MapFragment
import org.tasks.location.OsmMapFragment
import org.tasks.play.PlayServices

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    fun getLocationService(
        google: Lazy<LocationServiceGooglePlay>,
        android: Lazy<LocationServiceAndroid>,
        playServices: PlayServices,
    ): LocationService = if (playServices.isAvailable()) google.get() else android.get()

    @Provides
    fun getMapFragment(
        osm: Lazy<OsmMapFragment>,
        google: Lazy<GoogleMapFragment>,
        playServices: PlayServices,
    ): MapFragment = if (playServices.isAvailable()) google.get() else osm.get()

    @Provides
    fun getGeocoder(mapbox: GeocoderMapbox): Geocoder = mapbox
}