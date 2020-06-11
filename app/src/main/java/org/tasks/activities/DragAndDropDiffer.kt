package org.tasks.activities

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.todoroo.andlib.utility.AndroidUtilities
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.*

interface DragAndDropDiffer<T, R> : ListUpdateCallback {
    val publishSubject: PublishSubject<R>
    val updates: Queue<Pair<R, DiffUtil.DiffResult?>>
    val disposables: CompositeDisposable
    var items: R
    var dragging: Boolean

    fun submitList(list: List<T>) {
        disposables.add(
                Single.fromCallable { transform(list) }
                        .subscribeOn(Schedulers.computation())
                        .subscribe(publishSubject::onNext))
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

    fun initializeDiffer(list: List<T>): R {
        val initial = transform(list)
        disposables.add(publishSubject
                .observeOn(Schedulers.computation())
                .scan(Pair(initial, null), { last: Pair<R, DiffUtil.DiffResult?>, next: R ->
                    calculateDiff(last, next)
                })
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::applyDiff))
        return initial
    }

    fun transform(list: List<T>): R

    fun diff(last: R, next: R): DiffUtil.DiffResult
}