package org.tasks.db

import com.todoroo.andlib.sql.Criterion.Companion.or
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
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

    @JvmStatic
    fun showRecentlyCompleted(query: String): String = UNCOMPLETED
            .matcher(query)
            .replaceAll(
                    or(
                            Task.COMPLETION_DATE.lte(0),
                            Task.COMPLETION_DATE.gte(DateUtilities.now() - 59999))
                            .toString())

    fun removeOrder(query: String): String = ORDER.matcher(query).replaceAll("")
}