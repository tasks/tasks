package org.tasks.ui

import kotlinx.coroutines.flow.MutableSharedFlow

typealias TaskListEventBus = MutableSharedFlow<TaskListEvent>

sealed interface TaskListEvent {
    data class TaskCreated(val uuid: String) : TaskListEvent
    data class CalendarEventCreated(val title: String?, val uri: String) : TaskListEvent
}
