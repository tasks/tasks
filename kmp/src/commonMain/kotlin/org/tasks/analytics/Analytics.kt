package org.tasks.analytics

interface Analytics {
    fun logEvent(event: String, vararg params: Pair<String, Any>)
}
