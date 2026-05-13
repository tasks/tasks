package org.tasks.preferences.fragments

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.analytics.Reporting
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.dao.CaldavDao
import org.tasks.jobs.BackgroundWork
import org.tasks.security.KeyStoreEncryption
import org.tasks.service.TaskDeleter
import org.tasks.viewmodel.CaldavAccountSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class CaldavAccountSettingsHiltViewModel @Inject constructor(
    caldavDao: CaldavDao,
    caldavClientProvider: CaldavClientProvider,
    encryption: KeyStoreEncryption,
    taskDeleter: TaskDeleter,
    backgroundWork: BackgroundWork,
    reporting: Reporting,
) : CaldavAccountSettingsViewModel(
    caldavDao = caldavDao,
    caldavClientProvider = caldavClientProvider,
    encryption = encryption,
    taskDeleter = taskDeleter,
    backgroundWork = backgroundWork,
    reporting = reporting,
)
