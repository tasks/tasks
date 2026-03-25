package org.tasks.analytics

interface Analytics {
    fun logEvent(event: String, vararg params: Pair<String, Any>)
    fun addTask(source: String)
    fun completeTask(source: String)
}
