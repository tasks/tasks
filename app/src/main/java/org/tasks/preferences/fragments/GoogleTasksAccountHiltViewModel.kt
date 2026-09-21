package org.tasks.preferences.fragments

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.service.TaskDeleter
import org.tasks.viewmodel.GoogleTasksAccountViewModel
import javax.inject.Inject

@HiltViewModel
class GoogleTasksAccountHiltViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    caldavDao: CaldavDao,
    taskDeleter: TaskDeleter,
) : GoogleTasksAccountViewModel(
    caldavDao = caldavDao,
    taskDeleter = taskDeleter,
) {
    init {
        val account: CaldavAccount = savedStateHandle[GoogleTasksAccount.EXTRA_ACCOUNT]!!
        setAccount(account)
    }
}
