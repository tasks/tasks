package org.tasks.injection

import com.google.android.material.color.DynamicColors
import com.todoroo.andlib.utility.AndroidUtilities
import org.tasks.AppStore
import org.tasks.PlatformConfiguration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tasks.billing.DesktopLinkService
import org.tasks.billing.QrScanner
import org.tasks.location.Geocoder
import org.tasks.location.GeocoderNominatim
import org.tasks.location.LocationService
import org.tasks.location.LocationServiceAndroid
import org.tasks.location.MapFragment
import org.tasks.location.OsmMapFragment
import org.tasks.wear.WearRefresher

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class FlavorModule {
    @Provides
    @Singleton
    fun providePlayBilling(
        billing: org.tasks.billing.NoOpPlayBilling,
    ): org.tasks.billing.PlayBilling = billing

    @Provides
    @Singleton
    fun providePlayPurchaseVerifier(): org.tasks.billing.PlayPurchaseVerifier =
        org.tasks.billing.PlayPurchaseVerifier { true }

    @Provides
    @Singleton
    fun providePlayPurchaseStore(
        store: org.tasks.billing.NoOpPlayPurchaseStore,
    ): org.tasks.billing.PlayPurchaseStore = store

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
        appStore = AppStore.FDROID,
        billingProvider = org.tasks.billing.BillingProvider.STRIPE,
        isAndroid = true,
        isLibre = true,
        supportsWidgets = true,
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
    fun getLocationService(service: LocationServiceAndroid): LocationService = service

    @Provides
    fun getMapFragment(osm: OsmMapFragment): MapFragment = osm

    @Provides
    fun getGeocoder(nominatim: GeocoderNominatim): Geocoder = nominatim

    @Provides
    fun getWearRefresher(): WearRefresher = object : WearRefresher {
        override suspend fun refresh() = Unit
    }

    @Provides
    fun getQrScanner(): QrScanner = object : QrScanner {
        override suspend fun scan(): String? = null
    }

    @Provides
    fun getDesktopLinkService(): DesktopLinkService = object : DesktopLinkService {
        override suspend fun confirmLink(code: String) = false
    }

    @Provides
    @Singleton
    fun provideStoreSubscriptionProvider(): org.tasks.billing.StoreSubscriptionProvider? = null
}
