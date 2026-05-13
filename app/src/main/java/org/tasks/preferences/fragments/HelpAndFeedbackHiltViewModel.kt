package org.tasks.preferences.fragments

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.PlatformConfiguration
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.preferences.Preferences
import org.tasks.preferences.TasksPreferences
import org.tasks.viewmodel.HelpAndFeedbackViewModel
import javax.inject.Inject

@HiltViewModel
class HelpAndFeedbackHiltViewModel @Inject constructor(
    reporting: Reporting,
    tasksPreferences: TasksPreferences,
    platformConfiguration: PlatformConfiguration,
    purchaseState: PurchaseState,
    preferences: Preferences,
) : HelpAndFeedbackViewModel(
    reporting = reporting,
    tasksPreferences = tasksPreferences,
    platformConfiguration = platformConfiguration,
    purchaseState = purchaseState,
    collectStatistics = preferences.isTrackingEnabled,
)
