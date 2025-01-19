package org.tasks.data

import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.core.SortHelper
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.db.Table
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion
import org.tasks.data.sql.Field.Companion.field
import org.tasks.data.sql.Join
import org.tasks.data.sql.QueryTemplate
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.preferences.QueryPreferences

internal object TaskListQueryRecursive {
    private val RECURSIVE = Table("recursive_tasks")
    private val RECURSIVE_TASK = field("$RECURSIVE.task")
    private val SUBTASK_QUERY =
            QueryTemplate()
                .join(Join.inner(RECURSIVE, Task.PARENT.eq(RECURSIVE_TASK)))
                .where(activeAndVisible())
                .toString()

    // TODO: switch to datastore, reading from preferences is expensive (30+ ms)
    fun getRecursiveQuery(
        filter: Filter,
        preferences: QueryPreferences,
    ): String {
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
        val completedAtBottom = preferences.completedTasksAtBottom
        val parentCompleted = if (completedAtBottom) "tasks.completed > 0" else "0"
        val completionSort = if (completedAtBottom) {
            "(CASE WHEN tasks.completed > 0 THEN ${SortHelper.orderSelectForSortTypeRecursive(completedMode, false)} ELSE 0 END)"
        } else {
            "0"
        }
        val query = """
            WITH RECURSIVE recursive_tasks AS (
                SELECT 
                    tasks._id AS task,
                    $parentCompleted AS parent_complete,
                    0 AS subtask_complete,
                    $completionSort AS completion_sort,
                    0 AS parent,
                    tasks.collapsed AS collapsed,
                    0 AS hidden,
                    0 AS indent,
                    UPPER(tasks.title) AS sort_title,
                    ${SortHelper.orderSelectForSortTypeRecursive(groupMode, true)} AS primary_group,
                    ${SortHelper.orderSelectForSortTypeRecursive(sortMode, false)} AS primary_sort,
                    NULL as secondary_sort,
                    ${SortHelper.getSortGroup(groupMode)} AS sort_group
                FROM tasks
                ${
                    if (groupMode == SortHelper.SORT_LIST) {
                        """
                            INNER JOIN caldav_tasks ON cd_task = tasks._id AND cd_deleted = 0
                            INNER JOIN caldav_lists ON cd_calendar = cdl_uuid
                        """.trimIndent()
                    } else {
                        ""
                    }
                }
                $parentQuery
                UNION ALL SELECT
                    tasks._id AS task,
                    recursive_tasks.parent_complete AS parent_complete,
                    $parentCompleted AS subtask_complete,
                    $completionSort AS completion_sort,
                    recursive_tasks.task AS parent,
                    tasks.collapsed AS collapsed,
                    CASE WHEN recursive_tasks.collapsed > 0 OR recursive_tasks.hidden > 0 THEN 1 ELSE 0 END AS hidden,
                    recursive_tasks.indent+1 AS indent,
                    UPPER(tasks.title) AS sort_title,
                    recursive_tasks.primary_group AS primary_group,
                    recursive_tasks.primary_sort AS primary_sort,
                    ${SortHelper.orderSelectForSortTypeRecursive(subtaskMode, false)} AS secondary_sort,
                    recursive_tasks.sort_group AS sort_group
                FROM tasks
                $SUBTASK_QUERY
                ORDER BY
                    parent_complete,
                    indent DESC,
                    subtask_complete,
                    completion_sort ${if (preferences.completedAscending) "" else "DESC"},
                    ${SortHelper.orderForGroupTypeRecursive(groupMode, groupAscending)},
                    ${SortHelper.orderForSortTypeRecursive(sortMode, sortAscending, subtaskMode, subtaskAscending)}
            ),
            numbered_tasks AS (
                SELECT task, ROW_NUMBER() OVER () AS sequence
                FROM recursive_tasks
            ),
            max_indent AS (
                SELECT task,
                MAX(recursive_tasks.indent) OVER (PARTITION BY task) AS max_indent
                FROM recursive_tasks
            ),
            child_counts AS (
                SELECT DISTINCT(parent),
                COUNT(*) OVER (PARTITION BY parent) AS children
                FROM recursive_tasks
                WHERE parent > 0
            )
            SELECT
                ${TaskListQuery.FIELDS.joinToString(",\n") { it.toStringInSelect() }},
                group_concat(distinct(tag_uid)) AS tags,
                indent,
                sort_group,
                children,
                primary_sort,
                secondary_sort,
                parent_complete
            FROM tasks
                INNER JOIN numbered_tasks ON tasks._id = numbered_tasks.task
                INNER JOIN max_indent ON tasks._id = max_indent.task
                INNER JOIN recursive_tasks ON recursive_tasks.task = tasks._id
                LEFT JOIN child_counts ON child_counts.parent = tasks._id
                ${TaskListQuery.JOINS}
            WHERE
                recursive_tasks.hidden = 0
                AND recursive_tasks.indent = max_indent
                GROUP BY tasks._id
                ORDER BY sequence
        """.trimIndent()

        return SortHelper.adjustQueryForFlags(preferences, query)
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
