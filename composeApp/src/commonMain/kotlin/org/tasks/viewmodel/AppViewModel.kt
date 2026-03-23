package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tasks.data.dao.CaldavDao
import org.tasks.data.newLocalAccount
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

class AppViewModel(
    private val caldavDao: CaldavDao,
    private val syncAdapters: SyncAdapters,
) : ViewModel() {

    val hasAccount = caldavDao
        .watchAccountExists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            syncAdapters.sync(SyncSource.APP_RESUME)
        }
    }

    fun continueWithoutSync() {
        viewModelScope.launch {
            caldavDao.newLocalAccount()
        }
    }
}
