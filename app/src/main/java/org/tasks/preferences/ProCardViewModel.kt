package org.tasks.preferences

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.PlatformConfiguration
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.data.dao.CaldavDao
import javax.inject.Inject

@HiltViewModel
class ProCardViewModel @Inject constructor(
    caldavDao: CaldavDao,
    subscriptionProvider: SubscriptionProvider,
    tasksPreferences: TasksPreferences,
    accountDataRepository: TasksAccountDataRepository,
    serverEnvironment: TasksServerEnvironment,
    platformConfiguration: PlatformConfiguration,
) : org.tasks.viewmodel.ProCardViewModel(
    caldavDao = caldavDao,
    subscriptionProvider = subscriptionProvider,
    tasksPreferences = tasksPreferences,
    accountDataRepository = accountDataRepository,
    serverEnvironment = serverEnvironment,
    platformConfiguration = platformConfiguration,
)
