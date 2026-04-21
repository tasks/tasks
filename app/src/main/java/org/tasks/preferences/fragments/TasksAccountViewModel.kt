package org.tasks.preferences.fragments

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.analytics.Firebase
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.SubscriptionProvider
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.PrincipalDao
import javax.inject.Inject

@HiltViewModel
class TasksAccountViewModel @Inject constructor(
    provider: CaldavClientProvider,
    firebase: Firebase,
    accountDataRepository: TasksAccountDataRepository,
    caldavDao: CaldavDao,
    principalDao: PrincipalDao,
    subscriptionProvider: SubscriptionProvider,
    environment: TasksServerEnvironment,
) : org.tasks.viewmodel.TasksAccountViewModel(
    provider = provider,
    reporting = firebase,
    accountDataRepository = accountDataRepository,
    caldavDao = caldavDao,
    principalDao = principalDao,
    subscriptionProvider = subscriptionProvider,
    caldavUrl = environment.caldavUrl,
)
