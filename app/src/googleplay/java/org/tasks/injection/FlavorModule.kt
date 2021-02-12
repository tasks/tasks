package org.tasks.injection

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tasks.location.Geocoder
import org.tasks.location.Geofencing
import org.tasks.location.GoogleGeofencing
import org.tasks.location.MapboxGeocoder

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    fun getGeocoder(geocoder: MapboxGeocoder): Geocoder = geocoder

    @Provides
    fun getGeofencing(geofencing: GoogleGeofencing): Geofencing = geofencing
}