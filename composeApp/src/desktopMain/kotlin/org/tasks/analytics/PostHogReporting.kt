package org.tasks.analytics

import co.touchlab.kermit.Logger
import com.posthog.PostHog
import com.posthog.PostHogConfig
import java.io.File

class PostHogReporting(apiKey: String, dataDir: File) : Reporting {
    private val logger = Logger.withTag("PostHogReporting")

    private val enabled: Boolean = apiKey.isNotBlank().also { enabled ->
        if (enabled) {
            PostHog.setup(
                PostHogConfig(
                    apiKey = apiKey,
                    host = "https://us.i.posthog.com",
                ).apply {
                    preloadFeatureFlags = false
                    sendFeatureFlagEvent = false
                    storagePrefix = File(dataDir, "posthog").absolutePath
                }
            )
        }
    }

    override fun logEvent(event: String, vararg params: Pair<String, Any>) {
        val properties = params.toMap()
        logger.d { "$event -> $properties" }
        if (enabled) {
            PostHog.capture(
                event = event,
                properties = properties,
            )
        }
    }

    override fun addTask(source: String) =
        logEvent(AnalyticsEvents.ADD_TASK, AnalyticsEvents.PARAM_TYPE to source)

    override fun completeTask(source: String) =
        logEvent(AnalyticsEvents.COMPLETE_TASK, AnalyticsEvents.PARAM_TYPE to source)

    override fun reportException(t: Throwable, fatal: Boolean) {
        logger.e(t) { t.message ?: "" }
        if (enabled) {
            PostHog.capture(
                event = "exception",
                properties = mapOf(
                    "message" to (t.message ?: ""),
                    "class" to t.javaClass.name,
                    "stacktrace" to t.stackTraceToString(),
                    "fatal" to fatal,
                ),
            )
        }
    }

    fun close() {
        if (enabled) {
            PostHog.close()
        }
    }
}
