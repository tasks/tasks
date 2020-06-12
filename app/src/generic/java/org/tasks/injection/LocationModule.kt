package org.tasks.injection

import android.app.Application
import dagger.Module
import dagger.Provides
import org.tasks.location.MapFragment
import org.tasks.location.MapboxMapFragment
import org.tasks.location.MapboxSearchProvider
import org.tasks.location.PlaceSearchProvider

@Module
class LocationModule {
    @Provides
    @ActivityScope
    fun getPlaceSearchProvider(context: Application): PlaceSearchProvider {
        return MapboxSearchProvider(context)
    }

    @Provides
    @ActivityScope
    fun getMapFragment(context: Application): MapFragment {
        return MapboxMapFragment(context)
    }
}