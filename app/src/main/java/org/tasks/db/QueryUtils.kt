package org.tasks.db

import java.util.regex.Pattern

object QueryUtils {
    private val HIDDEN = Pattern.compile("tasks\\.hideUntil<=?\\(strftime\\('%s','now'\\)\\*1000\\)")
    private val UNCOMPLETED = Pattern.compile("tasks\\.completed<?=0")
    private val ORDER = Pattern.compile("order by .*? (asc|desc)", Pattern.CASE_INSENSITIVE)

    @JvmStatic
    fun showHidden(query: String): String = HIDDEN.matcher(query).replaceAll("1")

    @JvmStatic
    fun showCompleted(query: String): String = UNCOMPLETED.matcher(query).replaceAll("1")

    @JvmStatic
    fun showHiddenAndCompleted(query: String): String = showCompleted(showHidden(query))

    fun removeOrder(query: String): String = ORDER.matcher(query).replaceAll("")
}