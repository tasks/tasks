package org.tasks

import android.app.Application
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import timber.log.Timber

class WatchApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val posthogKey = getString(R.string.posthog_key)
        if (posthogKey.isNotBlank()) {
            PostHogAndroid.setup(
                this,
                PostHogAndroidConfig(
                    apiKey = posthogKey,
                    host = POSTHOG_HOST,
                )
            )
        }
    }

    companion object {
        private const val POSTHOG_HOST = "https://us.i.posthog.com"
    }
}
