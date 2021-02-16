package org.tasks.injection

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tasks.location.Geofencing
import org.tasks.location.GoogleGeofencing

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    fun getGeofencing(geofencing: GoogleGeofencing): Geofencing = geofencing
}