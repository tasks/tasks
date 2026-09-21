package org.tasks.preferences.fragments

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.jobs.BackgroundWork
import org.tasks.service.TaskDeleter
import javax.inject.Inject

@HiltViewModel
class LocalAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    caldavDao: CaldavDao,
    taskDeleter: TaskDeleter,
    backgroundWork: BackgroundWork,
    purchaseState: PurchaseState,
) : org.tasks.viewmodel.LocalAccountViewModel(
    caldavDao = caldavDao,
    taskDeleter = taskDeleter,
    backgroundWork = backgroundWork,
    purchaseState = purchaseState,
) {
    init {
        savedStateHandle.get<String>(KEY_DISPLAY_NAME)?.let {
            displayName.value = it
        }
    }

    override fun setDisplayName(name: String) {
        super.setDisplayName(name)
        savedStateHandle[KEY_DISPLAY_NAME] = name
    }

    companion object {
        private const val KEY_DISPLAY_NAME = "display_name"
    }
}
