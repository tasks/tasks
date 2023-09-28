package org.tasks.ui

import kotlinx.coroutines.flow.MutableSharedFlow

typealias MainActivityEventBus = MutableSharedFlow<MainActivityEvent>

sealed interface MainActivityEvent {
    data object ClearTaskEditFragment : MainActivityEvent
}

