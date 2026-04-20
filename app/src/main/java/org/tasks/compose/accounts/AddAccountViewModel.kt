package org.tasks.compose.accounts

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.jobs.BackgroundWork
import org.tasks.sync.SyncAdapters
import javax.inject.Inject

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    caldavDao: CaldavDao,
    purchaseState: PurchaseState,
    syncAdapters: SyncAdapters,
    backgroundWork: BackgroundWork,
) : org.tasks.viewmodel.AddAccountViewModel(caldavDao, purchaseState, syncAdapters, backgroundWork)
