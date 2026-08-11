package org.tasks.broadcast

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class ComposeRefreshBroadcaster : RefreshBroadcaster {
    private val _refreshes = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val refreshes: SharedFlow<Unit> = _refreshes

    override fun broadcastRefresh() {
        _refreshes.tryEmit(Unit)
    }
}
