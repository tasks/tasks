package org.tasks.analytics

import co.touchlab.kermit.Logger
import com.posthog.PostHog
import com.posthog.PostHogConfig
import org.tasks.TasksBuildConfig
import java.io.File
import java.util.Locale
import java.util.TimeZone

class PostHogReporting(
    apiKey: String,
    dataDir: File,
) : Reporting {
    private val logger = Logger.withTag("PostHogReporting")

    private val enabled: Boolean = apiKey.isNotBlank().also { enabled ->
        if (enabled) {
            val config = PostHogConfig(
                apiKey = apiKey,
                host = "https://us.i.posthog.com",
            ).apply {
                preloadFeatureFlags = false
                sendFeatureFlagEvent = false
                remoteConfig = false
                storagePrefix = File(dataDir, "posthog").absolutePath
            }
            PostHog.setup(config)
            PostHog.register("\$lib", "posthog")
            PostHog.register("\$lib_version", config.sdkVersion)
            PostHog.register("\$app_name", "Tasks")
            PostHog.register("\$app_namespace", "org.tasks")
            PostHog.register("\$app_version", TasksBuildConfig.VERSION_NAME)
            PostHog.register("\$app_build", TasksBuildConfig.VERSION_CODE)
            System.getProperty("os.name")?.let {
                PostHog.register("\$os_name", it)
            }
            System.getProperty("os.version")?.let {
                PostHog.register("\$os_version", it)
            }
            System.getProperty("os.arch")?.let {
                PostHog.register("arch", it)
            }
            with(Locale.getDefault()) {
                PostHog.register("\$locale", "$language-$country")
            }
            PostHog.register("\$timezone", TimeZone.getDefault().id)
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
