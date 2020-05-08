package org.tasks.injection

import android.app.Application
import org.tasks.locale.Locale

abstract class InjectingApplication : Application() {
    lateinit var component: ApplicationComponent

    override fun onCreate() {
        super.onCreate()
        val context = Locale.getInstance(this).createConfigurationContext(applicationContext)
        component = Dagger.Companion[context].applicationComponent
        inject(component)
    }

    protected abstract fun inject(component: ApplicationComponent)
}