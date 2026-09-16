package org.tasks.intents

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import org.tasks.analytics.Analytics
import org.tasks.analytics.AnalyticsEvents
import org.tasks.api.ApiQueryEngine
import org.tasks.api.ApiTaskFactory
import org.tasks.api.ApiWriter
import org.tasks.api.ListQuery
import org.tasks.api.ListRow
import org.tasks.api.ListWrite
import org.tasks.api.PlaceQuery
import org.tasks.api.PlaceWrite
import org.tasks.api.ReminderQuery
import org.tasks.api.ReminderWrite
import org.tasks.api.TagQuery
import org.tasks.api.TagWrite
import org.tasks.api.TaskQuery
import org.tasks.api.TaskRow
import org.tasks.api.TaskUpdate
import org.tasks.api.TaskWrite
import org.tasks.api.TasksContract
import org.tasks.api.TasksContract.Reminders.TYPE_LOCATION_ARRIVAL
import org.tasks.api.TasksContract.Reminders.TYPE_LOCATION_DEPARTURE
import org.tasks.api.completeTasks
import org.tasks.api.createList
import org.tasks.api.createPlace
import org.tasks.api.createTag
import org.tasks.api.createTasks
import org.tasks.api.deleteTask
import org.tasks.api.findLists
import org.tasks.api.findPlaces
import org.tasks.api.findReminders
import org.tasks.api.findTags
import org.tasks.api.findTasks
import org.tasks.api.setTaskReminders
import org.tasks.api.setTaskTags
import org.tasks.api.tasksById
import org.tasks.api.updateTasks
import org.tasks.ensureStarted

data class IntentTask(
    val id: Long,
    val title: String,
    val notes: String?,
    val due: Long?,
    val dueAllDay: Boolean,
    val recurrence: String?,
    val priority: String,
    val created: Long?,
    val completed: Long?,
    val list: IntentList,
    val tags: List<String>,
    val locationTrigger: IntentLocationTrigger?,
)

data class IntentList(
    val id: Long,
    val title: String,
    val isReadOnly: Boolean,
)

data class IntentLocationTrigger(
    val taskId: Long,
    val name: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val departing: Boolean,
)

data class IntentLocationTriggerWrite(
    val name: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val departing: Boolean,
)

object TasksIntents {
    @Throws(Throwable::class)
    suspend fun createTask(
        title: String,
        notes: String?,
        due: Long?,
        dueAllDay: Boolean,
        priority: String?,
        recurrence: String?,
        listId: Long?,
        tags: List<String>,
        locationTrigger: IntentLocationTriggerWrite?,
    ): IntentTask = call {
        val write = TaskWrite(
            title = title,
            notes = notes,
            priority = priority,
            due = due,
            dueAllDay = due?.let { dueAllDay },
            recurrence = recurrence,
            listId = listId,
        )
        val id = engine.createTasks(writer, listOf(write)).ids.single()
        if (tags.isNotEmpty()) {
            engine.setTaskTags(writer, listOf(id), add = tagIds(tags), remove = emptyList())
        }
        locationTrigger?.let { setLocationTrigger(id, it) }
        task(id)
    }

    @Throws(Throwable::class)
    suspend fun updateTask(
        id: Long,
        title: String?,
        notes: String?,
        due: Long?,
        dueAllDay: Boolean,
        priority: String?,
        recurrence: String?,
        listId: Long?,
        completed: Boolean?,
        tags: List<String>?,
        locationTrigger: IntentLocationTriggerWrite?,
    ): IntentTask = call {
        val patch = TaskWrite(
            title = title,
            notes = notes,
            priority = priority,
            due = due,
            dueAllDay = due?.let { dueAllDay },
            recurrence = recurrence,
            listId = listId,
        )
        if (!patch.isEmpty) {
            engine.updateTasks(writer, listOf(TaskUpdate(id, patch)))
        }
        tags?.let {
            val current = engine.tasksById(listOf(id)).firstOrNull()?.tagIds.orEmpty()
            val wanted = tagIds(it)
            engine.setTaskTags(writer, listOf(id), add = wanted - current.toSet(), remove = current - wanted.toSet())
        }
        locationTrigger?.let { setLocationTrigger(id, it) }
        completed?.let { engine.completeTasks(writer, listOf(id), completed = it) }
        task(id)
    }

    @Throws(Throwable::class)
    suspend fun deleteTasks(ids: List<Long>): Int = call {
        ids.sumOf { engine.deleteTask(writer, it).rowsDeleted }
    }

    @Throws(Throwable::class)
    suspend fun tasks(ids: List<Long>): List<IntentTask> = call {
        engine.tasksById(ids).toIntentTasks()
    }

    @Throws(Throwable::class)
    suspend fun findTasks(search: String?, limit: Int): List<IntentTask> = call {
        val query = TaskQuery(
            status = "open",
            matches = search?.trim()?.takeIf { it.isNotEmpty() }?.let { escapeRegex(it) },
            matchFields = listOf(TasksContract.Tasks.TITLE),
            sort = TasksContract.Tasks.SORT_DUE,
            limit = limit,
        )
        engine.findTasks(query).rows.toIntentTasks()
    }

    @Throws(Throwable::class)
    suspend fun createList(title: String): IntentList = call {
        val accountId = koin.get<ApiTaskFactory>().defaultList().account.id
        engine.createList(writer, ListWrite(title = title, accountId = accountId)).toIntentList()
    }

    @Throws(Throwable::class)
    suspend fun lists(ids: List<Long>): List<IntentList> = call {
        if (ids.isEmpty()) return@call emptyList()
        val found = engine.findLists(ListQuery(ids = ids, limit = ids.size)).rows.associateBy { it.id }
        ids.mapNotNull { found[it]?.toIntentList() }
    }

    @Throws(Throwable::class)
    suspend fun writableLists(): List<IntentList> = call {
        engine.findLists(ListQuery(limit = TasksContract.MAX_LIMIT))
            .rows
            .filterNot { it.isReadOnly }
            .map { it.toIntentList() }
    }

    @Throws(Throwable::class)
    suspend fun defaultList(): IntentList? = call {
        val id = koin.get<ApiTaskFactory>().defaultList().calendar.id
        engine.findLists(ListQuery(ids = listOf(id), limit = 1)).rows.firstOrNull()?.toIntentList()
    }

    private val koin get() = KoinPlatform.getKoin()

    private val engine: ApiQueryEngine get() = koin.get()

    private val writer: ApiWriter get() = koin.get()

    private suspend fun <T> call(block: suspend () -> T): T {
        ensureStarted()
        return withContext(Dispatchers.IO) {
            koin.get<Analytics>().logEventOncePerDay(AnalyticsEvents.APP_INTENT_CALL)
            block()
        }
    }

    private suspend fun task(id: Long): IntentTask =
        engine.tasksById(listOf(id)).toIntentTasks().firstOrNull()
            ?: throw IllegalStateException("Task $id not found")

    private suspend fun tagIds(names: List<String>): List<Long> = names
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .map { engine.createTag(writer, TagWrite(name = it)).row.id }

    private suspend fun setLocationTrigger(taskId: Long, trigger: IntentLocationTriggerWrite) {
        val place = engine.createPlace(
            writer,
            PlaceWrite(
                name = trigger.name,
                address = trigger.address,
                latitude = trigger.latitude,
                longitude = trigger.longitude,
            ),
        ).row
        val existing = engine
            .findReminders(ReminderQuery(taskIds = listOf(taskId), types = LOCATION_TYPES))
            .rows
            .map { it.id }
        engine.setTaskReminders(
            writer,
            taskId,
            add = listOf(
                ReminderWrite(
                    type = if (trigger.departing) TYPE_LOCATION_DEPARTURE else TYPE_LOCATION_ARRIVAL,
                    placeId = place.id,
                )
            ),
            removeReminderIds = existing,
        )
    }

    private suspend fun List<TaskRow>.toIntentTasks(): List<IntentTask> {
        if (isEmpty()) return emptyList()
        val lists = engine
            .findLists(ListQuery(ids = mapNotNull { it.listId }.distinct(), limit = TasksContract.MAX_LIMIT))
            .rows
            .associateBy { it.id }
        val tagIds = flatMap { it.tagIds }.distinct()
        val tags = if (tagIds.isEmpty()) {
            emptyMap()
        } else {
            engine.findTags(TagQuery(ids = tagIds, limit = tagIds.size)).rows.associate { it.id to it.name }
        }
        val reminders = engine
            .findReminders(ReminderQuery(taskIds = map { it.id }, types = LOCATION_TYPES, limit = TasksContract.MAX_LIMIT))
            .rows
            .groupBy { it.taskId }
        val placeIds = reminders.values.flatten().mapNotNull { it.placeId }.distinct()
        val places = if (placeIds.isEmpty()) {
            emptyMap()
        } else {
            engine.findPlaces(PlaceQuery(ids = placeIds, limit = placeIds.size)).rows.associateBy { it.id }
        }
        return map { row ->
            val list = row.listId?.let { lists[it] }
            val trigger = reminders[row.id]
                ?.firstNotNullOfOrNull { reminder ->
                    reminder.placeId?.let { places[it] }?.let { place -> reminder to place }
                }
            IntentTask(
                id = row.id,
                title = row.title,
                notes = row.notes,
                due = row.due,
                dueAllDay = row.dueAllDay,
                recurrence = row.recurrence?.takeIf { it.isNotBlank() },
                priority = row.priority,
                created = row.created,
                completed = row.completed,
                list = list?.toIntentList()
                    ?: IntentList(id = row.listId ?: 0, title = "", isReadOnly = row.isReadOnly),
                tags = row.tagIds.mapNotNull { tags[it] },
                locationTrigger = trigger?.let { (reminder, place) ->
                    IntentLocationTrigger(
                        taskId = row.id,
                        name = place.name,
                        address = place.address,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        departing = reminder.type == TYPE_LOCATION_DEPARTURE,
                    )
                },
            )
        }
    }

    private fun ListRow.toIntentList() = IntentList(id = id, title = title, isReadOnly = isReadOnly)

    private fun escapeRegex(text: String) = text.replace(REGEX_SPECIALS) { "\\" + it.value }

    private val REGEX_SPECIALS = Regex("""[\\^$.|?*+()\[\]{}]""")

    private val LOCATION_TYPES = listOf(TYPE_LOCATION_ARRIVAL, TYPE_LOCATION_DEPARTURE)
}
