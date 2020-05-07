package org.tasks.injection

import dagger.Module

@Module(includes = [ProductionModule::class])
internal class ContentProviderModule