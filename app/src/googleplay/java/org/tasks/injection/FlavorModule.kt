package org.tasks.injection

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.google.android.material.color.DynamicColors
import com.todoroo.andlib.utility.AndroidUtilities
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
import kotlinx.serialization.json.Json
import org.tasks.AppStore
import org.tasks.PlatformConfiguration
import org.tasks.analytics.Firebase
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.BillingProvider
import org.tasks.billing.DesktopLinkService
import org.tasks.billing.DesktopLinkServiceImpl
import org.tasks.billing.GooglePlayQrScanner
import org.tasks.billing.HiltPlayStoreBillingProvider
import org.tasks.billing.PlayBilling
import org.tasks.billing.PlayBillingClient
import org.tasks.billing.PlayPurchaseStore
import org.tasks.billing.PlayPurchaseVerifier
import org.tasks.billing.PlayPurchases
import org.tasks.billing.PlaySignatureVerifier
import org.tasks.billing.PlaySubscriptionProvider
import org.tasks.billing.PreferencesPlayPurchaseStore
import org.tasks.billing.QrScanner
import org.tasks.billing.StoreBillingProvider
import org.tasks.billing.StoreSubscriptionProvider
import org.tasks.extensions.wearDataLayerRegistry
import org.tasks.http.HttpClientFactory
import org.tasks.jobs.WorkManager
import org.tasks.location.Geocoder
import org.tasks.location.GeocoderMapbox
import org.tasks.location.GoogleMapFragment
import org.tasks.location.LocationService
import org.tasks.location.LocationServiceAndroid
import org.tasks.location.LocationServiceGooglePlay
import org.tasks.location.MapFragment
import org.tasks.location.OsmMapFragment
import org.tasks.play.PlayServices
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.wear.WearRefresher
import org.tasks.wear.WearRefresherImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    @Singleton
    fun providePlayBilling(
        @ApplicationContext context: Context,
        purchases: PlayPurchases,
        firebase: Firebase,
        @ApplicationScope scope: CoroutineScope,
        workManager: WorkManager,
        syncAdapters: Lazy<SyncAdapters>,
    ): PlayBilling = PlayBillingClient(
        context = context,
        purchases = purchases,
        reporting = firebase,
        scope = scope,
        onPurchasesAdded = { syncAdapters.get().sync(SyncSource.PURCHASE_COMPLETED) },
        onPurchaseFlowFinished = { workManager.updatePurchases() },
    )

    @Provides
    @Singleton
    fun providePlayPurchaseVerifier(): PlayPurchaseVerifier = PlaySignatureVerifier()

    @Provides
    @Singleton
    fun providePlayPurchaseStore(store: PreferencesPlayPurchaseStore): PlayPurchaseStore = store

    @Provides
    fun getPlatformConfiguration() = PlatformConfiguration(
        supportsTasksOrg = true,
        supportsCaldav = true,
        supportsGoogleTasks = true,
        supportsMicrosoft = true,
        supportsOpenTasks = true,
        supportsEteSync = true,
        supportsBackupImport = true,
        supportsGeofences = true,
        supportsCalendarEvents = true,
        billingProvider = BillingProvider.GOOGLE_PLAY,
        appStore = AppStore.GOOGLE_PLAY,
        isAndroid = true,
        supportsWidgets = true,
        supportsDesktopLinking = true,
        supportsLogExport = true,
        supportsNotificationTroubleshooting = true,
        supportsSystemNotificationSettings = true,
        supportsOngoingNotifications = AndroidUtilities.preUpsideDownCake(),
        supportsBundledNotifications = true,
        supportsVoiceReminders = true,
        supportsCompletionSound = true,
        supportsSwipeToSnooze = true,
        supportsRingMode = true,
        supportsDynamicColor = DynamicColors.isDynamicColorAvailable(),
        supportsLauncherIcon = true,
        supportsMarkdownToggle = true,
        supportsWallpaperTheme = true,
        supportsAutoNightTheme = true,
        supportsLanguageSelection = true,
    )

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
        @ApplicationContext applicationContext: Context,
        phoneDataLayerAppHelper: PhoneDataLayerAppHelper,
        @ApplicationScope scope: CoroutineScope,
    ): WearRefresher = WearRefresherImpl(
        phoneDataLayerAppHelper,
        Wearable.getMessageClient(applicationContext),
        scope,
    )

    @Provides
    fun getQrScanner(@ApplicationContext context: Context): QrScanner =
        GooglePlayQrScanner(context)

    @Provides
    fun getDesktopLinkService(
        httpClientFactory: HttpClientFactory,
        serverEnvironment: TasksServerEnvironment,
        storeSubscriptionProvider: StoreSubscriptionProvider,
        json: Json,
    ): DesktopLinkService = DesktopLinkServiceImpl(
        httpClientFactory = httpClientFactory,
        serverEnvironment = serverEnvironment,
        subscriptionProvider = storeSubscriptionProvider,
        json = json,
    )

    @Provides
    @Singleton
    fun provideStoreSubscriptionProvider(
        playPurchases: PlayPurchases,
        billing: PlayBilling,
    ): StoreSubscriptionProvider = PlaySubscriptionProvider(playPurchases, billing)

    @Provides
    @Singleton
    fun provideStoreBillingProvider(): StoreBillingProvider? = HiltPlayStoreBillingProvider()
}
