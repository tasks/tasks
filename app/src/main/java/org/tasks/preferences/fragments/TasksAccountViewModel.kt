package org.tasks.preferences.fragments

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.analytics.Firebase
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.PrincipalDao
import org.tasks.fcm.PushTokenManager
import org.tasks.jobs.BackgroundWork
import org.tasks.preferences.TasksPreferences
import org.tasks.service.TaskDeleter
import javax.inject.Inject

@HiltViewModel
class TasksAccountViewModel @Inject constructor(
    provider: CaldavClientProvider,
    firebase: Firebase,
    accountDataRepository: TasksAccountDataRepository,
    caldavDao: CaldavDao,
    principalDao: PrincipalDao,
    backgroundWork: BackgroundWork,
    pushTokenManager: PushTokenManager,
    taskDeleter: TaskDeleter,
    tasksPreferences: TasksPreferences,
    subscriptionProvider: SubscriptionProvider,
    environment: TasksServerEnvironment,
) : org.tasks.viewmodel.TasksAccountViewModel(
    provider = provider,
    reporting = firebase,
    accountDataRepository = accountDataRepository,
    caldavDao = caldavDao,
    principalDao = principalDao,
    backgroundWork = backgroundWork,
    pushTokenManager = pushTokenManager,
    taskDeleter = taskDeleter,
    tasksPreferences = tasksPreferences,
    subscriptionProvider = subscriptionProvider,
    caldavUrl = environment.caldavUrl,
)
