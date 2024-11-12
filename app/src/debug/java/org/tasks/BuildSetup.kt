package org.tasks

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import com.facebook.soloader.SoLoader
import com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo
import com.todoroo.andlib.utility.AndroidUtilities.atLeastQ
import leakcanary.AppWatcher
import org.tasks.logging.FileLogger
import org.tasks.preferences.Preferences
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

class BuildSetup @Inject constructor(
        private val context: Application,
        private val preferences: Preferences
) {
    fun setup() {
        Timber.plant(DebugTree())
        Timber.plant(FileLogger(context))
        SoLoader.init(context, false)
        if (preferences.getBoolean(R.string.p_leakcanary, false)) {
            AppWatcher.manualInstall(context)
        }
        if (preferences.getBoolean(R.string.p_flipper, false) && FlipperUtils.shouldEnableFlipper(context)) {
            val client = AndroidFlipperClient.getInstance(context)
            client.addPlugin(InspectorFlipperPlugin(context, DescriptorMapping.withDefaults()))
            client.addPlugin(DatabasesFlipperPlugin(context))
            client.addPlugin(NetworkFlipperPlugin())
            client.addPlugin(SharedPreferencesFlipperPlugin(context))
            client.start()
        }
        if (preferences.getBoolean(R.string.p_strict_mode_thread, false)) {
            val builder = StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog()
            if (preferences.getBoolean(R.string.p_crash_main_queries, false)) {
                builder.penaltyDeath()
            }
            StrictMode.setThreadPolicy(builder.build())
        }
        if (preferences.getBoolean(R.string.p_strict_mode_vm, false)) {
            val builder = VmPolicy.Builder()
                    .detectActivityLeaks()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedRegistrationObjects()
                    .detectLeakedClosableObjects()
                    .detectFileUriExposure()
                    .penaltyLog()
            if (atLeastOreo()) {
                builder.detectContentUriWithoutPermission()
            }
            if (atLeastQ()) {
                builder
                        .detectCredentialProtectedWhileLocked()
                        .detectImplicitDirectBoot()
            }
            StrictMode.setVmPolicy(builder.build())
        }
    }
}
