package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tasks.data.dao.CaldavDao
import org.tasks.data.newLocalAccount

class AppViewModel(
    private val caldavDao: CaldavDao,
) : ViewModel() {

    val hasAccount = caldavDao
        .watchAccountExists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun continueWithoutSync() {
        viewModelScope.launch {
            caldavDao.newLocalAccount()
        }
    }
}
