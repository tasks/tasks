package org.tasks.ui

import com.todoroo.astrid.data.Task
import kotlinx.coroutines.flow.MutableSharedFlow

typealias MainActivityEventBus = MutableSharedFlow<MainActivityEvent>

sealed interface MainActivityEvent {
    data class OpenTask(val task: Task) : MainActivityEvent
    object ClearTaskEditFragment : MainActivityEvent
}

