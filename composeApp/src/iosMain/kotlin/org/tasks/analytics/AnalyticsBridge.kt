package org.tasks.analytics

interface AnalyticsBridge {
    fun setup(apiKey: String, host: String)
    fun capture(event: String, properties: Map<String, Any>)
    fun identify(distinctId: String)
}

internal var installedAnalytics: AnalyticsBridge? = null
    private set

fun installAnalytics(analytics: AnalyticsBridge) {
    installedAnalytics = analytics
}
