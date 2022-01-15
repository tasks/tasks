package org.tasks.data

import com.todoroo.andlib.data.Table
import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Field.Companion.field
import com.todoroo.andlib.sql.Join
import com.todoroo.andlib.sql.Query
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.core.SortHelper
import com.todoroo.astrid.data.Task
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.preferences.QueryPreferences

internal object TaskListQueryRecursive {
    private val RECURSIVE = Table("recursive_tasks")
    private val RECURSIVE_TASK = field("$RECURSIVE.task")
    private val FIELDS =
            TaskListQuery.FIELDS.plus(listOf(
                    field("(${Query.select(field("group_concat(distinct(tag_uid))")).from(Tag.TABLE).where(Task.ID.eq(Tag.TASK))} GROUP BY ${Tag.TASK})").`as`("tags"),
                    field("indent"),
                    field("sort_group").`as`("sortGroup"),
                    field("children"),
                    field("primary_sort").`as`("primarySort"),
                    field("secondary_sort").`as`("secondarySort"))).toTypedArray()
    private val JOINS = """
        ${Join.inner(RECURSIVE, Task.ID.eq(RECURSIVE_TASK))}
        LEFT JOIN (SELECT parent, count(distinct recursive_tasks.task) AS children FROM recursive_tasks GROUP BY parent) AS recursive_children ON recursive_children.parent = tasks._id
        ${TaskListQuery.JOINS}
    """.trimIndent()
    private val GOOGLE_SUBTASKS =
            QueryTemplate()
                    .join(Join.inner(RECURSIVE, GoogleTask.PARENT.eq(RECURSIVE_TASK)))
                    .join(Join.inner(GoogleTask.TABLE, Criterion.and(GoogleTask.TASK.eq(Task.ID), GoogleTask.DELETED.eq(0))))
                    .where(activeAndVisible())
    private val ALL_SUBTASKS =
            QueryTemplate()
                    .join(Join.inner(RECURSIVE, Criterion.or(GoogleTask.PARENT.eq(RECURSIVE_TASK), Task.PARENT.eq(RECURSIVE_TASK))))
                    .join(Join.left(GoogleTask.TABLE, Criterion.and(GoogleTask.TASK.eq(Task.ID), GoogleTask.DELETED.eq(0))))
                    .where(activeAndVisible())

    fun getRecursiveQuery(
            filter: Filter,
            preferences: QueryPreferences,
            subtasks: SubtaskInfo
    ): MutableList<String> {
        var joinedQuery = JOINS
        var where = " WHERE recursive_tasks.hidden = 0"
        val parentQuery: String
        val subtaskQuery: QueryTemplate
        when (filter) {
            is CaldavFilter -> {
                parentQuery = newCaldavQuery(filter)
                subtaskQuery = QueryTemplate()
                        .join(Join.inner(RECURSIVE, Task.PARENT.eq(RECURSIVE_TASK)))
                        .join(Join.inner(CaldavTask.TABLE, Criterion.and(CaldavTask.TASK.eq(Task.ID), CaldavTask.DELETED.eq(0))))
                        .where(activeAndVisible())
            }
            is GtasksFilter -> {
                parentQuery = newGoogleTaskQuery(filter)
                subtaskQuery = GOOGLE_SUBTASKS
            }
            else -> {
                parentQuery = PermaSql.replacePlaceholdersForQuery(filter.getSqlQuery())
                subtaskQuery = when {
                    subtasks.hasGoogleSubtasks && subtasks.hasSubtasks -> ALL_SUBTASKS
                    subtasks.hasGoogleSubtasks -> GOOGLE_SUBTASKS
                    else -> QueryTemplate()
                            .join(Join.inner(RECURSIVE, Task.PARENT.eq(RECURSIVE_TASK)))
                            .where(activeAndVisible())
                }
                joinedQuery += " LEFT JOIN (SELECT task, max(indent) AS max_indent FROM recursive_tasks GROUP BY task) AS recursive_indents ON recursive_indents.task = tasks._id "
                where += " AND indent = max_indent "
            }
        }
        joinedQuery += where
        val manualSort = preferences.isManualSort
        val sortMode: Int
        val sortField: String
        when {
            manualSort && filter is GtasksFilter -> {
                sortMode = SortHelper.SORT_GTASKS
                sortField = "google_tasks.gt_order"
            }
            manualSort && filter is CaldavFilter -> {
                sortMode = SortHelper.SORT_CALDAV
                sortField = SortHelper.CALDAV_ORDER_COLUMN
            }
            else -> {
                sortMode = preferences.sortMode
                sortField = "NULL"
            }
        }
        val reverseSort = preferences.isReverseSort && sortMode != SortHelper.SORT_GTASKS && sortMode != SortHelper.SORT_CALDAV
        val sortSelect = SortHelper.orderSelectForSortTypeRecursive(sortMode)
        val withClause ="""
            CREATE TEMPORARY TABLE `recursive_tasks` AS
            WITH RECURSIVE recursive_tasks (task, parent_complete, subtask_complete, completion_sort, parent, collapsed, hidden, indent, title, sortField, primary_sort, secondary_sort, sort_group) AS (
                SELECT tasks._id, tasks.completed > 0 as parent_complete, 0 as subtask_complete, tasks.completed as completion_sort, 0 as parent, tasks.collapsed as collapsed, 0 as hidden, 0 AS sort_indent, UPPER(tasks.title) AS sort_title, $sortSelect, $sortField as primary_sort, NULL as secondarySort, ${SortHelper.getSortGroup(sortMode)} FROM tasks
                $parentQuery
                UNION ALL SELECT tasks._id, recursive_tasks.parent_complete, tasks.completed > 0, tasks.completed as completion_sort, recursive_tasks.task as parent, tasks.collapsed as collapsed, CASE WHEN recursive_tasks.collapsed > 0 OR recursive_tasks.hidden > 0 THEN 1 ELSE 0 END as hidden, recursive_tasks.indent+1 AS sort_indent, UPPER(tasks.title) AS sort_title, $sortSelect, recursive_tasks.primary_sort as primary_sort, $sortField as secondary_sort, recursive_tasks.sort_group FROM tasks
                $subtaskQuery
                ORDER BY parent_complete ASC, sort_indent DESC, subtask_complete ASC, completion_sort DESC, ${SortHelper.orderForSortTypeRecursive(sortMode, reverseSort)}
            ) SELECT * FROM recursive_tasks
        """.trimIndent()

        return mutableListOf(
                "DROP TABLE IF EXISTS `temp`.`recursive_tasks`",
                SortHelper.adjustQueryForFlags(preferences, withClause),
                "CREATE INDEX `r_tasks` ON `recursive_tasks` (`task`)",
                "CREATE INDEX `r_parents` ON `recursive_tasks` (`parent`)",
                Query.select(*FIELDS)
                        .withQueryTemplate(PermaSql.replacePlaceholdersForQuery(joinedQuery))
                        .from(Task.TABLE)
                        .toString())
    }

    private fun newCaldavQuery(filter: CaldavFilter) =
            QueryTemplate()
                    .join(Join.inner(
                            CaldavTask.TABLE,
                            Criterion.and(
                                    CaldavTask.CALENDAR.eq(filter.uuid),
                                    CaldavTask.TASK.eq(Task.ID),
                                    CaldavTask.DELETED.eq(0))))
                    .where(Criterion.and(activeAndVisible(), Task.PARENT.eq(0)))
                    .toString()

    private fun newGoogleTaskQuery(filter: GtasksFilter) =
            QueryTemplate()
                    .join(Join.inner(
                            GoogleTask.TABLE,
                            Criterion.and(
                                    GoogleTask.LIST.eq(filter.remoteId),
                                    GoogleTask.PARENT.eq(0),
                                    GoogleTask.TASK.eq(Task.ID),
                                    GoogleTask.DELETED.eq(0))))
                    .where(activeAndVisible())
                    .toString()
}