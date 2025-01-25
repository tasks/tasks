package org.tasks.data

import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.core.SortHelper
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.db.Table
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Tag
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion
import org.tasks.data.sql.Field.Companion.field
import org.tasks.data.sql.Join
import org.tasks.data.sql.Query
import org.tasks.data.sql.QueryTemplate
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.preferences.QueryPreferences

internal object TaskListQueryRecursive {
    private val RECURSIVE = Table("recursive_tasks")
    private val RECURSIVE_TASK = field("$RECURSIVE.task")
    private val FIELDS =
            TaskListQuery.FIELDS.plus(listOf(
                    field("(${
                        Query.select(field("group_concat(distinct(tag_uid))")).from(Tag.TABLE).where(
                        Task.ID.eq(Tag.TASK))} GROUP BY ${Tag.TASK})").`as`("tags"),
                    field("indent"),
                    field("sort_group"),
                    field("children"),
                    field("primary_sort"),
                    field("secondary_sort"),
                    field("parent_complete"),
            )).toTypedArray()
    private val JOINS = """
        ${Join.inner(RECURSIVE, Task.ID.eq(RECURSIVE_TASK))}
        LEFT JOIN (SELECT parent, count(distinct recursive_tasks.task) AS children FROM recursive_tasks GROUP BY parent) AS recursive_children ON recursive_children.parent = tasks._id
        ${TaskListQuery.JOINS}
    """.trimIndent()
    private val SUBTASK_QUERY =
            QueryTemplate()
                    .join(Join.inner(RECURSIVE, Task.PARENT.eq(RECURSIVE_TASK)))
                    .where(activeAndVisible())

    fun getRecursiveQuery(
        filter: Filter,
        preferences: QueryPreferences,
    ): MutableList<String> {
        val parentQuery = when (filter) {
            is CaldavFilter -> newCaldavQuery(filter.uuid)
            else -> PermaSql.replacePlaceholdersForQuery(filter.sql!!)
        }
        val manualSort = preferences.isManualSort
        val groupPreference = preferences.groupMode
        val groupMode = when {
            filter is CaldavFilter && (manualSort || groupPreference == SortHelper.SORT_LIST) ->
                SortHelper.GROUP_NONE
            else -> groupPreference
        }
        val sortMode = when {
            !manualSort || filter !is CaldavFilter -> preferences.sortMode
            filter.isGoogleTasks -> SortHelper.SORT_GTASKS
            else -> SortHelper.SORT_CALDAV
        }
        val subtaskPreference = preferences.subtaskMode
        val subtaskMode = when {
            sortMode == SortHelper.SORT_GTASKS || sortMode == SortHelper.SORT_CALDAV -> sortMode
            subtaskPreference == SortHelper.SORT_MANUAL -> SortHelper.SORT_CALDAV
            else -> subtaskPreference
        }
        val completedMode = preferences.completedMode
        val groupAscending =
            preferences.groupAscending && groupMode != SortHelper.GROUP_NONE
        val sortAscending =
            preferences.sortAscending && sortMode != SortHelper.SORT_GTASKS && sortMode != SortHelper.SORT_CALDAV
        val subtaskAscending =
            preferences.subtaskAscending && subtaskMode != SortHelper.SORT_GTASKS && subtaskMode != SortHelper.SORT_CALDAV
        val primaryGroupSelector = SortHelper.orderSelectForSortTypeRecursive(groupMode, true)
        val primarySortSelect = SortHelper.orderSelectForSortTypeRecursive(sortMode, false)
        val subtaskSort = SortHelper.orderSelectForSortTypeRecursive(subtaskMode, false)
        val parentCompleted = if (preferences.completedTasksAtBottom) "tasks.completed > 0" else "0"
        val completionSort = if (preferences.completedTasksAtBottom) {
            "(CASE WHEN tasks.completed > 0 THEN ${SortHelper.orderSelectForSortTypeRecursive(completedMode, false)} ELSE 0 END)"
        } else {
            "0"
        }
        val withClause = """
            CREATE TEMPORARY TABLE `recursive_tasks` AS
            WITH RECURSIVE recursive_tasks (task, parent_complete, subtask_complete, completion_sort, parent, collapsed, hidden, indent, title, primary_group, primary_sort, secondary_sort, sort_group) AS (
                SELECT tasks._id, $parentCompleted as parent_complete, 0 as subtask_complete, $completionSort as completion_sort, 0 as parent, tasks.collapsed as collapsed, 0 as hidden, 0 AS sort_indent, UPPER(tasks.title) AS sort_title, $primaryGroupSelector as primary_group, $primarySortSelect as primary_sort, NULL as secondarySort, ${SortHelper.getSortGroup(groupMode)}
                FROM tasks
                ${
                    if (groupMode == SortHelper.SORT_LIST) {
                        """
                            INNER JOIN caldav_tasks on cd_task = tasks._id AND cd_deleted = 0
                            INNER JOIN caldav_lists on cd_calendar = cdl_uuid
                        """.trimIndent()
                    } else {
                        ""
                    }
                }
                $parentQuery
                UNION ALL SELECT tasks._id, recursive_tasks.parent_complete, $parentCompleted as subtask_complete, $completionSort as completion_sort, recursive_tasks.task as parent, tasks.collapsed as collapsed, CASE WHEN recursive_tasks.collapsed > 0 OR recursive_tasks.hidden > 0 THEN 1 ELSE 0 END as hidden, recursive_tasks.indent+1 AS sort_indent, UPPER(tasks.title) AS sort_title, recursive_tasks.primary_group as primary_group, recursive_tasks.primary_sort as primary_sort, $subtaskSort as secondary_sort, recursive_tasks.sort_group FROM tasks
                $SUBTASK_QUERY
                ORDER BY parent_complete ASC, sort_indent DESC, subtask_complete ASC, completion_sort ${if (preferences.completedAscending) "ASC" else "DESC"}, ${SortHelper.orderForGroupTypeRecursive(groupMode, groupAscending)}, ${SortHelper.orderForSortTypeRecursive(sortMode, sortAscending, subtaskMode, subtaskAscending)}
            ) SELECT * FROM recursive_tasks
            WHERE indent = (SELECT MAX(indent) FROM recursive_tasks as r WHERE r.task = recursive_tasks.task)
        """.trimIndent()

        return mutableListOf(
            "DROP TABLE IF EXISTS `recursive_tasks`",
            SortHelper.adjustQueryForFlags(preferences, withClause),
            "CREATE INDEX `r_tasks` ON `recursive_tasks` (`task`)",
            "CREATE INDEX `r_parents` ON `recursive_tasks` (`parent`)",
            Query.select(*FIELDS)
                .withQueryTemplate(PermaSql.replacePlaceholdersForQuery("$JOINS WHERE recursive_tasks.hidden = 0"))
                .from(Task.TABLE)
                .toString(),
        )
    }

    private fun newCaldavQuery(list: String) =
            QueryTemplate()
                    .join(
                        Join.inner(
                            CaldavTask.TABLE,
                            Criterion.and(
                                    CaldavTask.CALENDAR.eq(list),
                                    CaldavTask.TASK.eq(Task.ID),
                                    CaldavTask.DELETED.eq(0))))
                    .where(Criterion.and(activeAndVisible(), Task.PARENT.eq(0)))
                    .toString()
}
