package org.tasks.activities

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.todoroo.andlib.utility.AndroidUtilities
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.Executors

interface DragAndDropDiffer<T, R> : ListUpdateCallback {
    val flow: MutableSharedFlow<R>
    val updates: Queue<Pair<R, DiffUtil.DiffResult?>>
    var items: R
    var dragging: Boolean
    val scope: CoroutineScope

    fun submitList(list: List<T>) {
        scope.launch {
            val transform = transform(list)
            flow.emit(transform)
        }
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
        flow
            .scan(Pair(initial, null), { last: Pair<R, DiffUtil.DiffResult?>, next: R ->
                calculateDiff(last, next)
            })
            .drop(1)
            .flowOn(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
            .onEach {
                withContext(Dispatchers.Main) {
                    applyDiff(it)
                }
            }
            .launchIn(scope)
        return initial
    }

    fun transform(list: List<T>): R

    fun diff(last: R, next: R): DiffUtil.DiffResult

    fun dispose() {
        scope.cancel()
    }
}