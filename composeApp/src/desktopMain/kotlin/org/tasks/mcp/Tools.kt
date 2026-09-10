package org.tasks.mcp

import org.tasks.api.AccountQuery
import org.tasks.api.ApiErrors
import org.tasks.api.ApiPage
import org.tasks.api.ReminderQuery
import org.tasks.api.ReminderWrite
import org.tasks.api.DUE_FILTERS
import org.tasks.api.Deletion
import org.tasks.api.ListQuery
import org.tasks.api.ListWrite
import org.tasks.api.MAX_BATCH
import org.tasks.api.PlaceQuery
import org.tasks.api.PlaceWrite
import org.tasks.api.TASK_STATUSES
import org.tasks.api.TagQuery
import org.tasks.api.TagWrite
import org.tasks.api.TaskQuery
import org.tasks.api.TaskRow
import org.tasks.api.TaskText
import org.tasks.api.TaskUpdate
import org.tasks.api.TaskWrite
import org.tasks.api.TasksContract
import org.tasks.api.pageLimit
import org.tasks.api.pageOffset
import io.modelcontextprotocol.kotlin.sdk.server.Server
import org.tasks.analytics.Analytics
import org.tasks.analytics.AnalyticsEvents
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

private val json = Json {
    prettyPrint = true
    encodeDefaults = false
    explicitNulls = false
}

private const val MAX_ROWS_BYTES = 32 * 1024

private const val ICON = "Icon name: a Material Symbols name in snake_case, such as beach_access, " +
    "luggage, flight_takeoff, shopping_cart or work. An unrecognised name is stored but renders " +
    "as no icon."

private const val ICON_CLEARS = "$ICON Empty string clears it."

private const val COLOR = "ARGB colour integer."


private val TASK_FIELDS = ApiTask.serializer().descriptor.fieldNames()
private val REMINDER_FIELDS = ApiReminder.serializer().descriptor.fieldNames()
private val LIST_FIELDS = ApiList.serializer().descriptor.fieldNames()
private val TAG_FIELDS = ApiTag.serializer().descriptor.fieldNames()
private val PLACE_FIELDS = ApiPlace.serializer().descriptor.fieldNames()
private val ACCOUNT_FIELDS = ApiAccount.serializer().descriptor.fieldNames()

fun Server.registerTasksTools(
    api: DatabaseTasksApi,
    permissions: ToolPermissions,
    log: ActivityLog,
    analytics: Analytics? = null,
) {
    val reg = Registration(log, ToolUsage(analytics, permissions), permissions)
    registerReadTools(api, reg)
    registerWriteTools(api, reg)
}

private class Registration(
    val log: ActivityLog,
    val usage: ToolUsage,
    val permissions: ToolPermissions,
)

internal class ToolUsage(
    private val analytics: Analytics?,
    private val permissions: ToolPermissions,
) {
    suspend fun record() {
        analytics?.logEventOncePerDay(
            event = AnalyticsEvents.MCP_TOOL_CALL,
            AnalyticsEvents.PARAM_ACCESS to when (permissions.mode) {
                AccessMode.ReadOnly -> "read_only"
                AccessMode.ReadWrite -> "read_write"
                AccessMode.Advanced -> "advanced"
            },
        )
    }
}

private fun Server.registerReadTools(api: DatabaseTasksApi, reg: Registration) {

    tool(
        reg, "list_tasks",
        title = "List tasks",
        description = """
            Search and filter tasks. This is the main read tool.

            Date ranges are strictly exclusive (before = <, after = >).
        """.trimIndent(),
        readOnly = true,
        schema = schema {
            integerArray("task_ids", "Only these tasks, by id. Narrows alongside every other filter here, so a known set can be re-read in one call.")
            string("matches", "Regular expression the task's own text must contain. Java/Kotlin syntax, unanchored - use ^...$ to match a whole field. Case-insensitive unless match_case is set. Does not match tag, list or place names; use the id filters for those.")
            stringArray("match_fields", "Which of the task's own fields `matches` is tested against. Defaults to both.", enum = TaskText.NAMES)
            boolean("match_case", "Make `matches` case-sensitive.", default = false)
            string("due", "'overdue' is what the app calls overdue: a timed task past its time, an all-day task past its day. 'no_due_date' is tasks without one. For today, tomorrow or this week, use due_after and due_before.", enum = DUE_FILTERS)
            string(
                "status",
                "Which tasks to return. Defaults to open.",
                enum = TASK_STATUSES,
            )
            integerArray("list_ids", "Only tasks on these lists. Get ids from list_task_lists.")
            integerArray("tag_ids", "Only tasks carrying these tags. Get ids from list_tags.")
            integerArray("place_ids", "Only tasks filed at these places. Get ids from list_places.")
            stringArray("priorities", "Only these priorities.", enum = TasksContract.Tasks.PRIORITIES)
            integerArray("parent_ids", "Only direct children of these tasks - one call expands a whole level of the tree, and each row names its own parent_id. 0 means top-level, so [0] is top-level only and mixing 0 with a real id unions the two rather than acting as a wildcard.")
            timestamp("due_before", "Tasks due strictly before this.")
            timestamp("due_after", "Tasks due strictly after this. Pass 0 to mean 'has a due date'.")
            timestamp("start_before", "Tasks starting strictly before this.")
            timestamp("start_after", "Tasks starting strictly after this. Pass 0 to mean 'has a start date'.")
            timestamp("completed_before", "Tasks completed strictly before this. Implies status=completed unless you set one.")
            timestamp("completed_after", "Tasks completed strictly after this. Implies status=completed unless you set one.")
            timestamp("created_before", "Tasks created strictly before this.")
            timestamp("created_after", "Tasks created strictly after this.")
            timestamp("modified_before", "Tasks whose row last changed before this.")
            timestamp("modified_after", "Tasks whose row changed after this.")
            string("sort", "Sort key. 'priority' ascending puts high first.", enum = TasksContract.Tasks.SORTS)
            boolean("sort_desc", "Reverse the sort.", default = false)
            paging("task")
            fields("task", TASK_FIELDS)
        },
    ) { args ->
        val projection = args.projection(TASK_FIELDS)
        val page = api.queryTasks(
            TaskQuery(
                ids = args.longs("task_ids"),
                listIds = args.longs("list_ids"),
                tagIds = args.longs("tag_ids"),
                placeIds = args.longs("place_ids"),
                priorities = args.strings("priorities"),
                parentIds = args.longs("parent_ids"),
                status = args.string("status"),
                due = args.string("due"),
                matches = args.string("matches"),
                matchCase = args.boolean("match_case") ?: false,
                matchFields = args.strings("match_fields"),
                dueBefore = args.date("due_before"),
                dueAfter = args.date("due_after"),
                startBefore = args.date("start_before"),
                startAfter = args.date("start_after"),
                completedBefore = args.date("completed_before"),
                completedAfter = args.date("completed_after"),
                createdBefore = args.date("created_before"),
                createdAfter = args.date("created_after"),
                modifiedBefore = args.date("modified_before"),
                modifiedAfter = args.date("modified_after"),
                sort = args.string("sort"),
                sortDesc = args.boolean("sort_desc") ?: false,
                limit = args.int("limit"),
                offset = args.int("offset"),
            )
        )
        page.toResult("task", projection, { it.toApiTask() }) { sent -> "$sent of ${page.total} task(s)" }
    }

    tool(
        reg, "list_task_lists",
        title = "List task lists",
        description = "Every task list, with the id of the account it belongs to and " +
            "whether it is read-only. Lists are the top-level container - a task belongs " +
            "to exactly one. Name the account with list_accounts.",
        readOnly = true,
        schema = schema {
            integerArray("list_ids", "Only these lists, by id.")
            integerArray("account_ids", "Only lists on these accounts. See list_accounts.")
            stringArray("access", "Only lists at these access levels.", enum = TasksContract.Lists.ACCESS_LEVELS)
            paging("list")
            fields("list", LIST_FIELDS)
        },
    ) { args ->
        val projection = args.projection(LIST_FIELDS)
        val page = api.queryLists(
            ListQuery(
                ids = args.longs("list_ids"),
                accountIds = args.longs("account_ids"),
                access = args.strings("access"),
                limit = args.int("limit"),
                offset = args.int("offset"),
            )
        )
        page.toResult("list", projection, { it.toApiList() }) { sent -> "$sent of ${page.total} list(s)" }
    }

    tool(
        reg, "list_tags",
        title = "List tags",
        description = "Every tag. Tags are many-to-many with tasks; a task row carries " +
            "tag_ids, and set_task_tags edits them.",
        readOnly = true,
        schema = schema {
            integerArray("tag_ids", "Only these tags, by id.")
            paging("tag")
            fields("tag", TAG_FIELDS)
        },
    ) { args ->
        val projection = args.projection(TAG_FIELDS)
        val page = api.queryTags(
            TagQuery(args.longs("tag_ids"), args.int("limit"), args.int("offset"))
        )
        page.toResult("tag", projection, { it.toApiTag() }) { sent -> "$sent of ${page.total} tag(s)" }
    }

    tool(
        reg, "list_places",
        title = "List saved places",
        description = "Every saved location, with coordinates and geofence radius. A " +
            "task can be filed at one place; whether that also reminds the user on " +
            "arrival or departure is a separate flag pair - see list_reminders.",
        readOnly = true,
        schema = schema {
            integerArray("place_ids", "Only these places, by id.")
            paging("place")
            fields("place", PLACE_FIELDS)
        },
    ) { args ->
        val projection = args.projection(PLACE_FIELDS)
        val page = api.queryPlaces(
            PlaceQuery(args.longs("place_ids"), args.int("limit"), args.int("offset"))
        )
        page.toResult("place", projection, { it.toApiPlace() }) { sent -> "$sent of ${page.total} place(s)" }
    }

    tool(
        reg, "list_accounts",
        title = "List sync accounts",
        description = "Sync accounts (CalDAV, Google Tasks, Microsoft, local, ...). " +
            "Read this to name the account behind a list, to tell the user an account " +
            "is logged out (non-empty `error`), or to check `repeats_on_server` before " +
            "reasoning about a recurring task.",
        readOnly = true,
        schema = schema {
            integerArray("account_ids", "Only these accounts, by id.")
            paging("account")
            fields("account", ACCOUNT_FIELDS)
        },
    ) { args ->
        val projection = args.projection(ACCOUNT_FIELDS)
        val page = api.queryAccounts(
            AccountQuery(args.longs("account_ids"), args.int("limit"), args.int("offset"))
        )
        page.toResult("account", projection, { it.toApiAccount() }) { sent -> "$sent of ${page.total} account(s)" }
    }

    tool(
        reg, "list_reminders",
        title = "List reminders",
        description = "Reminders attached to tasks. Both kinds live here: time reminders " +
            "that fire on a clock, and location reminders that fire on arriving at or " +
            "leaving the task's place, named by place_id - see list_places.",
        readOnly = true,
        schema = schema {
            integerArray("task_ids", "Only reminders on these tasks.")
            stringArray("types", "Only these reminder types.", enum = TasksContract.Reminders.TYPES)
            integerArray("place_ids", "Only location reminders at these places.")
            paging("reminder")
            fields("reminder", REMINDER_FIELDS)
        },
    ) { args ->
        val projection = args.projection(REMINDER_FIELDS)
        val page = api.queryReminders(
            ReminderQuery(
                taskIds = args.longs("task_ids"),
                types = args.strings("types"),
                placeIds = args.longs("place_ids"),
                limit = args.int("limit"),
                offset = args.int("offset"),
            )
        )
        page.toResult("reminder", projection, { it.toApiReminder() }) { sent -> "$sent of ${page.total} reminder(s)" }
    }
}

private fun Server.registerWriteTools(api: DatabaseTasksApi, reg: Registration) {

    tool(
        reg, "create_tasks",
        title = "Create tasks",
        description = """
            Create tasks. A batch that is refused writes nothing at all. Use set_task_tags
            and set_task_reminders afterwards to tag them or add reminders.

            Titles are stored verbatim. Unlike the app's quick-add box, this does not parse
            dates, tags or priority out of the text.
        """.trimIndent(),
        readOnly = false,
        schema = schema {
            objectArray("tasks", "The tasks to create, in order. At most $MAX_BATCH.") {
                newTaskFields()
            }
        },
    ) { args ->
        val creation = api.createTasks(args.objects("tasks").map { it.toTaskWrite() })
        val body = buildJsonObject {
            put("created_task_ids", JsonArray(creation.ids.map { JsonPrimitive(it) }))
            putEcho("tasks", creation.tasks)
            val overridden = creation.movedToParentListIds.size
            if (overridden > 0) {
                put("moved_to_parent_list_task_ids", json.encodeToJsonElement(creation.movedToParentListIds))
                put(
                    "note",
                    "$overridden task(s) were created on their parent's list, not the list_id " +
                        "sent: a subtask lives on its parent's list, so parent_id decides. Send " +
                        "parent_id on its own, or parent_id 0 with a list_id for a top-level task.",
                )
            }
        }
        body.toString() to "created ${creation.ids.size} task(s)"
    }

    tool(
        reg, "update_tasks",
        title = "Update tasks",
        description = """
            Patch up to $MAX_BATCH tasks in one transaction. Only the fields you send
            change; a field's empty value (empty string, or 0 for a date) clears it. Title
            cannot be cleared - an empty title is rejected. An omitted due_all_day keeps
            the task's current all-day-ness, so a date can be patched on its own, and the
            flag can be flipped on its own too.

            Use complete_tasks to complete or uncomplete; that is not a plain field write.

            If one entry is malformed the whole batch is refused and nothing is written, so
            there is no half-applied state to work out - fix the entry and send it again.

            An entry naming a task that no longer exists is not that kind of failure. It
            writes nothing and reports rows_changed 0 for that entry while the rest still
            apply. Check `results` per task rather than the total.

            Each task may appear once per batch; merge the changes if you have two for the
            same task.
        """.trimIndent(),
        readOnly = false,
        schema = schema {
            objectArray("updates", "The patches to apply, in order.") {
                integer("task_id", "The task to change.", required = true)
                taskPatchFields()
            }
        },
    ) { args ->
        val updates = args.objects("updates").map { it.toTaskUpdate() }
        val revision = api.updateTasks(updates)
        val changed = revision.rowsChanged
        val unchanged = revision.unchangedIds
        val body = buildJsonObject {
            put("rows_changed", changed.sum())
            put(
                "results",
                JsonArray(
                    updates.zip(changed).map { (update, rows) ->
                        buildJsonObject {
                            put("task_id", update.id)
                            put("rows_changed", rows)
                        }
                    }
                ),
            )
            if (unchanged.isNotEmpty()) {
                put("unchanged_task_ids", JsonArray(unchanged.map { JsonPrimitive(it) }))
                put(
                    "hint",
                    "${unchanged.size} of ${updates.size} entries changed nothing, which means " +
                        "those tasks no longer exist. Every other entry did apply - do not " +
                        "resend them.",
                )
            }
            val overridden = revision.movedToParentListIds.size
            if (overridden > 0) {
                put("moved_to_parent_list_task_ids", json.encodeToJsonElement(revision.movedToParentListIds))
                put(
                    "note",
                    "$overridden task(s) moved to their new parent's list, not the list_id " +
                        "sent: a subtask lives on its parent's list, so parent_id decides.",
                )
            }
            val detached = revision.detachedIds.size
            if (detached > 0) {
                put("detached_task_ids", json.encodeToJsonElement(revision.detachedIds))
                put(
                    "subtasks_note",
                    "$detached subtask(s) became top-level tasks on the new list: a subtask lives " +
                        "on its parent's list, so moving one alone takes it out from under its " +
                        "parent. Send parent_id as well to put it under a task there.",
                )
            }
            putEcho("tasks", revision.tasks)
        }
        body.toString() to "updated ${changed.count { it > 0 }} of ${updates.size} task(s)"
    }

    tool(
        reg, "complete_tasks",
        title = "Complete or uncomplete tasks",
        description = """
            Complete tasks, or uncomplete them with completed=false. Up to $MAX_BATCH in
            one transaction.

            There is one row per recurring series, so completing one advances that row to
            its next occurrence instead of leaving it completed. `results` marks those
            entries advanced=true; do not retry them, because retrying advances the series
            again. The exception is an account whose server advances repeats itself
            (repeats_on_server=true in list_accounts) - there completion sticks.
        """.trimIndent(),
        readOnly = false,
        schema = schema {
            integerArray("task_ids", "The tasks to complete. At most $MAX_BATCH.")
            boolean("completed", "false to uncomplete. Applies to every id you send.", default = true)
            timestamp("completed_at", "Completion time. Defaults to now.")
        },
    ) { args ->
        val sent = args.longs("task_ids")
        val complete = args.boolean("completed") ?: true
        val result = api.completeTasks(sent, complete, args.date("completed_at"))
        val ids = result.taskIds
        val changed = result.rowsChanged
        val advanced = result.advancedTaskIds.toSet()

        val body = buildJsonObject {
            put("rows_changed", changed.sum())
            put(
                "results",
                JsonArray(
                    ids.zip(changed).map { (id, rows) ->
                        buildJsonObject {
                            put("task_id", id)
                            put("rows_changed", rows)
                            if (id in advanced) put("advanced", true)
                        }
                    }
                ),
            )
            if (advanced.isNotEmpty()) {
                put(
                    "note",
                    "${advanced.size} of these repeat, so completing them advanced each series " +
                        "to its next occurrence instead of leaving it completed. That is the " +
                        "expected result - do not retry them.",
                )
            }
            if (result.alsoCompletedTaskIds.isNotEmpty()) {
                put("also_completed_task_ids", json.encodeToJsonElement(result.alsoCompletedTaskIds))
                put(
                    "subtasks_note",
                    "${result.alsoCompletedTaskIds.size} subtask(s) were completed along with " +
                        "their parent.",
                )
            }
            if (result.reopenedTaskIds.isNotEmpty()) {
                put("reopened_task_ids", json.encodeToJsonElement(result.reopenedTaskIds))
                put(
                    "reopened_note",
                    "${result.reopenedTaskIds.size} related task(s) were reopened as well: " +
                        "reopening a task reopens its subtasks, and a task with an open subtask " +
                        "is not complete.",
                )
            }
            putEcho("tasks", result.tasks)
        }
        val verb = if (!complete) "uncompleted" else "completed"
        val note = buildString {
            if (advanced.isNotEmpty()) append(", ${advanced.size} advanced (recurring)")
            if (result.alsoCompletedTaskIds.isNotEmpty()) append(", +${result.alsoCompletedTaskIds.size} subtask(s)")
            if (result.reopenedTaskIds.isNotEmpty()) append(", ${result.reopenedTaskIds.size} parent(s) reopened")
        }
        body.toString() to "$verb ${ids.size} task(s)$note"
    }

    tool(
        reg, "delete_task",
        title = "Delete a task",
        description = "Delete a task AND ALL OF ITS SUBTASKS. There is no undo and nothing " +
            "is moved to a trash; deleting a recurring task deletes the whole series. To clear " +
            "a task from the user's list without losing it, use complete_tasks instead.",
        readOnly = false,
        destructive = true,
        schema = schema {
            integer("task_id", "The task to delete.", required = true)
        },
    ) { args ->
        val id = args.requireLong("task_id")
        val deletion = api.deleteTask(id)
        val childNote = deletion.alsoAffected.takeIf { it > 0 }
        val body = deletion.body("subtasks_also_deleted")
        body.toString() to "deleted task $id${childNote?.let { " (+$it subtask(s))" } ?: ""}"
    }

    tool(
        reg, "delete_task_list",
        title = "Delete a task list",
        description = """
            Delete a list AND EVERY TASK ON IT. This is the most destructive tool here -
            it removes far more than delete_task, which only takes a task's subtasks.

            There is no undo. On a remote account the deletion is pushed to the server,
            so it disappears from the user's other devices too.

            list_tasks with list_ids and limit=0 counts the tasks that will go without
            transferring them.
        """.trimIndent(),
        readOnly = false,
        destructive = true,
        schema = schema {
            integer("list_id", "The list to delete. See list_task_lists.", required = true)
        },
    ) { args ->
        val id = args.requireLong("list_id")
        val deletion = api.deleteList(id)
        val body = deletion.body("tasks_also_deleted")
        body.toString() to "deleted list $id (+${deletion.alsoAffected} task(s))"
    }

    tool(
        reg, "delete_tag",
        title = "Delete a tag",
        description = "Delete a tag and take it off every task carrying it. There is no undo, " +
            "and no merge - to move its tasks onto another tag first, use set_task_tags. The " +
            "tasks themselves survive. To take a tag off one task, use set_task_tags with " +
            "remove_tag_ids instead.",
        readOnly = false,
        destructive = true,
        schema = schema {
            integer("tag_id", "The tag to delete. See list_tags.", required = true)
        },
    ) { args ->
        val id = args.requireLong("tag_id")
        val deletion = api.deleteTag(id)
        val body = deletion.body("tasks_untagged")
        body.toString() to "deleted tag $id (was on ${deletion.alsoAffected} task(s))"
    }

    tool(
        reg, "delete_place",
        title = "Delete a saved place",
        description = "Delete a saved location. Tasks filed there are unfiled and lose " +
            "their arrival and departure reminders, but are not themselves deleted. " +
            "There is no undo.",
        readOnly = false,
        destructive = true,
        schema = schema {
            integer("place_id", "The place to delete. See list_places.", required = true)
        },
    ) { args ->
        val id = args.requireLong("place_id")
        val deletion = api.deletePlace(id)
        val body = deletion.body("tasks_unfiled")
        body.toString() to "deleted place $id (was on ${deletion.alsoAffected} task(s))"
    }

    tool(
        reg, "create_task_list",
        title = "Create a task list",
        description = "Create a new task list on an account. On a remote account " +
            "(CalDAV, Google, Microsoft, Etebase) this performs a network round trip to " +
            "the server and can take several seconds. It is also the one create here " +
            "that is NOT idempotent - if it fails ambiguously, call list_task_lists and " +
            "match on title before trying again.",
        readOnly = false,
        schema = schema {
            integer("account_id", "Account to create the list on. See list_accounts.", required = true)
            string("title", "List name.", required = true)
            integer("color", COLOR)
            string("icon", ICON)
        },
    ) { args ->
        val list = api.createList(
            ListWrite(
                accountId = args.requireLong("account_id"),
                title = args.requireString("title"),
                color = args.int("color"),
                icon = args.string("icon"),
            )
        )
        buildJsonObject { put("list", json.encodeToJsonElement(list.toApiList())) }.toString() to
            "created list ${list.id}"
    }

    tool(
        reg, "update_task_list",
        title = "Rename or restyle a task list",
        description = "Change a list's name, colour or icon. Only the fields you send " +
            "change. On a remote account this reaches the server and can take several " +
            "seconds. Renaming a read-only shared list throws.",
        readOnly = false,
        schema = schema {
            integer("list_id", "The list to change. See list_task_lists.", required = true)
            string("title", "New name. Cannot be empty.")
            integer("color", "$COLOR 0 clears it.")
            string("icon", ICON_CLEARS)
        },
    ) { args ->
        val id = args.requireLong("list_id")
        val list = api.updateList(
            id,
            ListWrite(
                title = args.string("title"),
                color = args.int("color"),
                icon = args.string("icon"),
            )
        )
        buildJsonObject { put("list", json.encodeToJsonElement(list.toApiList())) }.toString() to
            "updated list $id"
    }

    tool(
        reg, "create_tag",
        title = "Create or resolve a tag",
        description = "Create a tag. Names are unique case-insensitively, so creating a " +
            "name that already exists returns the existing tag unchanged - which makes " +
            "this the right way to turn a tag name into an id without checking first.",
        readOnly = false,
        idempotent = true,
        schema = schema {
            string("name", "Tag name.", required = true)
            integer("color", "$COLOR Ignored if the tag already exists.")
            string("icon", "$ICON Ignored if the tag already exists.")
        },
    ) { args ->
        val name = args.requireString("name")
        val (tag, created) = api.createTag(
            TagWrite(name = name, color = args.int("color"), icon = args.string("icon"))
        )
        buildJsonObject {
            put("tag", json.encodeToJsonElement(tag.toApiTag()))
            put("created", created)
            if (!created) {
                put("note", "A tag named '${tag.name}' already existed, so that one was returned and nothing was created.")
            }
        }.toString() to "tag \"$name\" -> ${tag.id}${if (created) "" else " (existing)"}"
    }

    tool(
        reg, "update_tag",
        title = "Rename or restyle a tag",
        description = "Change a tag's name, colour or icon. Only the fields you send " +
            "change. Renaming onto a name another tag already holds throws - there is no " +
            "merge; move the tasks with set_task_tags and delete the emptied tag instead.",
        readOnly = false,
        schema = schema {
            integer("tag_id", "The tag to change. See list_tags.", required = true)
            string("name", "New name. Cannot be empty.")
            integer("color", "$COLOR 0 clears it.")
            string("icon", ICON_CLEARS)
        },
    ) { args ->
        val id = args.requireLong("tag_id")
        val tag = api.updateTag(
            id,
            TagWrite(
                name = args.string("name"),
                color = args.int("color"),
                icon = args.string("icon"),
            )
        )
        buildJsonObject { put("tag", json.encodeToJsonElement(tag.toApiTag())) }.toString() to
            "updated tag $id"
    }

    tool(
        reg, "set_task_tags",
        title = "Add or remove tags on tasks",
        description = """
            Add and/or remove tags across one or more tasks, as one transaction. Tags you do
            not mention are left alone. Every tag in add_tag_ids goes on every task in
            task_ids, and every tag in remove_tag_ids comes off every one of them - which is
            what "tag all of these read-later" is.

            This is one operation over many tasks rather than a batch of different ones, so
            there is nothing per-task to send. Adding a tag a task already carries is
            harmless and is not counted as a change. Use create_tag first to turn names
            into ids.
        """.trimIndent(),
        readOnly = false,
        idempotent = true,
        schema = schema {
            integerArray("task_ids", "The tasks to change. At most $MAX_BATCH.")
            integerArray("add_tag_ids", "Tags to add to every one of them.")
            integerArray("remove_tag_ids", "Tags to remove from every one of them.")
        },
    ) { args ->
        val change = api.setTaskTags(
            args.longs("task_ids"),
            args.longs("add_tag_ids"),
            args.longs("remove_tag_ids"),
        )
        val body = buildJsonObject {
            put("added", change.added)
            put("removed", change.removed)
            put(
                "results",
                JsonArray(
                    change.edits.map {
                        buildJsonObject {
                            put("task_id", it.taskId)
                            put("added", it.added)
                            put("removed", it.removed)
                        }
                    }
                ),
            )
            putEcho("tasks", change.tasks)
        }
        body.toString() to "${change.edits.size} task(s): +${change.added} / -${change.removed} tag(s)"
    }

    tool(
        reg, "create_place",
        title = "Create a saved place",
        description = "Save a location. Coordinates are required - there is no geocoding " +
            "in this API, so an address string alone is not enough. Places are unique by " +
            "coordinate, so repeating this returns the existing place rather than a duplicate.",
        readOnly = false,
        idempotent = true,
        schema = schema {
            number("latitude", "Decimal degrees, -90 to 90.", required = true)
            number("longitude", "Decimal degrees, -180 to 180.", required = true)
            string("name", "What to call the place, e.g. 'Home'. Falls back to the address or the coordinates when omitted.")
            string("address", "Street address.")
            string("phone", "Phone number.")
            string("url", "URL.")
            integer("radius", "Trigger radius in metres for location reminders, more than 0. Defaults to 250.")
            integer("color", COLOR)
            string("icon", ICON)
        },
    ) { args ->
        val (place, created) = api.createPlace(
            PlaceWrite(
                latitude = args.requireDouble("latitude"),
                longitude = args.requireDouble("longitude"),
                name = args.string("name"),
                address = args.string("address"),
                phone = args.string("phone"),
                url = args.string("url"),
                radius = args.int("radius"),
                color = args.int("color"),
                icon = args.string("icon"),
            )
        )
        buildJsonObject {
            put("place", json.encodeToJsonElement(place.toApiPlace()))
            put("created", created)
            if (!created) {
                put("note", "A place at these coordinates already existed ('${place.displayName}'), so that one was returned and nothing was created.")
            }
        }.toString() to "place -> ${place.id}${if (created) "" else " (existing)"}"
    }

    tool(
        reg, "update_place",
        title = "Update a saved place",
        description = "Change a saved place's details. Only the fields you send change. " +
            "Coordinates are insert-only: a place somewhere else is a different place, so " +
            "create that one instead.",
        readOnly = false,
        schema = schema {
            integer("place_id", "The place to change. See list_places.", required = true)
            string("name", "Display name. Empty string clears it.")
            string("address", "Street address.")
            string("phone", "Phone number.")
            string("url", "URL.")
            integer("radius", "Trigger radius in metres for location reminders, more than 0.")
            integer("color", "$COLOR 0 clears it.")
            string("icon", ICON_CLEARS)
        },
    ) { args ->
        val id = args.requireLong("place_id")
        val place = api.updatePlace(
            id,
            PlaceWrite(
                name = args.string("name"),
                address = args.string("address"),
                phone = args.string("phone"),
                url = args.string("url"),
                radius = args.int("radius"),
                color = args.int("color"),
                icon = args.string("icon"),
            )
        )
        buildJsonObject { put("place", json.encodeToJsonElement(place.toApiPlace())) }.toString() to
            "updated place $id"
    }

    tool(
        reg, "set_task_reminders",
        title = "Add or remove reminders",
        description = """
            Add reminders to a task, or delete them by id. Handles both kinds:

            - Time reminders: date_time and snooze take trigger_at; relative_start,
              relative_due and random take a signed offset_ms (negative is before).
              A snooze quiets the task's other reminders until it fires, then deletes
              itself - it is how "don't remind me about this until tomorrow" is done.
            - Location reminders: location_arrival and location_departure take a place_id
              and no timing at all.

            A task has one place, and every location reminder on it uses that place.
            Adding the first one files the task there; asking for a different place is
            rejected, so move the task with update_tasks first. Deleting a location
            reminder leaves the task filed at the place - to unfile it entirely, set
            place_id to 0 with update_tasks.

            Arrival and departure are independent, so adding one never disturbs the other
            and repeating an add is harmless.
        """.trimIndent(),
        readOnly = false,
        idempotent = true,
        schema = schema {
            integer("task_id", "The task.", required = true)
            objectArray("add", "Reminders to add.") {
                string("type", "Reminder type.", required = true, enum = TasksContract.Reminders.TYPES)
                timestamp("trigger_at", "Absolute time. date_time and snooze only.")
                integer("offset_ms", "Signed offset from start/due; negative is before. For random, how often it fires, at least a minute. Relative and random types only.")
                integer("repeat_count", "Repeats after the first trigger. Needs interval_ms.")
                integer("interval_ms", "Gap between repeats, at least a minute.")
                integer("place_id", "The place that triggers it. Required for the location types.")
            }
            integerArray("remove_reminder_ids", "Reminder ids to delete. Get them from list_reminders.")
        },
    ) { args ->
        val taskId = args.requireLong("task_id")
        val edit = api.setTaskReminders(taskId, args.reminders("add"), args.longs("remove_reminder_ids"))
        val body = buildJsonObject {
            put("added_reminder_ids", json.encodeToJsonElement(edit.addedIds))
            put("removed", edit.removed)
            put("reminders", json.encodeToJsonElement(edit.reminders.map { it.toApiReminder() }))
            edit.task?.let { put("task", json.encodeToJsonElement(it.toApiTask())) }
        }
        body.toString() to "task $taskId: +${edit.addedIds.size} / -${edit.removed} reminder(s)"
    }
}

private fun Server.tool(
    reg: Registration,
    name: String,
    title: String,
    description: String,
    schema: io.modelcontextprotocol.kotlin.sdk.types.ToolSchema,
    readOnly: Boolean,
    destructive: Boolean = false,
    idempotent: Boolean = false,
    handler: suspend (JsonObject) -> Pair<String, String?>,
) {
    require(name in McpToolCatalog.names) { "$name is missing from McpToolCatalog" }
    if (!reg.permissions.allows(name)) return
    val log = reg.log
    val usage = reg.usage
    addTool(
        name = name,
        title = title,
        description = description,
        inputSchema = schema,
        toolAnnotations = ToolAnnotations(
            title = title,
            readOnlyHint = readOnly,
            destructiveHint = if (readOnly) false else destructive,
            idempotentHint = if (readOnly) true else idempotent,

            openWorldHint = false,
        ),
    ) { request ->
        val started = System.nanoTime()
        usage.record()
        val args = request.arguments()
        try {
            schema.properties?.let { args.rejectUnknown(it) }
            val (text, summary) = handler(args)
            log.record(
                tool = name,
                summary = summary ?: "ok",
                outcome = ActivityLog.Outcome.Ok,
                durationMs = (System.nanoTime() - started) / 1_000_000,
            )
            CallToolResult(content = listOf(TextContent(text)))
        } catch (e: Throwable) {
            val (message, outcome) = e.toToolError()
            log.record(
                tool = name,
                summary = message.lineSequence().first().take(120),
                outcome = outcome,
                durationMs = (System.nanoTime() - started) / 1_000_000,
                detail = e.stackTraceToString().lineSequence().take(6).joinToString("\n"),
            )
            CallToolResult(content = listOf(TextContent(message)), isError = true)
        }
    }
}

private fun Throwable.toToolError(): Pair<String, ActivityLog.Outcome> =
    ApiErrors.explain(this) to ActivityLog.Outcome.Error

private fun CallToolRequest.arguments(): JsonObject = params.arguments ?: JsonObject(emptyMap())

private fun JsonObject.rejectUnknown(properties: JsonObject, path: String = "") {
    val unknown = keys - properties.keys
    if (unknown.isNotEmpty()) {
        throw IllegalArgumentException(
            "Unknown argument${if (unknown.size > 1) "s" else ""} ${unknown.joinToString { "'$path$it'" }}. " +
                "This tool takes: ${properties.keys.joinToString(", ")}",
        )
    }
    properties.forEach { (key, spec) ->
        val nested = spec.jsonObject["items"]?.jsonObject?.get("properties")?.jsonObject ?: return@forEach
        (this[key] as? JsonArray)?.forEachIndexed { index, element ->
            (element as? JsonObject)?.rejectUnknown(nested, "$key[$index].")
        }
    }
}

private fun SchemaBuilder.paging(noun: String) = apply {
    integer(
        "limit",
        "Max ${noun}s to return. 0 returns none and just counts them, which is how you " +
            "count without transferring anything.",
        minimum = 0,
        maximum = TasksContract.MAX_LIMIT,
        default = TasksContract.DEFAULT_LIMIT,
    )
    integer("offset", "Rows to skip, for paging. Follow next_offset.", minimum = 0, default = 0)
}

private fun JsonObject.limit(): Int = pageLimit(int("limit"))

private fun JsonObject.offset(): Int = pageOffset(int("offset"))

private fun Deletion.body(alsoAffectedKey: String): JsonObject = buildJsonObject {
    put("rows_deleted", rowsDeleted)
    if (rowsDeleted > 0) put(alsoAffectedKey, alsoAffected)
}

private fun SchemaBuilder.fields(noun: String, available: List<String>) = stringArray(
    "fields",
    "Return only these fields of each $noun, to keep the response small. Omit for all of " +
        "them. `id` always comes back, and a field naming an object (a date, a reminder) " +
        "keeps that object whole.",
    enum = available,
)

private fun JsonObject.projection(available: List<String>): Projection =
    Projection.of(strings("fields"), available)

private fun SchemaBuilder.newTaskFields() = apply {
    string("title", "Task title. Required and cannot be empty.", required = true)
    string("notes", "Free-form notes. Markdown, as the user would type it.")
    string("priority", "Priority. Defaults to none.", enum = TasksContract.Tasks.PRIORITIES)
    date("due_date", "When the task is due. Send 0 to explicitly mean no due date.")
    boolean("due_all_day", "True if the due date carries no time of day. Compute all-day dates from LOCAL midnight, not UTC.")
    date("start_date", "When work on the task can start; the app hides it until then.")
    boolean("start_all_day", "True if the start date carries no time of day.")
    string("recurrence", "RFC 5545 RRULE, e.g. 'RRULE:FREQ=WEEKLY;BYDAY=MO'.")
    string("repeat_from", "What the next occurrence is measured from.", enum = TasksContract.Tasks.REPEAT_FROMS)
    integer("parent_id", "Make this a subtask of that task. The parent must already exist - it cannot name a task created earlier in this same call, and an id that does not exist yet refuses the whole batch. To build a task and its subtasks, create the parent, then send the children in a second call with its id.")
    integer("list_id", "Which list. Defaults to the user's default list.")
    integer("place_id", "File the task at a saved place (no reminder implied).")
}

private fun SchemaBuilder.taskPatchFields() = apply {
    string("title", "New title. Cannot be empty.")
    string("notes", "New notes. Empty string clears them.")
    string("priority", "New priority.", enum = TasksContract.Tasks.PRIORITIES)
    date("due_date", "New due date. 0 clears it.")
    boolean("due_all_day", "Whether the due date is all-day. Omit to keep it as it is.")
    date("start_date", "New start date. 0 clears it.")
    boolean("start_all_day", "Whether the start date is all-day. Omit to keep it as it is.")
    string("recurrence", "New RRULE. Empty string stops the series repeating.")
    string("repeat_from", "What the next occurrence is measured from.", enum = TasksContract.Tasks.REPEAT_FROMS)
    integer("parent_id", "Re-parent the task. 0 makes it top-level.")
    integer("list_id", "Move the task (and its subtasks) to another list.")
    integer("place_id", "File the task at a place. 0 removes the place and its reminders.")
}

private fun JsonObject.toTaskWrite(): TaskWrite = TaskWrite(
    title = string("title"),
    notes = string("notes"),
    priority = string("priority"),
    due = date("due_date"),
    dueAllDay = boolean("due_all_day"),
    start = date("start_date"),
    startAllDay = boolean("start_all_day"),
    recurrence = string("recurrence"),
    repeatFrom = string("repeat_from"),
    parentId = long("parent_id"),
    listId = long("list_id"),
    placeId = long("place_id"),
)

private fun JsonObject.toTaskUpdate(): TaskUpdate =
    TaskUpdate(id = requireLong("task_id"), patch = toTaskWrite())

private inline fun <reified T> Projection.encode(rows: T): String {
    val encoder = encoder()
    return encoder.encodeToString(JsonElement.serializer(), apply(encoder.encodeToJsonElement(rows)))
}

private fun JsonObjectBuilder.putEcho(key: String, rows: List<TaskRow>) {
    val encoded = json.encodeToJsonElement(rows.map { it.toApiTask() }) as JsonArray
    val kept = encoded.fitTo(MAX_ROWS_BYTES)
    put(key, kept)
    if (kept.size < encoded.size) {
        put(
            "hint",
            "The write is complete, but ${encoded.size - kept.size} of ${encoded.size} echoed " +
                "rows were left out to fit the response size limit. Read the ids back if you " +
                "need the rest.",
        )
    }
}

private fun JsonArray.fitTo(budget: Int): JsonArray {
    if (isEmpty() || measure() <= budget) return this
    var low = 1
    var high = size
    var best = 1
    while (low <= high) {
        val mid = (low + high) / 2
        if (JsonArray(subList(0, mid)).measure() <= budget) {
            best = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return JsonArray(subList(0, best))
}

private fun JsonArray.measure(): Int = toString().toByteArray(Charsets.UTF_8).size

private inline fun <R, reified T> ApiPage<R>.toResult(
    label: String,
    projection: Projection,
    map: (R) -> T,
    summary: (Int) -> String,
): Pair<String, String> {
    val encoder = projection.encoder()
    val encoded = projection.apply(encoder.encodeToJsonElement(rows.map(map))) as JsonArray
    val rows = encoded.fitTo(MAX_ROWS_BYTES)
    val cut = rows.size < encoded.size
    val body = buildJsonObject {
        put("total", total)
        val start = offset ?: return@buildJsonObject
        val next = start + rows.size
        val more = next < total
        put("offset", start)
        put("has_more", more)
        put("${label}s", rows)
        if (more) {
            put("next_offset", next)
            put(
                "hint",
                if (cut) {
                    "This page was cut short to fit the response size limit, so it is smaller " +
                        "than the limit you asked for - that is not the end of the results. " +
                        "Continue from next_offset, and ask for fewer `fields` to fit more rows " +
                        "per page."
                } else {
                    "More rows match than were returned. Continue from next_offset - `total` " +
                        "is the full match count, not the number returned here."
                },
            )
        }
    }
    return body.toString() to (summary(rows.size) + projection.describe())
}

private fun JsonObject.element(name: String): JsonElement? =
    this[name]?.takeIf { it !is kotlinx.serialization.json.JsonNull }

internal fun JsonObject.string(name: String): String? =
    element(name)?.jsonPrimitive?.contentOrNull

internal fun JsonObject.requireString(name: String): String {
    val value = string(name)
        ?: throw IllegalArgumentException("Missing required argument '$name'")
    return value.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Argument '$name' must not be empty")
}

private fun JsonObject.long(name: String): Long? = element(name)?.let {
    it.jsonPrimitive.longOrNull
        ?: it.jsonPrimitive.contentOrNull?.toLongOrNull()
        ?: throw IllegalArgumentException("Argument '$name' must be an integer")
}

private fun JsonObject.date(name: String): Any? = element(name)?.let {
    it.jsonPrimitive.longOrNull ?: it.jsonPrimitive.contentOrNull
        ?: throw IllegalArgumentException("Argument '$name' must be an integer or a date string")
}

private fun JsonObject.requireLong(name: String): Long =
    long(name) ?: throw IllegalArgumentException("Missing required argument '$name'")

private fun JsonObject.int(name: String): Int? = element(name)?.let {
    it.jsonPrimitive.intOrNull
        ?: it.jsonPrimitive.contentOrNull?.toIntOrNull()
        ?: throw IllegalArgumentException("Argument '$name' must be an integer")
}

private fun JsonObject.requireDouble(name: String): Double =
    element(name)?.jsonPrimitive?.doubleOrNull
        ?: throw IllegalArgumentException("Missing or non-numeric argument '$name'")

private fun JsonObject.boolean(name: String): Boolean? = element(name)?.let {
    it.jsonPrimitive.booleanOrNull
        ?: it.jsonPrimitive.contentOrNull?.toBooleanStrictOrNull()
        ?: throw IllegalArgumentException("Argument '$name' must be true or false")
}

private fun JsonObject.array(name: String, of: String): JsonArray? = element(name)?.let {
    it as? JsonArray ?: throw IllegalArgumentException("Argument '$name' must be an array of $of")
}

private fun JsonObject.longs(name: String): List<Long> = array(name, "integers")?.map {
    (it as? JsonPrimitive)?.longOrNull
        ?: throw IllegalArgumentException("Argument '$name' must be an array of integers")
} ?: emptyList()

private fun JsonObject.strings(name: String): List<String> = array(name, "strings")?.map {
    (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content
        ?: throw IllegalArgumentException("Argument '$name' must be an array of strings")
} ?: emptyList()

private fun JsonObject.objects(name: String): List<JsonObject> = array(name, "objects")?.map {
    it as? JsonObject ?: throw IllegalArgumentException("Argument '$name' must be an array of objects")
} ?: emptyList()

private fun JsonObject.reminders(name: String): List<ReminderWrite> =
    objects(name).map { o ->
        ReminderWrite(
            type = o.requireString("type"),
            triggerAt = o.date("trigger_at"),
            offsetMs = o.long("offset_ms"),
            repeatCount = o.int("repeat_count"),
            intervalMs = o.long("interval_ms"),
            placeId = o.long("place_id"),
        )
    }

