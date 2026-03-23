package org.tasks.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.tasks.data.db.Database
import org.tasks.viewmodel.AddAccountViewModel
import org.tasks.viewmodel.AppViewModel

val commonModule = module {
    single { get<Database>().caldavDao() }
    single { get<Database>().taskDao() }
    single { get<Database>().tagDataDao() }
    single { get<Database>().tagDao() }
    single { get<Database>().alarmDao() }
    single { get<Database>().locationDao() }
    single { get<Database>().filterDao() }
    single { get<Database>().notificationDao() }
    single { get<Database>().googleTaskDao() }
    single { get<Database>().deletionDao() }
    single { get<Database>().contentProviderDao() }
    single { get<Database>().upgraderDao() }
    single { get<Database>().principalDao() }
    single { get<Database>().completionDao() }
    single { get<Database>().userActivityDao() }
    single { get<Database>().taskAttachmentDao() }
    single { get<Database>().taskListMetadataDao() }
    viewModel { AppViewModel(get()) }
    viewModel { AddAccountViewModel(get()) }
}

expect fun platformModule(): Module
