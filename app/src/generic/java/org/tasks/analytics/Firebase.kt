package org.tasks.analytics

import timber.log.Timber
import javax.inject.Inject

class Firebase @Inject constructor() {
    fun reportException(t: Throwable) = Timber.e(t)

    fun updateRemoteConfig() {}

    fun noChurn() = true

    fun logEvent(event: Int, vararg params: Pair<Int, Boolean>) {}
}