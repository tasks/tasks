package org.tasks.analytics

import timber.log.Timber
import javax.inject.Inject

@Suppress("UNUSED_PARAMETER")
class Firebase @Inject constructor() {
    fun reportException(t: Throwable) = Timber.e(t)

    fun updateRemoteConfig() {}

    fun logEvent(event: Int, vararg params: Pair<Int, Any>) {}

    fun addTask(source: String) {}

    fun completeTask(source: String) {}

    val subscribeCooldown = false
    val moreOptionsBadge = false
    val moreOptionsSolid = false
}