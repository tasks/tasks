package org.tasks.api

import org.tasks.TasksBuildConfig

object TasksContract {
    val AUTHORITY = "${TasksBuildConfig.APPLICATION_ID}.api"
    const val VERSION = "v0"
    val CONTENT_URI = "content://$AUTHORITY/$VERSION"

    val PERMISSION_READ = "${TasksBuildConfig.APPLICATION_ID}.permission.READ_TASKS"
    val PERMISSION_WRITE = "${TasksBuildConfig.APPLICATION_ID}.permission.WRITE_TASKS"

    const val DEFAULT_LIMIT = 100

    const val ID = "_id"

    const val PARAM_LIMIT = "limit"
    const val PARAM_OFFSET = "offset"

    const val PARAM_IF_MODIFIED_AT = "if_modified_at"

    object Tasks {
        const val PATH = "tasks"
        val CONTENT_URI = "${TasksContract.CONTENT_URI}/$PATH"
        const val TYPE_DIR = "vnd.android.cursor.dir/vnd.org.tasks.task"
        const val TYPE_ITEM = "vnd.android.cursor.item/vnd.org.tasks.task"

        const val TITLE = "title"
        const val NOTES = "notes"
        const val PRIORITY = "priority"
        const val DUE_DATE = "due_date"
        const val DUE_ALL_DAY = "due_all_day"
        const val START_DATE = "start_date"
        const val START_ALL_DAY = "start_all_day"
        const val COMPLETED_AT = "completed_at"
        const val CREATED_AT = "created_at"
        const val MODIFIED_AT = "modified_at"
        const val RECURRENCE = "recurrence"
        const val REPEAT_FROM = "repeat_from"
        const val PARENT_ID = "parent_id"
        const val LIST_ID = "list_id"
        const val TAG_IDS = "tag_ids"
        const val PLACE_ID = "place_id"
        const val CHILD_COUNT = "child_count"
        const val UNCOMPLETED_CHILD_COUNT = "uncompleted_child_count"
        const val IS_READ_ONLY = "is_read_only"

        val COLUMNS = listOf(
            ID, TITLE, NOTES, PRIORITY, DUE_DATE, DUE_ALL_DAY, START_DATE, START_ALL_DAY,
            COMPLETED_AT, CREATED_AT, MODIFIED_AT, RECURRENCE, REPEAT_FROM, PARENT_ID, LIST_ID,
            TAG_IDS, PLACE_ID, CHILD_COUNT, UNCOMPLETED_CHILD_COUNT, IS_READ_ONLY,
        )

        val WRITABLE = setOf(
            TITLE, NOTES, PRIORITY, DUE_DATE, DUE_ALL_DAY, START_DATE, START_ALL_DAY, COMPLETED_AT,
            RECURRENCE, REPEAT_FROM, PARENT_ID, LIST_ID, PLACE_ID,
        )

        val INSERT_ONLY = emptySet<String>()

        const val PARAM_ID = "_id"
        const val PARAM_SEARCH = "search"
        const val PARAM_LIST = "list_id"
        const val PARAM_TAG = "tag_id"
        const val PARAM_PLACE = "place_id"
        const val PARAM_PRIORITY = "priority"
        const val PARAM_PARENT = "parent_id"
        const val PARAM_COMPLETED = "completed"
        const val PARAM_SORT = "sort"
        const val PARAM_SORT_DESC = "sort_desc"

        const val PARAM_DUE_BEFORE = "due_before"
        const val PARAM_DUE_AFTER = "due_after"
        const val PARAM_START_BEFORE = "start_before"
        const val PARAM_START_AFTER = "start_after"
        const val PARAM_COMPLETED_BEFORE = "completed_before"
        const val PARAM_COMPLETED_AFTER = "completed_after"
        const val PARAM_CREATED_BEFORE = "created_before"
        const val PARAM_CREATED_AFTER = "created_after"
        const val PARAM_MODIFIED_BEFORE = "modified_before"
        const val PARAM_MODIFIED_AFTER = "modified_after"

        val PARAMS = listOf(
            PARAM_ID, PARAM_SEARCH, PARAM_LIST, PARAM_TAG, PARAM_PLACE, PARAM_PRIORITY,
            PARAM_PARENT, PARAM_COMPLETED,
            PARAM_DUE_BEFORE, PARAM_DUE_AFTER, PARAM_START_BEFORE, PARAM_START_AFTER,
            PARAM_COMPLETED_BEFORE, PARAM_COMPLETED_AFTER, PARAM_CREATED_BEFORE, PARAM_CREATED_AFTER,
            PARAM_MODIFIED_BEFORE, PARAM_MODIFIED_AFTER, PARAM_SORT, PARAM_SORT_DESC,
            PARAM_LIMIT, PARAM_OFFSET,
        )

        const val SORT_DUE = "due"
        const val SORT_START = "start"
        const val SORT_CREATED = "created"
        const val SORT_MODIFIED = "modified"
        const val SORT_PRIORITY = "priority"
        const val SORT_TITLE = "title"

        val SORTS = listOf(SORT_DUE, SORT_START, SORT_CREATED, SORT_MODIFIED, SORT_PRIORITY, SORT_TITLE)

        const val PRIORITY_HIGH = "high"
        const val PRIORITY_MEDIUM = "medium"
        const val PRIORITY_LOW = "low"
        const val PRIORITY_NONE = "none"

        val PRIORITIES = listOf(PRIORITY_HIGH, PRIORITY_MEDIUM, PRIORITY_LOW, PRIORITY_NONE)

        const val REPEAT_FROM_DUE_DATE = "due_date"
        const val REPEAT_FROM_COMPLETION_DATE = "completion_date"

        val REPEAT_FROMS = listOf(REPEAT_FROM_DUE_DATE, REPEAT_FROM_COMPLETION_DATE)
    }

    object Alarms {
        const val PATH = "alarms"
        val CONTENT_URI = "${TasksContract.CONTENT_URI}/$PATH"
        const val TYPE_DIR = "vnd.android.cursor.dir/vnd.org.tasks.alarm"
        const val TYPE_ITEM = "vnd.android.cursor.item/vnd.org.tasks.alarm"

        const val TASK_ID = "task_id"
        const val TYPE = "type"
        const val TRIGGER_AT = "trigger_at"
        const val OFFSET_MS = "offset_ms"
        const val REPEAT_COUNT = "repeat_count"
        const val INTERVAL_MS = "interval_ms"
        const val PLACE_ID = "place_id"

        val COLUMNS = listOf(
            ID, TASK_ID, TYPE, TRIGGER_AT, OFFSET_MS, REPEAT_COUNT, INTERVAL_MS, PLACE_ID,
        )
        val WRITABLE = setOf(TRIGGER_AT, OFFSET_MS, REPEAT_COUNT, INTERVAL_MS)
        val INSERT_ONLY = setOf(TASK_ID, TYPE, PLACE_ID)

        const val PARAM_TASK = "task_id"
        const val PARAM_TYPE = "type"
        const val PARAM_PLACE = "place_id"

        val PARAMS = listOf(PARAM_TASK, PARAM_TYPE, PARAM_PLACE, PARAM_LIMIT, PARAM_OFFSET)

        const val TYPE_DATE_TIME = "date_time"
        const val TYPE_RELATIVE_START = "relative_start"
        const val TYPE_RELATIVE_DUE = "relative_due"
        const val TYPE_RANDOM = "random"
        const val TYPE_SNOOZE = "snooze"
        const val TYPE_LOCATION_ARRIVAL = "location_arrival"
        const val TYPE_LOCATION_DEPARTURE = "location_departure"

        val TYPES = listOf(
            TYPE_DATE_TIME, TYPE_RELATIVE_START, TYPE_RELATIVE_DUE, TYPE_RANDOM, TYPE_SNOOZE,
            TYPE_LOCATION_ARRIVAL, TYPE_LOCATION_DEPARTURE,
        )

        val LOCATION_TYPES = setOf(TYPE_LOCATION_ARRIVAL, TYPE_LOCATION_DEPARTURE)

        const val LOCATION_ID_BASE = 1_000_000_000L

        data class LocationId(val geofenceId: Long, val arrival: Boolean)

        fun encodeLocationId(geofenceId: Long, arrival: Boolean): Long =
            LOCATION_ID_BASE + geofenceId * 2 + if (arrival) 0 else 1

        fun decodeLocationId(id: Long): LocationId? {
            if (id < LOCATION_ID_BASE) return null
            val offset = id - LOCATION_ID_BASE
            return LocationId(geofenceId = offset / 2, arrival = offset % 2 == 0L)
        }

        fun isLocationId(id: Long) = id >= LOCATION_ID_BASE
    }

    object TaskTags {
        const val PATH = "task_tags"
        val CONTENT_URI = "${TasksContract.CONTENT_URI}/$PATH"
        const val TYPE_DIR = "vnd.android.cursor.dir/vnd.org.tasks.task_tag"
        const val TYPE_ITEM = "vnd.android.cursor.item/vnd.org.tasks.task_tag"

        const val TASK_ID = "task_id"
        const val TAG_ID = "tag_id"

        val COLUMNS = listOf(ID, TASK_ID, TAG_ID)
        val WRITABLE = emptySet<String>()
        val INSERT_ONLY = setOf(TASK_ID, TAG_ID)

        const val PARAM_TASK = "task_id"
        const val PARAM_TAG = "tag_id"

        val PARAMS = listOf(PARAM_TAG, PARAM_LIMIT, PARAM_OFFSET)
    }

    object Lists {
        const val PATH = "lists"
        val CONTENT_URI = "${TasksContract.CONTENT_URI}/$PATH"
        const val TYPE_DIR = "vnd.android.cursor.dir/vnd.org.tasks.list"
        const val TYPE_ITEM = "vnd.android.cursor.item/vnd.org.tasks.list"

        const val TITLE = "title"
        const val COLOR = "color"
        const val ICON = "icon"
        const val ORDER = "order"
        const val ACCESS = "access"
        const val ACCOUNT_ID = "account_id"

        val COLUMNS = listOf(ID, TITLE, COLOR, ICON, ORDER, ACCESS, ACCOUNT_ID)
        val WRITABLE = setOf(TITLE, COLOR, ICON)
        val INSERT_ONLY = setOf(ACCOUNT_ID)

        const val PARAM_ID = "_id"
        const val PARAM_ACCOUNT = "account_id"
        const val PARAM_ACCESS = "access"

        val PARAMS = listOf(PARAM_ID, PARAM_ACCOUNT, PARAM_ACCESS, PARAM_LIMIT, PARAM_OFFSET)

        const val ACCESS_OWNER = "owner"
        const val ACCESS_READ_WRITE = "read_write"
        const val ACCESS_READ_ONLY = "read_only"

        val ACCESS_LEVELS = listOf(ACCESS_OWNER, ACCESS_READ_WRITE, ACCESS_READ_ONLY)
    }

    object Tags {
        const val PATH = "tags"
        val CONTENT_URI = "${TasksContract.CONTENT_URI}/$PATH"
        const val TYPE_DIR = "vnd.android.cursor.dir/vnd.org.tasks.tag"
        const val TYPE_ITEM = "vnd.android.cursor.item/vnd.org.tasks.tag"

        const val NAME = "name"
        const val COLOR = "color"
        const val ICON = "icon"
        const val ORDER = "order"

        val COLUMNS = listOf(ID, NAME, COLOR, ICON, ORDER)
        val WRITABLE = setOf(NAME, COLOR, ICON)
        val INSERT_ONLY = emptySet<String>()

        const val PARAM_ID = "_id"

        val PARAMS = listOf(PARAM_ID, PARAM_LIMIT, PARAM_OFFSET)
    }

    object Places {
        const val PATH = "places"
        val CONTENT_URI = "${TasksContract.CONTENT_URI}/$PATH"
        const val TYPE_DIR = "vnd.android.cursor.dir/vnd.org.tasks.place"
        const val TYPE_ITEM = "vnd.android.cursor.item/vnd.org.tasks.place"

        const val NAME = "name"
        const val DISPLAY_NAME = "display_name"
        const val ADDRESS = "address"
        const val PHONE = "phone"
        const val URL = "url"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val RADIUS = "radius"
        const val COLOR = "color"
        const val ICON = "icon"
        const val ORDER = "order"

        val COLUMNS = listOf(
            ID, NAME, DISPLAY_NAME, ADDRESS, PHONE, URL, LATITUDE, LONGITUDE, RADIUS, COLOR,
            ICON, ORDER,
        )
        val WRITABLE = setOf(NAME, ADDRESS, PHONE, URL, RADIUS, COLOR, ICON)
        val INSERT_ONLY = setOf(LATITUDE, LONGITUDE)

        const val PARAM_ID = "_id"

        val PARAMS = listOf(PARAM_ID, PARAM_LIMIT, PARAM_OFFSET)
    }

    object Accounts {
        const val PATH = "accounts"
        val CONTENT_URI = "${TasksContract.CONTENT_URI}/$PATH"
        const val TYPE_DIR = "vnd.android.cursor.dir/vnd.org.tasks.account"
        const val TYPE_ITEM = "vnd.android.cursor.item/vnd.org.tasks.account"

        const val NAME = "name"
        const val TYPE = "type"
        const val USERNAME = "username"
        const val URL = "url"
        const val ERROR = "error"
        const val REPEATS_ON_SERVER = "repeats_on_server"

        val COLUMNS = listOf(ID, NAME, TYPE, USERNAME, URL, ERROR, REPEATS_ON_SERVER)
        val WRITABLE = emptySet<String>()
        val INSERT_ONLY = emptySet<String>()

        const val PARAM_ID = "_id"

        val PARAMS = listOf(PARAM_ID, PARAM_LIMIT, PARAM_OFFSET)

        const val TYPE_CALDAV = "caldav"
        const val TYPE_TASKS_ORG = "tasks_org"
        const val TYPE_GOOGLE_TASKS = "google_tasks"
        const val TYPE_MICROSOFT = "microsoft"
        const val TYPE_ETEBASE = "etebase"
        const val TYPE_OPENTASKS = "opentasks"
        const val TYPE_LOCAL = "local"

        val TYPES = listOf(
            TYPE_CALDAV, TYPE_TASKS_ORG, TYPE_GOOGLE_TASKS, TYPE_MICROSOFT, TYPE_ETEBASE,
            TYPE_OPENTASKS, TYPE_LOCAL,
        )

        const val ERROR_UNAUTHORIZED = "unauthorized"
        const val ERROR_PAYMENT_REQUIRED = "payment_required"
        const val ERROR_TERMS_REQUIRED = "terms_required"
        const val ERROR_FAILED = "failed"

        val ERRORS = listOf(
            ERROR_UNAUTHORIZED, ERROR_PAYMENT_REQUIRED, ERROR_TERMS_REQUIRED, ERROR_FAILED,
        )
    }

    val COLLECTIONS = listOf(
        Tasks.PATH, Alarms.PATH, TaskTags.PATH, Lists.PATH, Tags.PATH, Places.PATH,
        Accounts.PATH,
    )

    fun columnsFor(path: String): List<String> = when (path) {
        Tasks.PATH -> Tasks.COLUMNS
        Alarms.PATH -> Alarms.COLUMNS
        TaskTags.PATH -> TaskTags.COLUMNS
        Lists.PATH -> Lists.COLUMNS
        Tags.PATH -> Tags.COLUMNS
        Places.PATH -> Places.COLUMNS
        Accounts.PATH -> Accounts.COLUMNS
        else -> throw IllegalArgumentException("Unknown collection: $path")
    }

    fun paramsFor(path: String): List<String> = when (path) {
        Tasks.PATH -> Tasks.PARAMS
        Alarms.PATH -> Alarms.PARAMS
        TaskTags.PATH -> TaskTags.PARAMS
        Lists.PATH -> Lists.PARAMS
        Tags.PATH -> Tags.PARAMS
        Places.PATH -> Places.PARAMS
        Accounts.PATH -> Accounts.PARAMS
        else -> throw IllegalArgumentException("Unknown collection: $path")
    }
}
