package org.tasks.ui

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class CompletableViewModel<T> : ViewModel() {
    private val data = MutableLiveData<T>()
    private val error = MutableLiveData<Throwable>()

    var inProgress = false
        private set

    fun observe(
            lifecycleOwner: LifecycleOwner,
            dataObserver: suspend (T) -> Unit,
            errorObserver: (Throwable) -> Unit) {
        data.observe(lifecycleOwner) {
            lifecycleOwner.lifecycleScope.launch {
                dataObserver(it)
            }
        }
        error.observe(lifecycleOwner, errorObserver)
    }

    protected suspend fun run(callable: suspend () -> T) {
        if (!inProgress) {
            inProgress = true
            try {
                data.postValue(callable())
            } catch (e: Exception) {
                Timber.e(e)
                error.postValue(e)
            }
            inProgress = false
        }
    }

    fun removeObserver(owner: LifecycleOwner) {
        data.removeObservers(owner)
        error.removeObservers(owner)
    }
}