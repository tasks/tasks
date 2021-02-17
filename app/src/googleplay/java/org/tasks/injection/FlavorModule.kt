package org.tasks.injection

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tasks.gtasks.PlayServices
import org.tasks.location.*

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    fun getLocationService(
            @ApplicationContext context: Context,
            google: Lazy<LocationServiceGooglePlay>,
            android: Lazy<LocationServiceAndroid>
    ): LocationService = if (PlayServices.isAvailable(context)) google.get() else android.get()

    @Provides
    fun getMapFragment(
            @ApplicationContext context: Context,
            osm: Lazy<OsmMapFragment>,
            google: Lazy<GoogleMapFragment>,
    ): MapFragment = if (PlayServices.isAvailable(context)) google.get() else osm.get()

    @Provides
    fun getGeocoder(mapbox: GeocoderMapbox): Geocoder = mapbox
}