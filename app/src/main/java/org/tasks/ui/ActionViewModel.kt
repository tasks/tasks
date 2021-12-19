package org.tasks.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

open class ActionViewModel : ViewModel() {
    private val completed = MutableLiveData<Boolean>()
    private val error = MutableLiveData<Throwable>()

    var inProgress = false
        private set

    fun observe(
            lifecycleOwner: LifecycleOwner,
            completeObserver: suspend (Boolean) -> Any,
            errorObserver: (Throwable) -> Unit) {
        completed.observe(lifecycleOwner) {
            lifecycleOwner.lifecycleScope.launch {
                completeObserver(it)
            }
        }
        error.observe(lifecycleOwner, errorObserver)
    }

    protected suspend fun run(action: suspend () -> Unit) {
        if (!inProgress) {
            inProgress = true
            try {
                action()
                completed.value = true
            } catch (e: Exception) {
                error.value = e
            }
            inProgress = false
        }
    }
}