package org.tasks.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.observe
import com.todoroo.andlib.utility.AndroidUtilities
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

open class ActionViewModel : ViewModel() {
    private val completed = MutableLiveData<Boolean>()
    private val error = MutableLiveData<Throwable>()
    private val disposables = CompositeDisposable()

    var inProgress = false
        private set

    fun observe(
            lifecycleOwner: LifecycleOwner,
            completeObserver: (Boolean) -> Unit,
            errorObserver: (Throwable) -> Unit) {
        completed.observe(lifecycleOwner, completeObserver)
        error.observe(lifecycleOwner, errorObserver)
    }

    protected fun run(action: () -> Unit) {
        AndroidUtilities.assertMainThread()
        if (!inProgress) {
            inProgress = true
            disposables.add(
                    Completable.fromAction(action)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doFinally {
                                AndroidUtilities.assertMainThread()
                                inProgress = false
                            }
                            .subscribe({ completed.setValue(true) }) { value: Throwable -> error.setValue(value) })
        }
    }

    override fun onCleared() = disposables.clear()
}