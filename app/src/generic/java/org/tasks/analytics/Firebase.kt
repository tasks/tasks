package org.tasks.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Suppress("UNUSED_PARAMETER")
class Firebase @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val preferences: Preferences
) {
    fun reportException(t: Throwable) = Timber.e(t)

    fun updateRemoteConfig() {}

    fun logEvent(event: Int, vararg params: Pair<Int, Any>) {
        Timber.d("${context.getString(event)} -> $params")
    }

    fun addTask(source: String) =
        logEvent(R.string.event_add_task, R.string.param_type to source)

    fun completeTask(source: String) =
        logEvent(R.string.event_complete_task, R.string.param_type to source)

    val subscribeCooldown: Boolean
        get() = installCooldown
                || preferences.lastSubscribeRequest + days(28L) > currentTimeMillis()

    private val installCooldown: Boolean
        get() = preferences.installDate + days(7L) > currentTimeMillis()

    private fun days(default: Long): Long =
        TimeUnit.DAYS.toMillis(default)
}
