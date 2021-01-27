package org.tasks.injection

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.android.scopes.ViewModelScoped
import org.tasks.location.MapFragment
import org.tasks.location.MapboxMapFragment
import org.tasks.location.MapboxSearchProvider
import org.tasks.location.PlaceSearchProvider

@Module
@InstallIn(ActivityComponent::class, ViewModelComponent::class)
class LocationModule {
    @Provides
    @ViewModelScoped
    fun getPlaceSearchProvider(@ApplicationContext context: Context): PlaceSearchProvider {
        return MapboxSearchProvider(context)
    }

    @Provides
    @ActivityScoped
    fun getMapFragment(@ApplicationContext context: Context): MapFragment {
        return MapboxMapFragment(context)
    }
}