package org.tasks.di

import org.koin.dsl.module
import org.tasks.PlatformConfiguration

val flavorModule = module {
    single {
        PlatformConfiguration(
            supportsBackupImport = false,
            isLibre = true,
        )
    }
}
