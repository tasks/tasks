package org.tasks.sync.todoist

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.analytics.Reporting
import org.tasks.data.dao.CaldavDao
import org.tasks.security.KeyStoreEncryption
import org.tasks.service.TaskDeleter
import org.tasks.viewmodel.TodoistAccountSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class TodoistAccountSettingsHiltViewModel @Inject constructor(
    caldavDao: CaldavDao,
    clientProvider: TodoistClientProvider,
    encryption: KeyStoreEncryption,
    taskDeleter: TaskDeleter,
    reporting: Reporting,
) : TodoistAccountSettingsViewModel(
    caldavDao = caldavDao,
    clientProvider = clientProvider,
    encryption = encryption,
    taskDeleter = taskDeleter,
    reporting = reporting,
)
