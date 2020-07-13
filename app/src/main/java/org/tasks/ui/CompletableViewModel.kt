package org.tasks.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.observe
import com.todoroo.andlib.utility.AndroidUtilities
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

abstract class CompletableViewModel<T> : ViewModel() {
    private val data = MutableLiveData<T>()
    private val error = MutableLiveData<Throwable>()
    private val disposables = CompositeDisposable()
    private var inProgress = false

    fun observe(
            lifecycleOwner: LifecycleOwner,
            dataObserver: (T) -> Unit,
            errorObserver: (Throwable) -> Unit) {
        data.observe(lifecycleOwner, dataObserver)
        error.observe(lifecycleOwner, errorObserver)
    }

    fun inProgress(): Boolean {
        return inProgress
    }

    protected fun run(callable: () -> T) {
        AndroidUtilities.assertMainThread()
        if (!inProgress) {
            inProgress = true
            disposables.add(
                    Single.fromCallable(callable)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doFinally {
                                AndroidUtilities.assertMainThread()
                                inProgress = false
                            }
                            .subscribe({ value: T -> data.setValue(value) }) { value: Throwable -> error.setValue(value) })
        }
    }

    override fun onCleared() {
        disposables.dispose()
    }

    fun removeObserver(owner: LifecycleOwner) {
        data.removeObservers(owner)
        error.removeObservers(owner)
    }
}