package org.tasks.preferences.fragments

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.analytics.Reporting
import org.tasks.data.dao.CaldavDao
import org.tasks.etebase.EtebaseClientProvider
import org.tasks.jobs.BackgroundWork
import org.tasks.security.KeyStoreEncryption
import org.tasks.service.TaskDeleter
import org.tasks.viewmodel.EtebaseAccountSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class EtebaseAccountSettingsHiltViewModel @Inject constructor(
    caldavDao: CaldavDao,
    clientProvider: EtebaseClientProvider,
    encryption: KeyStoreEncryption,
    taskDeleter: TaskDeleter,
    backgroundWork: BackgroundWork,
    reporting: Reporting,
) : EtebaseAccountSettingsViewModel(
    caldavDao = caldavDao,
    clientProvider = clientProvider,
    encryption = encryption,
    taskDeleter = taskDeleter,
    backgroundWork = backgroundWork,
    reporting = reporting,
)
