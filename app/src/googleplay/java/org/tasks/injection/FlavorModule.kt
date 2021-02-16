package org.tasks.injection

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tasks.location.*

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    fun getGeocoder(geocoder: GeocoderMapbox): Geocoder = geocoder

    @Provides
    fun getGeofencing(geofencing: GoogleGeofencing): Geofencing = geofencing
}