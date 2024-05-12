package org.tasks.dialogs

import androidx.lifecycle.ViewModel
import org.tasks.data.entity.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PriorityPickerViewModel @Inject constructor(): ViewModel() {

    private val _priority = MutableStateFlow(Task.Priority.NONE)
    val priority = _priority.asStateFlow()

    fun setPriority(priority: Int) {
        _priority.value = priority
    }

}
