package org.tasks.analytics

import timber.log.Timber
import javax.inject.Inject

@Suppress("UNUSED_PARAMETER")
class Firebase @Inject constructor() {
    fun reportException(t: Throwable) = Timber.e(t)

    fun logEvent(event: Int, vararg params: Pair<Int, Any>) {}
}