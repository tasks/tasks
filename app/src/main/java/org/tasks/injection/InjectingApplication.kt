package org.tasks.injection

import android.app.Application

abstract class InjectingApplication : Application() {
    lateinit var component: ApplicationComponent

    override fun onCreate() {
        super.onCreate()
        component = Dagger.Companion[this].applicationComponent
        inject(component)
    }

    protected abstract fun inject(component: ApplicationComponent)
}