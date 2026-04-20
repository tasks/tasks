package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.jobs.BackgroundWork
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

open class AddAccountViewModel(
    caldavDao: CaldavDao,
    private val purchaseState: PurchaseState,
    private val syncAdapters: SyncAdapters,
    private val backgroundWork: BackgroundWork,
) : ViewModel() {

    val hasTasksAccount: Boolean get() = purchaseState.hasTasksAccount

    val hasPro: Boolean get() = purchaseState.hasPro

    private val _accountAdded = MutableSharedFlow<Unit>()
    val accountAdded: SharedFlow<Unit> = _accountAdded

    init {
        val accountCount = caldavDao.watchAccounts().map { it.size }
        viewModelScope.launch {
            val initialCount = accountCount.first()
            accountCount.drop(1).first { it > initialCount }
            syncAdapters.sync(SyncSource.ACCOUNT_ADDED)
            backgroundWork.updateBackgroundSync()
            _accountAdded.emit(Unit)
        }
    }
}
