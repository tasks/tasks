package org.tasks.analytics

import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Suppress("UNUSED_PARAMETER")
class Firebase @Inject constructor(
    private val preferences: Preferences
) {
    fun reportException(t: Throwable) = Timber.e(t)

    fun updateRemoteConfig() {}

    fun logEvent(event: Int, vararg params: Pair<Int, Any>) {}

    fun addTask(source: String) {}

    fun completeTask(source: String) {}

    val subscribeCooldown: Boolean
        get() = installCooldown
                || preferences.lastSubscribeRequest + days(28L) > currentTimeMillis()

    val nameYourPrice = false

    private val installCooldown: Boolean
        get() = preferences.installDate + days(7L) > currentTimeMillis()

    private fun days(default: Long): Long =
        TimeUnit.DAYS.toMillis(default)
}