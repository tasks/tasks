package org.tasks.activities

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.todoroo.andlib.utility.AndroidUtilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import java.util.Queue

interface DragAndDropDiffer<T, R> : ListUpdateCallback {
    val channel: Channel<List<T>>
    val updates: Queue<Pair<R, DiffUtil.DiffResult?>>
    var items: R
    var dragging: Boolean
    val scope: CoroutineScope

    fun submitList(list: List<T>) {
        channel.trySend(list)
    }

    fun calculateDiff(last: Pair<R, DiffUtil.DiffResult?>, next: R): Pair<R, DiffUtil.DiffResult?> {
        AndroidUtilities.assertNotMainThread()
        return Pair(next, diff(last.first!!, next))
    }

    fun applyDiff(update: Pair<R, DiffUtil.DiffResult?>) {
        AndroidUtilities.assertMainThread()
        updates.add(update)
        if (!dragging) {
            drainQueue()
        }
    }

    fun drainQueue() {
        AndroidUtilities.assertMainThread()
        var update = updates.poll()
        while (update != null) {
            items = update.first
            update.second?.dispatchUpdatesTo(this as ListUpdateCallback)
            update = updates.poll()
        }
    }

    @ExperimentalCoroutinesApi
    fun initializeDiffer(list: List<T>): R {
        val initial = transform(list)
        channel
            .consumeAsFlow()
            .map { transform(it) }
            .scan(Pair(initial, null)) { last: Pair<R, DiffUtil.DiffResult?>, next: R ->
                calculateDiff(last, next)
            }
                .drop(1)
            .flowOn(Dispatchers.Default)
            .onEach { applyDiff(it) }
            .launchIn(CoroutineScope(Dispatchers.Main + Job()))
        return initial
    }

    fun transform(list: List<T>): R

    fun diff(last: R, next: R): DiffUtil.DiffResult

    fun dispose() {
        scope.cancel()
    }
}