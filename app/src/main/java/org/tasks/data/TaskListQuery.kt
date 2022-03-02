package org.tasks.data

import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Field.Companion.field
import com.todoroo.andlib.sql.Join
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import org.tasks.data.TaskListQueryNonRecursive.getNonRecursiveQuery
import org.tasks.data.TaskListQueryRecursive.getRecursiveQuery
import org.tasks.preferences.QueryPreferences

object TaskListQuery {
    private val JOIN_GTASK = Criterion.and(
            Task.ID.eq(field("${TaskListFragment.GTASK_METADATA_JOIN}.gt_task")),
            field("${TaskListFragment.GTASK_METADATA_JOIN}.gt_deleted").eq(0))
    private val JOIN_CALDAV = Criterion.and(
            Task.ID.eq(field("${TaskListFragment.CALDAV_METADATA_JOIN}.cd_task")),
            field("${TaskListFragment.CALDAV_METADATA_JOIN}.cd_deleted").eq(0))
    val JOINS = """
        ${Join.left(GoogleTask.TABLE.`as`(TaskListFragment.GTASK_METADATA_JOIN), JOIN_GTASK)}
        ${Join.left(CaldavTask.TABLE.`as`(TaskListFragment.CALDAV_METADATA_JOIN), JOIN_CALDAV)}
        ${Join.left(Geofence.TABLE, Geofence.TASK.eq(Task.ID))}
        ${Join.left(Place.TABLE, Place.UID.eq(Geofence.PLACE))}
    """.trimIndent()
    val FIELDS = listOf(
            field("tasks.*"),
            field("${TaskListFragment.GTASK_METADATA_JOIN}.*"),
            field("${TaskListFragment.CALDAV_METADATA_JOIN}.*"),
            field("geofences.*"),
            field("places.*"))

    @JvmStatic
    fun getQuery(
            preferences: QueryPreferences,
            filter: Filter,
            subtasks: SubtaskInfo
    ): MutableList<String> = when {
        filter.supportsManualSort() && preferences.isManualSort ->
            getRecursiveQuery(filter, preferences, subtasks)
        filter.supportsAstridSorting() && preferences.isAstridSort ->
            getNonRecursiveQuery(filter, preferences)
        filter.supportsSubtasks() && subtasks.usesSubtasks() && !preferences.usePagedQueries() ->
            getRecursiveQuery(filter, preferences, subtasks)
        else -> getNonRecursiveQuery(filter, preferences)
    }
}