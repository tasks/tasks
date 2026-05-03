package org.tasks.analytics

import android.content.Context
import org.tasks.fcm.FcmTokenProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.preferences.Preferences
import org.tasks.viewmodel.TasksAccountViewModel.Companion.DEFAULT_TOS_VERSION
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Suppress("UNUSED_PARAMETER")
class Firebase @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val preferences: Preferences
) : Reporting, FcmTokenProvider {
    override fun reportException(t: Throwable, fatal: Boolean) = Timber.e(t)

    fun updateRemoteConfig() {}

    override fun logEvent(event: String, vararg params: Pair<String, Any>) {
        Timber.d("$event -> ${params.toMap()}")
    }

    fun logEvent(event: Int, vararg params: Pair<Int, Any>) {
        logEvent(
            event = context.getString(event),
            params = params.map { context.getString(it.first) to it.second }.toTypedArray()
        )
    }

    fun logEventOncePerDay(event: Int, vararg params: Pair<Int, Any>) {
        logEvent(event, *params)
    }

    override fun addTask(source: String) =
        logEvent(R.string.event_add_task, R.string.param_type to source)

    override fun completeTask(source: String) =
        logEvent(R.string.event_complete_task, R.string.param_type to source)

    override fun identify(distinctId: String) {
        Timber.d("identify -> $distinctId")
    }

    val subscribeCooldown: Boolean
        get() = installCooldown
                || preferences.lastSubscribeRequest + days(28L) > currentTimeMillis()

    private val installCooldown: Boolean
        get() = preferences.installDate + days(7L) > currentTimeMillis()

    private fun days(default: Long): Long =
        TimeUnit.DAYS.toMillis(default)

    fun getTosVersion(): Int = DEFAULT_TOS_VERSION

    fun registerPrefChangeListener() {}

    fun unregisterPrefChangeListener() {}

    override suspend fun getToken(): String? = null
}
