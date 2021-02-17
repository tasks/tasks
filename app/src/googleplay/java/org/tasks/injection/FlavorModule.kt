package org.tasks.injection

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tasks.location.LocationService
import org.tasks.location.LocationServiceGooglePlay

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    fun getLocationService(service: LocationServiceGooglePlay): LocationService = service
}