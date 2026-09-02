package org.tasks.api

import androidx.sqlite.SQLiteStatement
import org.tasks.api.TasksContract.Accounts
import org.tasks.api.TasksContract.Alarms
import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Places
import org.tasks.api.TasksContract.Tags
import org.tasks.api.TasksContract.TaskTags
import org.tasks.api.TasksContract.Tasks
import org.tasks.data.NO_ORDER
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.isPurchaseTokenInUse
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.time.startOfMinute

internal class ApiSource(

    val select: String,
    val from: String,
    val baseWhere: String?,
    val idExpression: String,

    val filter: (ApiQueryArgs) -> SqlWhere? = { SqlWhere() },

    val byId: (Long) -> SqlWhere? = { null },
)

internal class ApiTable(
    val path: String,
    val columns: List<String>,
    val sources: List<ApiSource>,
    val read: (SQLiteStatement) -> Array<Any?>,
) {
    constructor(
        path: String,
        columns: List<String>,
        select: String,
        from: String,
        baseWhere: String?,
        idColumn: String,
        read: (SQLiteStatement) -> Array<Any?>,
    ) : this(
        path = path,
        columns = columns,
        sources = listOf(
            ApiSource(
                select = select,
                from = from,
                baseWhere = baseWhere,
                idExpression = idColumn,
                byId = { id -> SqlWhere().and("$idColumn = ?", id) },
            ),
        ),
        read = read,
    )

    val idColumn: String get() = sources.single().idExpression

    fun indexOf(column: String) = columns.indexOf(column)
}

private fun SQLiteStatement.text(index: Int): String = if (isNull(index)) "" else getText(index)

private fun SQLiteStatement.longOrZero(index: Int): Long = if (isNull(index)) 0L else getLong(index)

private fun SQLiteStatement.intOrZero(index: Int): Int = if (isNull(index)) 0 else getInt(index)

private fun SQLiteStatement.doubleOrZero(index: Int): Double = if (isNull(index)) 0.0 else getDouble(index)

private fun SQLiteStatement.order(index: Int): Int = intOrZero(index).takeIf { it != NO_ORDER } ?: 0

private fun bool(value: Boolean) = if (value) 1 else 0

internal fun isAllDay(timestamp: Long) = timestamp > 0 && !Task.hasDueTime(timestamp)

internal fun Long.withoutTimeMarker(): Long = if (this > 0) startOfMinute() else this

internal object Priorities {
    val TO_API = mapOf(
        Task.Priority.HIGH to Tasks.PRIORITY_HIGH,
        Task.Priority.MEDIUM to Tasks.PRIORITY_MEDIUM,
        Task.Priority.LOW to Tasks.PRIORITY_LOW,
        Task.Priority.NONE to Tasks.PRIORITY_NONE,
    )
    val FROM_API = TO_API.entries.associate { (k, v) -> v to k }

    fun toApi(stored: Int) = TO_API[stored.coerceIn(Task.Priority.HIGH, Task.Priority.NONE)]!!
}

internal object RepeatFrom {
    val TO_API = mapOf(
        Task.RepeatFrom.DUE_DATE to Tasks.REPEAT_FROM_DUE_DATE,
        Task.RepeatFrom.COMPLETION_DATE to Tasks.REPEAT_FROM_COMPLETION_DATE,
    )
    val FROM_API = TO_API.entries.associate { (k, v) -> v to k }

    fun toApi(stored: Int) = TO_API[stored] ?: Tasks.REPEAT_FROM_DUE_DATE
}

internal object AccountErrors {
    fun toApi(stored: String): String {
        if (stored.isBlank()) {
            return ""
        }
        val account = CaldavAccount(error = stored)
        return when {
            account.isLoggedOut() -> Accounts.ERROR_UNAUTHORIZED
            account.isPaymentRequired() || stored.isPurchaseTokenInUse() ->
                Accounts.ERROR_PAYMENT_REQUIRED
            account.isTosRequired() -> Accounts.ERROR_TERMS_REQUIRED
            else -> Accounts.ERROR_FAILED
        }
    }
}

internal object AlarmTypes {

    val STORED = mapOf(
        Alarm.TYPE_DATE_TIME to Alarms.TYPE_DATE_TIME,
        Alarm.TYPE_REL_START to Alarms.TYPE_RELATIVE_START,
        Alarm.TYPE_REL_END to Alarms.TYPE_RELATIVE_DUE,
        Alarm.TYPE_RANDOM to Alarms.TYPE_RANDOM,
        Alarm.TYPE_SNOOZE to Alarms.TYPE_SNOOZE,
    )

    val LOCATION = mapOf(
        Alarm.TYPE_GEO_ENTER to Alarms.TYPE_LOCATION_ARRIVAL,
        Alarm.TYPE_GEO_EXIT to Alarms.TYPE_LOCATION_DEPARTURE,
    )

    val TO_API = STORED + LOCATION
    val FROM_API = TO_API.entries.associate { (k, v) -> v to k }

    val STORED_FROM_API = STORED.entries.associate { (k, v) -> v to k }

    val ABSOLUTE = setOf(Alarm.TYPE_DATE_TIME, Alarm.TYPE_SNOOZE)

    fun toApi(stored: Int) = TO_API[stored] ?: Alarms.TYPE_DATE_TIME

    fun isLocation(stored: Int) = stored in LOCATION
}

internal object ListAccess {
    val TO_API = mapOf(
        CaldavCalendar.ACCESS_READ_ONLY to Lists.ACCESS_READ_ONLY,
        CaldavCalendar.ACCESS_READ_WRITE to Lists.ACCESS_READ_WRITE,
        CaldavCalendar.ACCESS_OWNER to Lists.ACCESS_OWNER,
    )

    fun toApi(stored: Int) = TO_API[stored] ?: Lists.ACCESS_OWNER

    fun storedFor(api: String): List<Int> = when (api) {
        Lists.ACCESS_READ_ONLY -> listOf(CaldavCalendar.ACCESS_READ_ONLY)
        Lists.ACCESS_READ_WRITE -> listOf(CaldavCalendar.ACCESS_READ_WRITE)
        Lists.ACCESS_OWNER -> listOf(CaldavCalendar.ACCESS_OWNER, CaldavCalendar.ACCESS_UNKNOWN)
        else -> throw IllegalArgumentException("Unknown access level: $api")
    }
}

internal object AccountTypes {
    val TO_API = mapOf(
        CaldavAccount.TYPE_CALDAV to Accounts.TYPE_CALDAV,
        CaldavAccount.TYPE_LOCAL to Accounts.TYPE_LOCAL,
        CaldavAccount.TYPE_OPENTASKS to Accounts.TYPE_OPENTASKS,
        CaldavAccount.TYPE_TASKS to Accounts.TYPE_TASKS_ORG,
        CaldavAccount.TYPE_ETEBASE to Accounts.TYPE_ETEBASE,
        CaldavAccount.TYPE_MICROSOFT to Accounts.TYPE_MICROSOFT,
        CaldavAccount.TYPE_GOOGLE_TASKS to Accounts.TYPE_GOOGLE_TASKS,
    )

    fun toApi(stored: Int) = TO_API[stored] ?: Accounts.TYPE_CALDAV
}

private const val TASK_LIST_UUID =
    "(SELECT cd_calendar FROM caldav_tasks WHERE cd_task = tasks._id AND cd_deleted = 0 LIMIT 1)"

private const val TASK_LIST_ID =
    "(SELECT cdl_id FROM caldav_lists WHERE cdl_uuid = $TASK_LIST_UUID LIMIT 1)"

internal const val TASK_LIST_ACCESS =
    "(SELECT cdl_access FROM caldav_lists WHERE cdl_uuid = $TASK_LIST_UUID LIMIT 1)"

private const val TASK_TAG_IDS = """
    (SELECT group_concat(DISTINCT tagdata._id) FROM tags
        INNER JOIN tagdata ON tagdata.remoteId = tags.tag_uid
      WHERE tags.task = tasks._id)
"""

private const val TASK_PLACE_ID = """
    (SELECT places.place_id FROM geofences
        INNER JOIN places ON geofences.place = places.uid
      WHERE geofences.task = tasks._id
      ORDER BY places.name ASC LIMIT 1)
"""

private const val TASK_CHILD_COUNT =
    "(SELECT COUNT(*) FROM tasks children WHERE children.parent = tasks._id AND children.deleted = 0)"

private const val TASK_UNCOMPLETED_CHILD_COUNT =
    "(SELECT COUNT(*) FROM tasks children WHERE children.parent = tasks._id" +
            " AND children.deleted = 0 AND children.completed = 0)"

private const val GEOFENCE_PLACE_ID =
    "(SELECT places.place_id FROM places WHERE places.uid = geofences.place)"

private fun locationSource(arrival: Boolean): ApiSource {
    val flag = if (arrival) "geofences.arrival" else "geofences.departure"
    val storedType = if (arrival) Alarm.TYPE_GEO_ENTER else Alarm.TYPE_GEO_EXIT
    val apiType = if (arrival) Alarms.TYPE_LOCATION_ARRIVAL else Alarms.TYPE_LOCATION_DEPARTURE
    val offset = if (arrival) 0 else 1
    val id = "(${Alarms.LOCATION_ID_BASE} + geofences.geofence_id * 2 + $offset)"
    return ApiSource(
        select = "geofences.task, $storedType, 0, 0, 0, $GEOFENCE_PLACE_ID",
        from = "geofences INNER JOIN tasks ON tasks._id = geofences.task",
        baseWhere = "tasks.deleted = 0 AND $flag > 0",
        idExpression = id,
        filter = { args ->
            if (args.has(Alarms.PARAM_TYPE) && apiType !in args.all(Alarms.PARAM_TYPE)) {
                return@ApiSource null
            }
            SqlWhere().apply {
                if (args.has(Alarms.PARAM_TASK)) {
                    inLongs("geofences.task", args.longs(Alarms.PARAM_TASK))
                }
                if (args.has(Alarms.PARAM_PLACE)) {
                    and(
                        "EXISTS (SELECT 1 FROM places WHERE places.uid = geofences.place" +
                                " AND places.place_id IN" +
                                " (${args.longs(Alarms.PARAM_PLACE).joinToString(",")}))"
                    )
                }
            }
        },
        byId = { value ->
            Alarms.decodeLocationId(value)
                ?.takeIf { it.arrival == arrival }
                ?.let { SqlWhere().and("geofences.geofence_id = ?", it.geofenceId) }
        },
    )
}

private fun ApiQueryArgs.storedAlarmTypes(): List<Int>? {
    if (!has(Alarms.PARAM_TYPE)) return emptyList()
    val requested = all(Alarms.PARAM_TYPE)
    requested.forEach {
        if (it !in AlarmTypes.FROM_API) {
            throw IllegalArgumentException(
                "Unknown value for ${Alarms.PARAM_TYPE}: '$it'." +
                        " Expected one of ${Alarms.TYPES.joinToString("|")}"
            )
        }
    }
    return requested.mapNotNull { AlarmTypes.STORED_FROM_API[it] }.takeIf { it.isNotEmpty() }
}

internal object ApiTables {
    val TASKS = ApiTable(
        path = Tasks.PATH,
        columns = Tasks.COLUMNS,
        select = """
            tasks.remoteId, tasks.title, tasks.notes, tasks.importance, tasks.dueDate,
            tasks.hideUntil, tasks.completed, tasks.created, tasks.modified, tasks.recurrence,
            tasks.repeat_from, tasks.parent, $TASK_LIST_ID, $TASK_TAG_IDS, $TASK_PLACE_ID,
            $TASK_CHILD_COUNT, $TASK_UNCOMPLETED_CHILD_COUNT, $TASK_LIST_ACCESS
        """.trimIndent(),
        from = "tasks",
        baseWhere = "tasks.deleted = 0",
        idColumn = "tasks._id",
        read = { s ->
            val due = s.longOrZero(5)
            val start = s.longOrZero(6)
            arrayOf(
                s.getLong(0),
                s.text(2),
                s.text(3),
                Priorities.toApi(s.intOrZero(4)),
                due.withoutTimeMarker(),
                bool(isAllDay(due)),
                start.withoutTimeMarker(),
                bool(isAllDay(start)),
                s.longOrZero(7),
                s.longOrZero(8),
                s.longOrZero(9),
                s.text(10),
                RepeatFrom.toApi(s.intOrZero(11)),
                s.longOrZero(12),
                s.longOrZero(13),
                s.text(14),
                s.longOrZero(15),
                s.intOrZero(16),
                s.intOrZero(17),
                bool(!s.isNull(18) && s.getInt(18) == CaldavCalendar.ACCESS_READ_ONLY),
            )
        },
    )

    val ALARMS = ApiTable(
        path = Alarms.PATH,
        columns = Alarms.COLUMNS,
        sources = listOf(
            ApiSource(
                select = "alarms.task, alarms.type, alarms.time," +
                        " alarms.repeat, alarms.interval, 0",
                from = "alarms INNER JOIN tasks ON tasks._id = alarms.task",
                baseWhere = "tasks.deleted = 0" +
                        " AND alarms.type IN (${AlarmTypes.STORED.keys.joinToString(",")})",
                idExpression = "alarms._id",
                filter = { args ->

                    if (args.has(Alarms.PARAM_PLACE)) return@ApiSource null
                    val types = args.storedAlarmTypes() ?: return@ApiSource null
                    SqlWhere().apply {
                        if (args.has(Alarms.PARAM_TASK)) {
                            inLongs("alarms.task", args.longs(Alarms.PARAM_TASK))
                        }
                        types?.let { inInts("alarms.type", it) }
                    }
                },
                byId = { id ->
                    if (id >= Alarms.LOCATION_ID_BASE) null
                    else SqlWhere().and("alarms._id = ?", id)
                },
            ),
            locationSource(arrival = true),
            locationSource(arrival = false),
        ),
        read = { s ->
            val type = s.intOrZero(2)
            val time = s.longOrZero(3)
            val location = AlarmTypes.isLocation(type)
            arrayOf(
                s.getLong(0),
                s.getLong(1),
                AlarmTypes.toApi(type),
                if (!location && type in AlarmTypes.ABSOLUTE) time else 0L,
                if (!location && type !in AlarmTypes.ABSOLUTE) time else 0L,
                if (location) 0 else s.intOrZero(4),
                if (location) 0L else s.longOrZero(5),
                s.longOrZero(6),
            )
        },
    )

    val TASK_TAGS = ApiTable(
        path = TaskTags.PATH,
        columns = TaskTags.COLUMNS,
        select = "tags.task, (SELECT tagdata._id FROM tagdata WHERE tagdata.remoteId = tags.tag_uid)",
        from = "tags INNER JOIN tasks ON tasks._id = tags.task",
        baseWhere = "tasks.deleted = 0",
        idColumn = "tags._id",
        read = { s -> arrayOf(s.getLong(0), s.getLong(1), s.longOrZero(2)) },
    )

    val LISTS = ApiTable(
        path = Lists.PATH,
        columns = Lists.COLUMNS,
        select = """
            caldav_lists.cdl_name, caldav_lists.cdl_color,
            caldav_lists.cdl_icon, caldav_lists.cdl_order, caldav_lists.cdl_access,
            (SELECT cda_id FROM caldav_accounts WHERE cda_uuid = caldav_lists.cdl_account)
        """.trimIndent(),
        from = "caldav_lists",
        baseWhere = null,
        idColumn = "caldav_lists.cdl_id",
        read = { s ->
            arrayOf(
                s.getLong(0),
                s.text(1),
                s.intOrZero(2),
                s.text(3),
                s.order(4),
                ListAccess.toApi(s.intOrZero(5)),
                s.longOrZero(6),
            )
        },
    )

    val TAGS = ApiTable(
        path = Tags.PATH,
        columns = Tags.COLUMNS,
        select = "tagdata.name, tagdata.color, tagdata.td_icon, tagdata.td_order",
        from = "tagdata",
        baseWhere = null,
        idColumn = "tagdata._id",
        read = { s ->
            arrayOf(s.getLong(0), s.text(1), s.intOrZero(2), s.text(3), s.order(4))
        },
    )

    val PLACES = ApiTable(
        path = Places.PATH,
        columns = Places.COLUMNS,
        select = """
            places.name, places.address, places.phone, places.url,
            places.latitude, places.longitude, places.radius, places.place_color, places.place_icon,
            places.place_order
        """.trimIndent(),
        from = "places",
        baseWhere = null,
        idColumn = "places.place_id",
        read = { s ->
            val name = s.text(1)
            val address = s.text(2)
            val latitude = s.doubleOrZero(5)
            val longitude = s.doubleOrZero(6)
            arrayOf(
                s.getLong(0),
                name,
                Place(name = name, address = address, latitude = latitude, longitude = longitude)
                    .displayName,
                address,
                s.text(3),
                s.text(4),
                latitude,
                longitude,
                s.intOrZero(7),
                s.intOrZero(8),
                s.text(9),
                s.order(10),
            )
        },
    )

    val ACCOUNTS = ApiTable(
        path = Accounts.PATH,
        columns = Accounts.COLUMNS,
        select = """
            caldav_accounts.cda_name,
            caldav_accounts.cda_account_type, caldav_accounts.cda_username, caldav_accounts.cda_url,
            caldav_accounts.cda_error, caldav_accounts.cda_server_type
        """.trimIndent(),
        from = "caldav_accounts",
        baseWhere = null,
        idColumn = "caldav_accounts.cda_id",
        read = { s ->
            arrayOf(
                s.getLong(0),
                s.text(1),
                AccountTypes.toApi(s.intOrZero(2)),
                s.text(3),
                s.text(4),
                AccountErrors.toApi(s.text(5)),
                bool(CaldavAccount(serverType = s.intOrZero(6)).isSuppressRepeatingTasks),
            )
        },
    )

    val ALL = listOf(TASKS, ALARMS, TASK_TAGS, LISTS, TAGS, PLACES, ACCOUNTS)

    fun byPath(path: String) = ALL.first { it.path == path }
}
