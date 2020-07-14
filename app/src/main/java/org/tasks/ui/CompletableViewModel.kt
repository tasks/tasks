package org.tasks.ui

import androidx.lifecycle.*
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

abstract class CompletableViewModel<T> : ViewModel() {
    private val data = MutableLiveData<T>()
    private val error = MutableLiveData<Throwable>()
    private val disposables = CompositeDisposable()

    var inProgress = false
        private set

    fun observe(
            lifecycleOwner: LifecycleOwner,
            dataObserver: suspend (T) -> Unit,
            errorObserver: (Throwable) -> Unit) {
        data.observe(lifecycleOwner) {
            lifecycleOwner.lifecycleScope.launch {
                dataObserver.invoke(it)
            }
        }
        error.observe(lifecycleOwner, errorObserver)
    }

    protected suspend fun run(callable: suspend () -> T) {
        if (!inProgress) {
            inProgress = true
            try {
                data.value = callable.invoke()
            } catch (e: Exception) {
                error.value = e
            }
            inProgress = false
        }
    }

    override fun onCleared() = disposables.dispose()

    fun removeObserver(owner: LifecycleOwner) {
        data.removeObservers(owner)
        error.removeObservers(owner)
    }
}