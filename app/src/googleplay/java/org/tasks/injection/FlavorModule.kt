package org.tasks.injection

import android.content.Context
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.phone.PhoneDataLayerAppHelper
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import org.tasks.extensions.wearDataLayerRegistry
import org.tasks.location.Geocoder
import org.tasks.location.GeocoderMapbox
import org.tasks.location.GoogleMapFragment
import org.tasks.location.LocationService
import org.tasks.location.LocationServiceAndroid
import org.tasks.location.LocationServiceGooglePlay
import org.tasks.location.MapFragment
import org.tasks.location.OsmMapFragment
import org.tasks.play.PlayServices
import org.tasks.wear.WearRefresher
import org.tasks.wear.WearRefresherImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    fun getLocationService(
        google: Lazy<LocationServiceGooglePlay>,
        android: Lazy<LocationServiceAndroid>,
        playServices: PlayServices,
    ): LocationService = if (playServices.isAvailable()) google.get() else android.get()

    @Provides
    fun getMapFragment(
        osm: Lazy<OsmMapFragment>,
        google: Lazy<GoogleMapFragment>,
        playServices: PlayServices,
    ): MapFragment = if (playServices.isAvailable()) google.get() else osm.get()

    @Provides
    fun getGeocoder(mapbox: GeocoderMapbox): Geocoder = mapbox

    @OptIn(ExperimentalHorologistApi::class)
    @Provides
    fun wearDataLayerRegistry(
        @ApplicationContext applicationContext: Context,
        @ApplicationScope coroutineScope: CoroutineScope,
    ) = applicationContext.wearDataLayerRegistry(coroutineScope)

    @OptIn(ExperimentalHorologistApi::class)
    @Provides
    fun phoneDataLayerAppHelper(
        @ApplicationContext applicationContext: Context,
        wearDataLayerRegistry: WearDataLayerRegistry,
    ) = PhoneDataLayerAppHelper(
        context = applicationContext,
        registry = wearDataLayerRegistry,
    )

    @OptIn(ExperimentalHorologistApi::class)
    @Provides
    @Singleton
    fun getWearRefresher(
        phoneDataLayerAppHelper: PhoneDataLayerAppHelper,
        wearDataLayerRegistry: WearDataLayerRegistry,
        @ApplicationScope scope: CoroutineScope,
    ): WearRefresher = WearRefresherImpl(phoneDataLayerAppHelper, wearDataLayerRegistry, scope)
}