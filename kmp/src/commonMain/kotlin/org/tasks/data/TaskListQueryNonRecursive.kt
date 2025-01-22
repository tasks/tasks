package org.tasks.data

import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.core.SortHelper
import org.tasks.data.entity.Tag
import org.tasks.data.entity.Task
import org.tasks.data.sql.Field.Companion.field
import org.tasks.data.sql.Join
import org.tasks.data.sql.Query
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.Filter
import org.tasks.filters.RecentlyModifiedFilter
import org.tasks.preferences.QueryPreferences

internal object TaskListQueryNonRecursive {
    private const val TAGS_METADATA_JOIN = "for_tags"

    private val JOIN_TAGS = Task.ID.eq(field("$TAGS_METADATA_JOIN.task"))
    private val JOINS = """
        ${Join.left(Tag.TABLE.`as`(TAGS_METADATA_JOIN), JOIN_TAGS)}
        ${TaskListQuery.JOINS}
    """.trimIndent()
    private val TAGS =
            field("group_concat(distinct($TAGS_METADATA_JOIN.tag_uid))")
                    .`as`("tags")
    private val FIELDS =
        TaskListQuery.FIELDS.plus(listOf(
            TAGS,
            field("tasks.completed > 0").`as`("parentComplete")
        )).toTypedArray()

    fun getNonRecursiveQuery(filter: Filter, preferences: QueryPreferences): MutableList<String> {
        val joinedQuery = JOINS + if (filter is AstridOrderingFilter) filter.getSqlQuery() else filter.sql!!
        val sortMode = preferences.sortMode
        val groupMode = preferences.groupMode
        val sortGroup = field(SortHelper.getSortGroup(groupMode) ?: "NULL").`as`("sortGroup")
        val query = SortHelper.adjustQueryForFlagsAndSort(preferences, joinedQuery, sortMode)
        val completeAtBottom = if (preferences.completedTasksAtBottom) "parentComplete ASC," else ""
        val completionSort = if (preferences.completedTasksAtBottom) {
                "tasks.completed DESC,"
            } else {
                ""
            }
        val orderBy = "$completeAtBottom $completionSort"
        val groupedQuery = when {
            filter is RecentlyModifiedFilter ->
                query.replace("ORDER BY", "GROUP BY ${Task.ID} ORDER BY")
            query.contains("ORDER BY") ->
                query.replace("ORDER BY", "GROUP BY ${Task.ID} ORDER BY $orderBy")
            preferences.completedTasksAtBottom ->
                "$query GROUP BY ${Task.ID} ORDER BY $orderBy"
            else ->
                "$query GROUP BY ${Task.ID}"
        }
        return mutableListOf(
                Query.select(*FIELDS.plus(sortGroup))
                        .withQueryTemplate(PermaSql.replacePlaceholdersForQuery(groupedQuery))
                        .from(Task.TABLE)
                        .toString())
    }
}
