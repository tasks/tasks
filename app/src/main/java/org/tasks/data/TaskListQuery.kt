package org.tasks.data

import org.tasks.data.TaskListQueryNonRecursive.getNonRecursiveQuery
import org.tasks.data.TaskListQueryRecursive.getRecursiveQuery
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion
import org.tasks.data.sql.Field.Companion.field
import org.tasks.data.sql.Join
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.Filter
import org.tasks.preferences.QueryPreferences

object TaskListQuery {
    private const val CALDAV_METADATA_JOIN = "for_caldav"
    private val JOIN_CALDAV = Criterion.and(
            Task.ID.eq(field("$CALDAV_METADATA_JOIN.cd_task")),
            field("$CALDAV_METADATA_JOIN.cd_deleted").eq(0))
    val JOINS = """
        ${Join.left(CaldavTask.TABLE.`as`(CALDAV_METADATA_JOIN), JOIN_CALDAV)}
        ${
        Join.left(
        CaldavCalendar.TABLE, field("$CALDAV_METADATA_JOIN.cd_calendar").eq(
            CaldavCalendar.UUID))}
        ${Join.left(CaldavAccount.TABLE, CaldavCalendar.ACCOUNT.eq(CaldavAccount.UUID))}
        ${Join.left(Geofence.TABLE, Geofence.TASK.eq(Task.ID))}
        ${Join.left(Place.TABLE, Place.UID.eq(Geofence.PLACE))}
    """.trimIndent()
    val FIELDS = listOf(
            field("tasks.*"),
            field("$CALDAV_METADATA_JOIN.*"),
            field("${CaldavAccount.ACCOUNT_TYPE}").`as`("accountType"),
            field("geofences.*"),
            field("places.*"))

    @JvmStatic
    fun getQuery(
        preferences: QueryPreferences,
        filter: Filter,
    ): MutableList<String> = when {
        filter.supportsManualSort() && preferences.isManualSort ->
            getRecursiveQuery(filter, preferences)
        filter is AstridOrderingFilter && preferences.isAstridSort ->
            getNonRecursiveQuery(filter, preferences)
        filter.supportsSorting() ->
            getRecursiveQuery(filter, preferences)
        else -> getNonRecursiveQuery(filter, preferences)
    }
}