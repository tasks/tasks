package org.tasks.analytics

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Firebase @Inject constructor(
        @param:ApplicationContext val context: Context,
        preferences: Preferences
) {

    private var crashlytics: FirebaseCrashlytics? = null
    private var analytics: FirebaseAnalytics? = null

    fun reportException(t: Throwable) {
        Timber.e(t)
        crashlytics?.recordException(t)
    }

    fun reportIabResult(result: String, sku: String, state: String) {
        logEvent(
            R.string.event_purchase_result,
            R.string.param_sku to sku,
            R.string.param_result to result,
            R.string.param_state to state,
        )
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

    init {
        if (preferences.isTrackingEnabled) {
            analytics = FirebaseAnalytics.getInstance(context).apply {
                setAnalyticsCollectionEnabled(true)
            }
            crashlytics = FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(true)
            }
        }
    }
}