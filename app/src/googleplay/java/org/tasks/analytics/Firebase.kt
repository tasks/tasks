package org.tasks.analytics

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import com.android.billingclient.api.BillingClient.BillingResponse
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import io.fabric.sdk.android.Fabric
import org.tasks.R
import org.tasks.billing.BillingClientImpl
import org.tasks.injection.ApplicationScope
import org.tasks.injection.ApplicationContext
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ApplicationScope
class Firebase @Inject constructor(@param:ApplicationContext val context: Context, preferences: Preferences) {

    private var enabled: Boolean = preferences.isTrackingEnabled
    private var analytics: FirebaseAnalytics? = null
    private var remoteConfig: FirebaseRemoteConfig? = null

    fun reportException(t: Throwable) {
        Timber.e(t)
        if (enabled) {
            Crashlytics.logException(t)
        }
    }

    fun reportIabResult(@BillingResponse response: Int, sku: String?) {
        if (!enabled) {
            return
        }
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, sku)
        bundle.putString(FirebaseAnalytics.Param.SUCCESS, BillingClientImpl.BillingResponseToString(response))
        analytics?.logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, bundle)
    }

    fun updateRemoteConfig() {
        if (!enabled) {
            return
        }
        remoteConfig?.fetchAndActivate()?.addOnSuccessListener {
            Timber.d(it.toString())
        }
    }

    fun logEvent(@StringRes event: Int, vararg p: Pair<Int, Any>) {
        val bundle = Bundle()
        p.forEach {
            val key = context.getString(it.first)
            when (it.second::class) {
                String::class -> bundle.putString(key, it.second as String)
                Boolean::class -> bundle.putBoolean(key, it.second as Boolean)
                else -> Timber.e("Unhandled param: $it")
            }
        }
        analytics?.logEvent(context.getString(event), bundle)
    }

    fun noChurn(): Boolean = remoteConfig?.getBoolean("no_churn") ?: false

    init {
        if (enabled) {
            analytics = FirebaseAnalytics.getInstance(context)
            analytics?.setAnalyticsCollectionEnabled(true)
            Fabric.with(context, Crashlytics())
            remoteConfig = FirebaseRemoteConfig.getInstance()
            remoteConfig?.setConfigSettingsAsync(remoteConfigSettings {
                minimumFetchIntervalInSeconds =
                        TimeUnit.HOURS.toSeconds(WorkManager.REMOTE_CONFIG_INTERVAL_HOURS)
            })
            remoteConfig?.setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }
}