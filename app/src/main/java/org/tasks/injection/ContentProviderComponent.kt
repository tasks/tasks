package org.tasks.injection

import com.todoroo.astrid.provider.Astrid2TaskProvider
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ContentProviderModule::class])
interface ContentProviderComponent {
    fun inject(contentProvider: Astrid2TaskProvider)
}