package org.tasks.analytics

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import com.android.billingclient.api.BillingClient.BillingResponse
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.billing.BillingClientImpl
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Firebase @Inject constructor(
        @param:ApplicationContext val context: Context,
        preferences: Preferences
) {

    private var crashlytics: FirebaseCrashlytics? = null
    private var analytics: FirebaseAnalytics? = null
    private var remoteConfig: FirebaseRemoteConfig? = null

    fun reportException(t: Throwable) {
        Timber.e(t)
        crashlytics?.recordException(t)
    }

    fun reportIabResult(@BillingResponse response: Int, sku: String?) {
        analytics?.logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, sku)
            putString(FirebaseAnalytics.Param.SUCCESS, BillingClientImpl.BillingResponseToString(response))
        })
    }

    fun updateRemoteConfig() {
        remoteConfig?.fetchAndActivate()?.addOnSuccessListener {
            Timber.d(it.toString())
        }
    }

    fun logEvent(@StringRes event: Int, vararg p: Pair<Int, Any>) {
        analytics?.logEvent(context.getString(event), Bundle().apply {
            p.forEach {
                val key = context.getString(it.first)
                when (it.second::class) {
                    String::class -> putString(key, it.second as String)
                    Boolean::class -> putBoolean(key, it.second as Boolean)
                    else -> Timber.e("Unhandled param: $it")
                }
            }
        })
    }

    fun noChurn(): Boolean = remoteConfig?.getBoolean("no_churn") ?: false

    fun averageSubscription(): Double = remoteConfig?.getDouble("avg_sub") ?: 4.01

    init {
        if (preferences.isTrackingEnabled) {
            analytics = FirebaseAnalytics.getInstance(context).apply {
                setAnalyticsCollectionEnabled(true)
            }
            crashlytics = FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(true)
            }
            remoteConfig = FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(remoteConfigSettings {
                    minimumFetchIntervalInSeconds =
                            TimeUnit.HOURS.toSeconds(WorkManager.REMOTE_CONFIG_INTERVAL_HOURS)
                })
                setDefaultsAsync(R.xml.remote_config_defaults)
            }
        }
    }
}