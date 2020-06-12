package org.tasks.injection

import android.app.Application
import org.tasks.locale.Locale

internal class Dagger private constructor(context: Application) {
    val applicationComponent: ApplicationComponent =
            DaggerApplicationComponent.builder()
                    .applicationModule(ApplicationModule(context))
                    .productionModule(ProductionModule())
                    .build()

    companion object {
        private val lock = Any()
        private var instance: Dagger? = null
        operator fun get(context: Application): Dagger {
            if (instance == null) {
                synchronized(lock) {
                    if (instance == null) {
                        Locale.getInstance(context)
                        instance = Dagger(context)
                    }
                }
            }
            return instance!!
        }
    }
}