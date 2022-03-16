package org.tasks.ui

import kotlinx.coroutines.flow.MutableSharedFlow

typealias TaskEditEventBus = MutableSharedFlow<TaskEditEvent>

sealed interface TaskEditEvent {
    data class Discard(val id: Long) : TaskEditEvent
}