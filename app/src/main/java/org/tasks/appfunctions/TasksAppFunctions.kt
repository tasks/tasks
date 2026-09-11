package org.tasks.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.api.AccountQuery
import org.tasks.api.ReminderQuery
import org.tasks.api.ApiQueryEngine
import org.tasks.analytics.Analytics
import org.tasks.analytics.AnalyticsEvents
import org.tasks.api.ApiWriter
import org.tasks.api.ListQuery
import org.tasks.api.ListWrite
import org.tasks.api.PlaceQuery
import org.tasks.api.PlaceWrite
import org.tasks.api.TagQuery
import org.tasks.api.TagWrite
import org.tasks.api.TaskQuery
import org.tasks.api.TaskUpdate
import org.tasks.api.changeList
import org.tasks.api.changePlace
import org.tasks.api.changeTag
import org.tasks.api.completeTasks
import org.tasks.api.createList
import org.tasks.api.createPlace
import org.tasks.api.createTag
import org.tasks.api.createTasks
import org.tasks.api.deleteList
import org.tasks.api.deletePlace
import org.tasks.api.deleteTag
import org.tasks.api.deleteTask
import org.tasks.api.findAccounts
import org.tasks.api.findReminders
import org.tasks.api.findLists
import org.tasks.api.findPlaces
import org.tasks.api.findTags
import org.tasks.api.findTasks
import org.tasks.api.setTaskReminders
import org.tasks.api.setTaskTags
import org.tasks.api.updateTasks

@RequiresApi(36)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "TasksAppFunctionService",
    appFunctionXmlFileName = "tasks_app_functions",
)
abstract class TasksAppFunctions : AppFunctionService() {

    @Inject internal lateinit var writer: ApiWriter

    @Inject internal lateinit var engine: ApiQueryEngine

    @Inject internal lateinit var analytics: Analytics

    /**
     * Search tasks.
     *
     * Required workflow: call "listTaskLists", "listTags" or "listPlaces" first to turn a name the
     * user said into an identifier.
     *
     * @param taskIds Only these tasks, by identifier. Narrows alongside every other filter here,
     *   so a known set can be re-read in one call.
     * @param matches Regular expression the task's own text must contain. Java syntax,
     *   unanchored - use ^...$ to match a whole field. Case-insensitive unless matchCase is set.
     *   Does not match tag, list or place names; use the identifier filters for those.
     * @param matchFields Which fields matches is tested against: "title", "notes", or both.
     *   Defaults to both.
     * @param matchCase Make matches case-sensitive.
     * @param due "overdue" is what the app calls overdue: a timed task past its time, an all-day
     *   task past its day. "no_due_date" is tasks without one. For today, tomorrow or this week,
     *   use dueAfter and dueBefore.
     * @param status Which tasks to return: "open", "completed" or "any". Defaults to open.
     * @param listIds Only tasks on these lists.
     * @param tagIds Only tasks carrying these tags.
     * @param placeIds Only tasks filed at these places.
     * @param priorities Only these priorities: "high", "medium", "low" or "none".
     * @param parentIds Only direct subtasks of these tasks - one call expands a whole level of the
     *   tree, and each task names its own parentId. 0 means top-level, so [0] is top-level only,
     *   and mixing 0 with a real identifier unions the two rather than acting as a wildcard.
     * @param dueBefore Only tasks due strictly before this.
     * @param dueAfter Only tasks due strictly after this.
     * @param startBefore Only tasks starting strictly before this.
     * @param startAfter Only tasks starting strictly after this.
     * @param completedBefore Only tasks completed strictly before this. Implies status
     *   "completed" unless you set one.
     * @param completedAfter Only tasks completed strictly after this. Implies status
     *   "completed" unless you set one.
     * @param createdBefore Only tasks created strictly before this.
     * @param createdAfter Only tasks created strictly after this.
     * @param modifiedBefore Only tasks last changed strictly before this.
     * @param modifiedAfter Only tasks last changed strictly after this.
     * @param sort Sort key: "due", "start", "created", "modified", "priority" or "title".
     *   "priority" ascending puts high first.
     * @param sortDesc Reverse the sort.
     * @param limit How many tasks to return at most. Omit for the default of 100;
     *   anything above 1000 is capped.
     * @param offset How many matching tasks to skip, for paging. Follow the page's offset.
     * @return A page of matching tasks, with the total number that matched.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listTasks(
        taskIds: LongArray? = null,
        matches: String? = null,
        matchFields: List<String>? = null,
        matchCase: Boolean? = null,
        due: String? = null,
        status: String? = null,
        listIds: LongArray? = null,
        tagIds: LongArray? = null,
        placeIds: LongArray? = null,
        priorities: List<String>? = null,
        parentIds: LongArray? = null,
        dueBefore: LocalDateTime? = null,
        dueAfter: LocalDateTime? = null,
        startBefore: LocalDateTime? = null,
        startAfter: LocalDateTime? = null,
        completedBefore: LocalDateTime? = null,
        completedAfter: LocalDateTime? = null,
        createdBefore: LocalDateTime? = null,
        createdAfter: LocalDateTime? = null,
        modifiedBefore: LocalDateTime? = null,
        modifiedAfter: LocalDateTime? = null,
        sort: String? = null,
        sortDesc: Boolean? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): AppTaskPage = io {
        val page = engine.findTasks(
            TaskQuery(
                ids = taskIds?.toList().orEmpty(),
                listIds = listIds?.toList().orEmpty(),
                tagIds = tagIds?.toList().orEmpty(),
                placeIds = placeIds?.toList().orEmpty(),
                priorities = priorities.orEmpty(),
                parentIds = parentIds?.toList().orEmpty(),
                status = status,
                due = due,
                matches = matches,
                matchCase = matchCase == true,
                matchFields = matchFields.orEmpty(),
                dueBefore = dueBefore?.asEpochMillis(),
                dueAfter = dueAfter?.asEpochMillis(),
                startBefore = startBefore?.asEpochMillis(),
                startAfter = startAfter?.asEpochMillis(),
                completedBefore = completedBefore?.asEpochMillis(),
                completedAfter = completedAfter?.asEpochMillis(),
                createdBefore = createdBefore?.asEpochMillis(),
                createdAfter = createdAfter?.asEpochMillis(),
                modifiedBefore = modifiedBefore?.asEpochMillis(),
                modifiedAfter = modifiedAfter?.asEpochMillis(),
                sort = sort,
                sortDesc = sortDesc == true,
                limit = limit,
                offset = offset,
            )
        )
        AppTaskPage(page.rows.map { it.toAppTask() }, page.total, page.offset, page.hasMore)
    }

    /**
     * Create one or more tasks. Use "setTaskTags" and "setTaskReminders" afterwards to tag them
     * or add reminders.
     *
     * Titles are stored verbatim. Unlike the app's quick-add box, this does not parse dates, tags
     * or priority out of the text.
     * Required workflow: call "listTaskLists" first when the user names a list.
     *
     * @param tasks The tasks to create, in order. At most 50. The batch runs as one
     *   transaction, so a batch that is refused writes nothing at all.
     * @return The created tasks with the identifiers assigned to them, and which of them landed
     *   on a parent's list instead of the one they asked for.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createTasks(tasks: List<NewTask>): AppTaskCreation = io {
        engine.createTasks(writer, tasks.map { it.toTaskWrite() }).toAppTaskCreation()
    }

    /**
     * Change one or more existing tasks. Only the fields present on a patch are written.
     *
     * Omitting an all-day flag keeps the task's current all-day-ness, so a date can be changed on
     * its own without turning a timed task into an all-day one.
     *
     * Each task may appear once per batch; merge the changes if you have two for the same task.
     *
     * @param updates The changes to apply. At most 50. The batch runs as one transaction, so a
     *   batch that is refused writes nothing at all.
     * @return The tasks as they are after the change, which entries changed nothing, and which
     *   tasks moved to a parent's list instead of the one they asked for.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun updateTasks(updates: List<TaskPatch>): AppTaskUpdate = io {
        engine.updateTasks(writer, updates.map { TaskUpdate(it.id, it.toTaskWrite()) }).toAppTaskUpdate()
    }

    /**
     * Complete tasks, or reopen them.
     *
     * There is one task per recurring series, so completing it advances that task to its next
     * occurrence instead of leaving it completed. The exception is an account whose server
     * advances repeats itself (repeatsOnServer in "listAccounts") - there completion sticks.
     *
     * @param taskIds Identifiers of the tasks to complete. At most 50.
     * @param completed False to reopen the tasks instead. Completes when omitted.
     * @param completedAt When they were completed. Defaults to now.
     * @return The tasks afterwards, which of them advanced to a next occurrence, and which
     *   identifiers matched nothing.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun completeTasks(
        taskIds: LongArray,
        completed: Boolean? = null,
        completedAt: LocalDateTime? = null,
    ): AppCompletion = io {
        engine.completeTasks(
            writer,
            ids = taskIds.toList(),
            completed = completed != false,
            completedAt = completedAt?.asEpochMillis(),
        ).toAppCompletion()
    }

    /**
     * DESTRUCTIVE. Permanently delete a task and every subtask under it.
     *
     * There is no undo and nothing is moved to a trash; deleting a recurring task deletes the
     * whole series. To clear a task from the user's list without losing it, use "completeTasks"
     * instead.
     *
     * @param taskId Identifier of the task to delete.
     * @return Whether the task was deleted, with alsoAffected counting the subtasks deleted with
     *   it.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun deleteTask(taskId: Long): AppDeletion = io {
        engine.deleteTask(writer, taskId).toAppDeletion()
    }

    /**
     * List the task lists on this device, including which of them can be written to.
     *
     * @param listIds Only these lists, by identifier.
     * @param accountIds Only lists on these accounts.
     * @param access Only lists with this access: "owner", "read_write" or "read_only".
     * @param limit How many lists to return at most. Omit for the default of 100;
     *   anything above 1000 is capped.
     * @param offset How many lists to skip, for paging.
     * @return A page of lists, with the total number that exist.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listTaskLists(
        listIds: LongArray? = null,
        accountIds: LongArray? = null,
        access: List<String>? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): AppTaskListPage = io {
        engine.findLists(
            ListQuery(
                ids = listIds?.toList().orEmpty(),
                accountIds = accountIds?.toList().orEmpty(),
                access = access.orEmpty(),
                limit = limit,
                offset = offset,
            )
        ).let { AppTaskListPage(it.rows.map { row -> row.toAppTaskList() }, it.total, it.offset, it.hasMore) }
    }

    /**
     * Create a task list.
     *
     * On a remote account (CalDAV, Google, Microsoft, Etebase) this performs a network round trip
     * to the server and can take several seconds. It is also the one create here that is not
     * idempotent - if it fails ambiguously, call "listTaskLists" and match on title before trying
     * again.
     *
     * @param title What to call the list.
     * @param accountId Identifier of the account to create it under. Required - call
     *   "listAccounts" first.
     * @param color ARGB colour integer.
     * @param icon Icon name: a Material Symbols name in snake_case, such as beach_access, luggage,
     *   flight_takeoff, shopping_cart or work. An unrecognised name is stored but renders as no
     *   icon.
     * @return The created list.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createList(
        title: String,
        accountId: Long,
        color: Int? = null,
        icon: String? = null,
    ): AppTaskList = io {
        engine.createList(
            writer,
            ListWrite(title = title, accountId = accountId, color = color, icon = icon),
        ).toAppTaskList()
    }

    /**
     * Rename or restyle a task list. Only the fields you send change. On a remote account this
     * reaches the server and can take several seconds.
     *
     * @param listId Identifier of the list to change.
     * @param title The new name. Cannot be empty.
     * @param color ARGB colour integer. 0 clears it.
     * @param icon Icon name: a Material Symbols name in snake_case, such as beach_access, luggage,
     *   flight_takeoff, shopping_cart or work. An unrecognised name is stored but renders as no
     *   icon. Empty string clears it.
     * @return The list as it is after the change.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun updateList(
        listId: Long,
        title: String? = null,
        color: Int? = null,
        icon: String? = null,
    ): AppTaskList = io {
        engine.changeList(writer, listId, ListWrite(title = title, color = color, icon = icon))
            .toAppTaskList()
    }

    /**
     * DESTRUCTIVE. Permanently delete a task list AND EVERY TASK ON IT.
     *
     * This is the most damaging function here - it destroys far more than "deleteTask". There is
     * no undo, and on a synced account the deletion is pushed to the server, so the list also
     * disappears from the user's other devices. "listTasks" with listIds and limit 0 counts the
     * tasks that will go without fetching them.
     *
     * @param listId Identifier of the list to delete.
     * @return Whether the list was deleted, with alsoAffected counting the tasks deleted with it.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun deleteList(listId: Long): AppDeletion = io {
        engine.deleteList(writer, listId).toAppDeletion()
    }

    /**
     * List the tags on this device.
     *
     * @param tagIds Only these tags, by identifier.
     * @param limit How many tags to return at most. Omit for the default of 100;
     *   anything above 1000 is capped.
     * @param offset How many tags to skip, for paging.
     * @return A page of tags, with the total number that exist.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listTags(
        tagIds: LongArray? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): AppTagPage =
        io {
            engine.findTags(TagQuery(tagIds?.toList().orEmpty(), limit, offset))
                .let { AppTagPage(it.rows.map { row -> row.toAppTag() }, it.total, it.offset, it.hasMore) }
        }

    /**
     * Create a tag, or resolve one that already exists.
     *
     * Names are unique case-insensitively, so a name that already exists comes back unchanged
     * rather than duplicated - which makes this the way to turn a tag name into an identifier.
     *
     * @param name What to call the tag.
     * @param color ARGB colour integer. Ignored if the tag already exists.
     * @param icon Icon name: a Material Symbols name in snake_case, such as beach_access, luggage,
     *   flight_takeoff, shopping_cart or work. An unrecognised name is stored but renders as no
     *   icon. Ignored if the tag already exists.
     * @return The tag, and whether it was created or already existed.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createTag(name: String, color: Int? = null, icon: String? = null): AppTagCreation =
        io {
            engine.createTag(writer, TagWrite(name = name, color = color, icon = icon)).toAppTagCreation()
        }

    /**
     * Rename or restyle a tag. Only the fields you send change. Renaming onto a name another tag
     * already holds fails - there is no merge; move the tasks with "setTaskTags" and delete the
     * emptied tag instead.
     *
     * @param tagId Identifier of the tag to change.
     * @param name The new name. Cannot be empty.
     * @param color ARGB colour integer. 0 clears it.
     * @param icon Icon name: a Material Symbols name in snake_case, such as beach_access, luggage,
     *   flight_takeoff, shopping_cart or work. An unrecognised name is stored but renders as no
     *   icon. Empty string clears it.
     * @return The tag as it is after the change.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun updateTag(
        tagId: Long,
        name: String? = null,
        color: Int? = null,
        icon: String? = null,
    ): AppTag = io {
        engine.changeTag(writer, tagId, TagWrite(name = name, color = color, icon = icon)).toAppTag()
    }

    /**
     * DESTRUCTIVE. Permanently delete a tag and strip it from every task carrying it.
     *
     * There is no undo, and no merge - to move its tasks onto another tag first, use
     * "setTaskTags". The tasks themselves survive. To take a tag off one task, use "setTaskTags"
     * with removeTagIds instead.
     *
     * @param tagId Identifier of the tag to delete.
     * @return Whether the tag was deleted, with alsoAffected counting the tasks that lost it.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun deleteTag(tagId: Long): AppDeletion = io {
        engine.deleteTag(writer, tagId).toAppDeletion()
    }

    /**
     * Add and/or remove tags across one or more tasks. Tags you do not mention are left alone.
     *
     * Every tag in addTagIds goes on every task in taskIds, and every tag in removeTagIds comes
     * off every one of them. Adding a tag a task already carries is harmless.
     * Required workflow: call "createTag" first to turn tag names into identifiers - it resolves
     * an existing name rather than duplicating it.
     *
     * @param taskIds Identifiers of the tasks to change. At most 50.
     * @param addTagIds Tags to add to every one of them.
     * @param removeTagIds Tags to remove from every one of them.
     * @return The tasks as they are after the change, and what each one gained and lost.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun setTaskTags(
        taskIds: LongArray,
        addTagIds: LongArray? = null,
        removeTagIds: LongArray? = null,
    ): AppTagChange = io {
        engine.setTaskTags(
            writer,
            taskIds.toList(),
            addTagIds?.toList().orEmpty(),
            removeTagIds?.toList().orEmpty(),
        ).toAppTagChange()
    }

    /**
     * List the saved places on this device. Use these for location reminders.
     *
     * @param placeIds Only these places, by identifier.
     * @param limit How many places to return at most. Omit for the default of 100;
     *   anything above 1000 is capped.
     * @param offset How many places to skip, for paging.
     * @return A page of places, with the total number that exist.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listPlaces(
        placeIds: LongArray? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): AppPlacePage =
        io {
            engine.findPlaces(PlaceQuery(placeIds?.toList().orEmpty(), limit, offset))
                .let { AppPlacePage(it.rows.map { row -> row.toAppPlace() }, it.total, it.offset, it.hasMore) }
        }

    /**
     * Save a place, so tasks can be filed at it and location reminders can point at it.
     *
     * There is no geocoding here, so coordinates are required and an address alone is not enough.
     * Places are unique by coordinate, so repeating this returns the existing place.
     *
     * @param latitude Latitude in degrees, -90 to 90.
     * @param longitude Longitude in degrees, -180 to 180.
     * @param name What to call the place, for example "Home". Falls back to the address or the
     *   coordinates when omitted.
     * @param address Street address, when known.
     * @param phone Phone number.
     * @param url URL.
     * @param radius Trigger radius in metres for location reminders, more than 0. Defaults to 250.
     * @param color ARGB colour integer.
     * @param icon Icon name: a Material Symbols name in snake_case, such as beach_access, luggage,
     *   flight_takeoff, shopping_cart or work. An unrecognised name is stored but renders as no
     *   icon.
     * @return The place, and whether it was created or already existed.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createPlace(
        latitude: Double,
        longitude: Double,
        name: String? = null,
        address: String? = null,
        phone: String? = null,
        url: String? = null,
        radius: Int? = null,
        color: Int? = null,
        icon: String? = null,
    ): AppPlaceCreation = io {
        engine.createPlace(
            writer,
            PlaceWrite(
                name = name,
                latitude = latitude,
                longitude = longitude,
                address = address,
                phone = phone,
                url = url,
                radius = radius,
                color = color,
                icon = icon,
            ),
        ).toAppPlaceCreation()
    }

    /**
     * Change a saved place. Only the fields you send change. Coordinates are insert-only: a place
     * somewhere else is a different place, so create that one instead.
     *
     * @param placeId Identifier of the place to change.
     * @param name The new name. Empty string clears it.
     * @param address Street address.
     * @param phone Phone number.
     * @param url URL.
     * @param radius Trigger radius in metres for location reminders, more than 0.
     * @param color ARGB colour integer. 0 clears it.
     * @param icon Icon name: a Material Symbols name in snake_case, such as beach_access, luggage,
     *   flight_takeoff, shopping_cart or work. An unrecognised name is stored but renders as no
     *   icon. Empty string clears it.
     * @return The place as it is after the change.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun updatePlace(
        placeId: Long,
        name: String? = null,
        address: String? = null,
        phone: String? = null,
        url: String? = null,
        radius: Int? = null,
        color: Int? = null,
        icon: String? = null,
    ): AppPlace = io {
        engine.changePlace(
            writer,
            placeId,
            PlaceWrite(
                name = name,
                address = address,
                phone = phone,
                url = url,
                radius = radius,
                color = color,
                icon = icon,
            ),
        ).toAppPlace()
    }

    /**
     * DESTRUCTIVE. Permanently delete a saved place and every location reminder using it.
     *
     * There is no undo. Tasks filed at the place survive but stop being location-aware, and any
     * arrival or departure reminder on them is gone.
     *
     * @param placeId Identifier of the place to delete.
     * @return Whether the place was deleted, with alsoAffected counting the tasks unfiled by it.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun deletePlace(placeId: Long): AppDeletion = io {
        engine.deletePlace(writer, placeId).toAppDeletion()
    }

    /**
     * List reminders, optionally narrowed to particular tasks, kinds or places.
     *
     * @param taskIds Only reminders on these tasks.
     * @param types Only these kinds, for example "location_arrival" or "date_time".
     * @param placeIds Only location reminders pointing at these places.
     * @param limit How many reminders to return at most. Omit for the default of 100;
     *   anything above 1000 is capped.
     * @param offset How many reminders to skip, for paging.
     * @return A page of reminders, both time and location.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listReminders(
        taskIds: LongArray? = null,
        types: List<String>? = null,
        placeIds: LongArray? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): AppReminderPage = io {
        engine.findReminders(
            ReminderQuery(
                taskIds = taskIds?.toList().orEmpty(),
                types = types.orEmpty(),
                placeIds = placeIds?.toList().orEmpty(),
                limit = limit,
                offset = offset,
            )
        ).let { AppReminderPage(it.rows.map { row -> row.toAppReminder() }, it.total, it.offset, it.hasMore) }
    }

    /**
     * Add and/or remove reminders on a task. Reminders you do not mention are left alone.
     *
     * A task has one place, and every location reminder on it uses that place. Adding the first
     * one files the task there; asking for a different place is rejected, so move the task with
     * "updateTasks" first. Deleting a location reminder leaves the task filed at the place - to
     * unfile it entirely, set placeId to 0 with "updateTasks".
     *
     * Arrival and departure are independent reminders, so ask for both to cover either direction;
     * adding one never disturbs the other, and repeating an add is harmless.
     * Required workflow: call "listPlaces" first to turn a place name into an identifier.
     *
     * @param taskId Identifier of the task to change.
     * @param add Reminders to add. Reminders already on the task are left alone.
     * @param removeReminderIds Identifiers of reminders to delete. Get them from "listReminders".
     * @return The task's reminders after the change, with what was added, how many went, and the
     *   task itself - adding the first location reminder files it at that place.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun setTaskReminders(
        taskId: Long,
        add: List<NewReminder>? = null,
        removeReminderIds: LongArray? = null,
    ): AppReminderChange = io {
        engine.setTaskReminders(
            writer,
            taskId = taskId,
            add = add.orEmpty().map { it.toReminderWrite() },
            removeReminderIds = removeReminderIds?.toList().orEmpty(),
        ).toAppReminderChange()
    }

    /**
     * List the sync accounts on this device. Read this to name the account behind a list, to tell
     * the user an account is logged out (non-null error), or to check repeatsOnServer before
     * reasoning about a recurring task.
     *
     * @param accountIds Only these accounts, by identifier.
     * @param limit How many accounts to return at most. Omit for the default of 100;
     *   anything above 1000 is capped.
     * @param offset How many accounts to skip, for paging.
     * @return A page of accounts, including whether each server advances recurring tasks itself
     *   and any sync error it is reporting.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listAccounts(
        accountIds: LongArray? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): AppAccountPage =
        io {
            engine.findAccounts(AccountQuery(accountIds?.toList().orEmpty(), limit, offset))
                .let { AppAccountPage(it.rows.map { row -> row.toAppAccount() }, it.total, it.offset, it.hasMore) }
        }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        try {
            analytics.logEventOncePerDay(AnalyticsEvents.APP_FUNCTION_CALL)
            block()
        } catch (e: AppFunctionException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw e.toAppFunctionException()
        }
    }
}
