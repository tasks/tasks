package org.tasks.caldav

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import timber.log.Timber

abstract class CaldavViewModel : ViewModel() {
    val error = MutableLiveData<Throwable?>()
    val inFlight = MutableLiveData(false)
    val finish = MutableLiveData<Intent>()

    protected suspend fun <T> doRequest(action: suspend () -> T): T? =
        withContext(NonCancellable) {
            if (inFlight.value == true) {
                return@withContext null
            }
            inFlight.value = true
            try {
                return@withContext action()
            } catch (e: Exception) {
                Timber.e(e)
                error.value = e
                return@withContext null
            } finally {
                inFlight.value = false
            }
        }
}