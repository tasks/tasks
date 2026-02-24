package org.tasks.presentation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object RefreshTrigger {
    private val _flow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val flow = _flow.asSharedFlow()

    fun trigger() {
        _flow.tryEmit(Unit)
    }
}
