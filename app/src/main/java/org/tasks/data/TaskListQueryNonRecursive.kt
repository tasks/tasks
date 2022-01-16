package org.tasks.data

import com.todoroo.andlib.sql.Field.Companion.field
import com.todoroo.andlib.sql.Join
import com.todoroo.andlib.sql.Query
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.core.SortHelper
import com.todoroo.astrid.data.Task
import org.tasks.filters.RecentlyModifiedFilter
import org.tasks.preferences.QueryPreferences

internal object TaskListQueryNonRecursive {
    private val JOIN_TAGS = Task.ID.eq(field("${TaskListFragment.TAGS_METADATA_JOIN}.task"))
    private val JOINS = """
        ${Join.left(Tag.TABLE.`as`(TaskListFragment.TAGS_METADATA_JOIN), JOIN_TAGS)}
        ${TaskListQuery.JOINS}
    """.trimIndent()
    private val TAGS =
            field("group_concat(distinct(${TaskListFragment.TAGS_METADATA_JOIN}.tag_uid))")
                    .`as`("tags")
    private const val ORDER_BY = "ORDER BY parentComplete ASC, tasks.completed DESC"
    private val FIELDS =
        TaskListQuery.FIELDS.plus(listOf(
            TAGS,
            field("tasks.completed > 0").`as`("parentComplete")
        )).toTypedArray()

    fun getNonRecursiveQuery(filter: Filter, preferences: QueryPreferences): MutableList<String> {
        val joinedQuery = JOINS + filter.getSqlQuery()
        val sortMode = preferences.sortMode
        val sortGroup = field(SortHelper.getSortGroup(sortMode) ?: "NULL").`as`("sortGroup")
        val query = SortHelper.adjustQueryForFlagsAndSort(preferences, joinedQuery, sortMode)
        val groupedQuery = when {
            filter is RecentlyModifiedFilter ->
                query.replace("ORDER BY", "GROUP BY ${Task.ID} ORDER BY")
            query.contains("ORDER BY") ->
                query.replace("ORDER BY", "GROUP BY ${Task.ID} $ORDER_BY,")
            else ->
                "$query GROUP BY ${Task.ID} $ORDER_BY"
        }
        return mutableListOf(
                Query.select(*FIELDS.plus(sortGroup))
                        .withQueryTemplate(PermaSql.replacePlaceholdersForQuery(groupedQuery))
                        .from(Task.TABLE)
                        .toString())
    }
}
