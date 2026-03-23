package org.tasks

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.tasks.di.commonModule
import org.tasks.di.platformModule

class TasksApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TasksApplication)
            modules(commonModule, platformModule())
        }
    }
}
