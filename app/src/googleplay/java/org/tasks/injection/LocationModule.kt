package org.tasks.injection

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import org.tasks.R
import org.tasks.location.*
import org.tasks.preferences.Preferences

@Module
@InstallIn(ActivityComponent::class)
internal class LocationModule {
    @Provides
    @ActivityScoped
    fun getLocationProvider(@ApplicationContext context: Context): LocationProvider =
            PlayLocationProvider(context)

    @Provides
    @ActivityScoped
    fun getMapFragment(
            preferences: Preferences,
            @ApplicationContext context: Context
    ): MapFragment = when (preferences.getIntegerFromString(R.string.p_map_tiles, 0)) {
        1 -> OsmMapFragment(context)
        else -> GoogleMapFragment(context)
    }
}
