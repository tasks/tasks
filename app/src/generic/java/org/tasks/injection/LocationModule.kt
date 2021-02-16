package org.tasks.injection

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import org.tasks.location.AndroidLocationProvider
import org.tasks.location.LocationProvider
import org.tasks.location.MapFragment
import org.tasks.location.OsmMapFragment

@Module
@InstallIn(ActivityComponent::class)
class LocationModule {
    @Provides
    @ActivityScoped
    fun getLocationProvider(provider: AndroidLocationProvider): LocationProvider = provider

    @Provides
    @ActivityScoped
    fun getMapFragment(@ApplicationContext context: Context): MapFragment {
        return OsmMapFragment(context)
    }
}