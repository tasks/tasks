package org.tasks.analytics

import co.touchlab.crashkios.crashlytics.CrashlyticsKotlin
import co.touchlab.crashkios.crashlytics.enableCrashlytics
import co.touchlab.crashkios.crashlytics.setCrashlyticsUnhandledExceptionHook
import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter
import kotlinx.coroutines.runBlocking
import org.tasks.preferences.TasksPreferences

@OptIn(ExperimentalKermitApi::class)
class IosReporting(
    override val tasksPreferences: TasksPreferences,
    crashlytics: Boolean,
    analytics: AnalyticsBridge?,
    posthogKey: String,
) : Reporting {
    private val logger = Logger.withTag("IosReporting")

    private val collectStatistics: Boolean =
        runBlocking { tasksPreferences.get(TasksPreferences.collectStatistics, true) }

    private val crashlyticsEnabled: Boolean = (crashlytics && collectStatistics).also { enabled ->
        if (crashlytics) {
            enableCrashlytics()
            CrashlyticsKotlin.setCollectionEnabled(enabled)
        }
        if (enabled) {
            setCrashlyticsUnhandledExceptionHook()
            Logger.addLogWriter(CrashlyticsLogWriter(minCrashSeverity = null))
        }
    }

    private val analytics: AnalyticsBridge? = analytics
        ?.takeIf { posthogKey.isNotBlank() && collectStatistics }
        ?.also { it.setup(posthogKey, POSTHOG_HOST) }

    override fun logEvent(event: String, vararg params: Pair<String, Any>) {
        val properties = params.toMap()
        logger.d { "$event -> $properties" }
        analytics?.capture(event, properties)
    }

    override fun addTask(source: String) =
        logEvent(AnalyticsEvents.ADD_TASK, AnalyticsEvents.PARAM_TYPE to source)

    override fun completeTask(source: String) =
        logEvent(AnalyticsEvents.COMPLETE_TASK, AnalyticsEvents.PARAM_TYPE to source)

    override fun identify(distinctId: String) {
        logger.d { "identify -> $distinctId" }
        analytics?.identify(distinctId)
    }

    override fun reportException(t: Throwable, fatal: Boolean) {
        logger.e(t) { t.message ?: "" }
        if (crashlyticsEnabled) {
            if (fatal) {
                CrashlyticsKotlin.sendFatalException(t)
            } else {
                CrashlyticsKotlin.sendHandledException(t)
            }
        }
    }

    companion object {
        private const val POSTHOG_HOST = "https://us.i.posthog.com"
    }
}
