package org.tasks.db

object QueryUtils {
    private val HIDDEN = "tasks\\.hideUntil<=?\\(strftime\\('%s','now'\\)\\*1000\\)".toRegex()
    private val UNCOMPLETED = "tasks\\.completed<?=0".toRegex()
    private val ORDER = "order by .*? (asc|desc)".toRegex(RegexOption.IGNORE_CASE)

    fun showHidden(query: String): String = HIDDEN.replace(query, "1")

    fun showCompleted(query: String): String = UNCOMPLETED.replace(query, "1")

    fun showHiddenAndCompleted(query: String): String = showCompleted(showHidden(query))

    fun removeOrder(query: String): String = ORDER.replace(query, "")
}
