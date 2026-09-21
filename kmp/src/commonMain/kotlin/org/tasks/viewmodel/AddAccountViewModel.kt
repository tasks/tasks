package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao

open class AddAccountViewModel(
    caldavDao: CaldavDao,
    private val purchaseState: PurchaseState,
) : ViewModel() {

    val hasTasksAccount: Boolean get() = purchaseState.hasTasksAccount

    val hasPro: Boolean get() = purchaseState.hasPro

    private val _accountAdded = MutableSharedFlow<Unit>(replay = 1)
    val accountAdded: SharedFlow<Unit> = _accountAdded

    init {
        viewModelScope.launch {
            val initialCount = caldavDao.getAccounts().size
            caldavDao.watchAccounts()
                .map { it.size }
                .first { it > initialCount }
            _accountAdded.emit(Unit)
        }
    }
}
