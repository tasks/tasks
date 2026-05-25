package org.tasks.broadcast

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.shareIn
import org.tasks.compose.throttleLatest

class ComposeRefreshBroadcaster(scope: CoroutineScope) : RefreshBroadcaster {
    private val refreshChannel = Channel<Unit>(Channel.CONFLATED)
    val refreshes: SharedFlow<Unit> = refreshChannel
        .consumeAsFlow()
        .throttleLatest(1000)
        .shareIn(scope, SharingStarted.Eagerly)

    override fun broadcastRefresh() {
        refreshChannel.trySend(Unit)
    }
}
