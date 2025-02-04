package org.tasks

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo
import com.todoroo.andlib.utility.AndroidUtilities.atLeastQ
import leakcanary.AppWatcher
import org.tasks.logging.FileLogger
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

class BuildSetup @Inject constructor(
    private val context: Application,
    private val preferences: Preferences,
    private val fileLogger: FileLogger,
) {
    fun setup() {
        Timber.plant(Timber.DebugTree())
        Timber.plant(fileLogger)
        if (preferences.getBoolean(R.string.p_leakcanary, false)) {
            AppWatcher.manualInstall(context)
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
