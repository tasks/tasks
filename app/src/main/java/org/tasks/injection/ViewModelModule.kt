package org.tasks.injection

import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import org.tasks.billing.Inventory
import org.tasks.location.PlaceSearch
import org.tasks.location.PlaceSearchGoogle
import org.tasks.location.PlaceSearchMapbox

@Module
@InstallIn(ViewModelComponent::class)
class ViewModelModule {
    @Provides
    @ViewModelScoped
    fun getPlaceSearchProvider(
            inventory: Inventory,
            google: Lazy<PlaceSearchGoogle>,
            mapbox: Lazy<PlaceSearchMapbox>
    ): PlaceSearch = if (inventory.hasTasksAccount) google.get() else mapbox.get()
}