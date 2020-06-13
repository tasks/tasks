package org.tasks.injection

import android.content.Context
import org.tasks.locale.Locale
import timber.log.Timber

internal class Dagger private constructor(context: Context) {
    val applicationComponent: ApplicationComponent

    companion object {
        private val lock = Any()
        private var instance: Dagger? = null
        operator fun get(context: Context): Dagger {
            if (instance == null) {
                synchronized(lock) {
                    if (instance == null) {
                        instance = Dagger(context)
                    }
                }
            }
            return instance!!
        }
    }

    init {
        var localeContext = context.applicationContext
        try {
            localeContext = Locale.getInstance(localeContext).createConfigurationContext(localeContext)
        } catch (e: Exception) {
            Timber.e(e)
        }
        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(localeContext))
                .productionModule(ProductionModule())
                .build()
    }
}