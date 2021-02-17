package org.tasks.injection

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tasks.location.LocationService
import org.tasks.location.LocationServiceAndroid
import org.tasks.location.MapFragment
import org.tasks.location.OsmMapFragment

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    fun getLocationService(service: LocationServiceAndroid): LocationService = service

    @Provides
    fun getMapFragment(osm: OsmMapFragment): MapFragment = osm
}