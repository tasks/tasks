package org.tasks.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.analytics.Reporting
import org.tasks.data.dao.CaldavDao
import org.tasks.preferences.TasksPreferences
import javax.inject.Inject

@HiltViewModel
class SubscriptionOnboardingHiltViewModel @Inject constructor(
    caldavDao: CaldavDao,
    tasksPreferences: TasksPreferences,
    reporting: Reporting,
) : SubscriptionOnboardingViewModel(caldavDao, tasksPreferences, reporting)
