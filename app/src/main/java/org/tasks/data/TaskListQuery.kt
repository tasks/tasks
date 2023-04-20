package org.tasks.data

import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Field.Companion.field
import com.todoroo.andlib.sql.Join
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import org.tasks.data.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.TaskListQueryNonRecursive.getNonRecursiveQuery
import org.tasks.data.TaskListQueryRecursive.getRecursiveQuery
import org.tasks.preferences.QueryPreferences

object TaskListQuery {
    private val JOIN_CALDAV = Criterion.and(
            Task.ID.eq(field("${TaskListFragment.CALDAV_METADATA_JOIN}.cd_task")),
            field("${TaskListFragment.CALDAV_METADATA_JOIN}.cd_deleted").eq(0))
    val JOINS = """
        ${Join.left(CaldavTask.TABLE.`as`(TaskListFragment.CALDAV_METADATA_JOIN), JOIN_CALDAV)}
        ${Join.left(CaldavCalendar.TABLE, field("${TaskListFragment.CALDAV_METADATA_JOIN}.cd_calendar").eq(CaldavCalendar.UUID))}
        ${Join.left(CaldavAccount.TABLE, CaldavCalendar.ACCOUNT.eq(CaldavAccount.UUID))}
        ${Join.left(Geofence.TABLE, Geofence.TASK.eq(Task.ID))}
        ${Join.left(Place.TABLE, Place.UID.eq(Geofence.PLACE))}
    """.trimIndent()
    val FIELDS = listOf(
            field("tasks.*"),
            field("${TaskListFragment.CALDAV_METADATA_JOIN}.*"),
            field("CASE ${CaldavAccount.ACCOUNT_TYPE} WHEN $TYPE_GOOGLE_TASKS THEN 1 ELSE 0 END").`as`("isGoogleTask"),
            field("geofences.*"),
            field("places.*"))

    @JvmStatic
    fun getQuery(
            preferences: QueryPreferences,
            filter: Filter,
            subtasks: SubtaskInfo
    ): MutableList<String> = when {
        filter.supportsManualSort() && preferences.isManualSort ->
            getRecursiveQuery(filter, preferences)
        filter.supportsAstridSorting() && preferences.isAstridSort ->
            getNonRecursiveQuery(filter, preferences)
        filter.supportsSubtasks() && subtasks.usesSubtasks() ->
            getRecursiveQuery(filter, preferences)
        else -> getNonRecursiveQuery(filter, preferences)
    }
}