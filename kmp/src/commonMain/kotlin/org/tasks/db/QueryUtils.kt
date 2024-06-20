package org.tasks.db

import java.util.regex.Pattern

object QueryUtils {
    private val HIDDEN = "tasks\\.hideUntil<=?\\(strftime\\('%s','now'\\)\\*1000\\)".toPattern()
    private val UNCOMPLETED = "tasks\\.completed<?=0".toPattern()
    private val ORDER = "order by .*? (asc|desc)".toPattern(Pattern.CASE_INSENSITIVE)

    fun showHidden(query: String): String = HIDDEN.matcher(query).replaceAll("1")

    fun showCompleted(query: String): String = UNCOMPLETED.matcher(query).replaceAll("1")

    fun showHiddenAndCompleted(query: String): String = showCompleted(showHidden(query))

    fun removeOrder(query: String): String = ORDER.matcher(query).replaceAll("")
}