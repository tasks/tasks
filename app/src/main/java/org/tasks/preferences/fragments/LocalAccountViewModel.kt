package org.tasks.preferences.fragments

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.dao.CaldavDao
import org.tasks.service.TaskDeleter
import javax.inject.Inject

@HiltViewModel
class LocalAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    caldavDao: CaldavDao,
    taskDeleter: TaskDeleter,
) : org.tasks.viewmodel.LocalAccountViewModel(
    caldavDao = caldavDao,
    taskDeleter = taskDeleter,
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
