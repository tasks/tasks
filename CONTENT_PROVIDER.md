# Tasks.org API Reference

Content provider API for reading and writing Tasks.org data from another app.

> **`v0` is unstable.** Columns and parameters may change between releases. Check the
> [changelog](#changelog) when you update Tasks.

## Base configuration

| Setting | Value |
| --- | --- |
| **Authority** | `org.tasks.api` |
| **Base URI** | `content://org.tasks.api/v0` |
| **Default limit** | 100 rows, no maximum |
| **Required permissions** | `org.tasks.permission.READ_TASKS`, `org.tasks.permission.WRITE_TASKS` |

### Getting connected

Add the following to your manifest:

```xml
<uses-permission android:name="org.tasks.permission.READ_TASKS" />
<uses-permission android:name="org.tasks.permission.WRITE_TASKS" />

<queries>
  <provider android:authorities="org.tasks.api" />
</queries>
```

Check if the content provider is available, and which release provides it:

```kotlin
val provider = packageManager.resolveContentProvider("org.tasks.api", 0)
val versionCode = provider?.let {
    PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(it.packageName, 0))
}
```

`null` means Tasks.org is not installed. [Changelog](#changelog) entries are headed with
this version code.

Request runtime permissions:

```kotlin
requestPermissions(
    arrayOf(
        "org.tasks.permission.READ_TASKS",
        "org.tasks.permission.WRITE_TASKS",
    ),
    RC,
)
```

## Endpoints

The collection URI lists and creates; `/{id}` applies to one row.

| Collection | Query | Insert | Update | Delete | Holds |
| --- | --- | --- | --- | --- | --- |
| `/v0/tasks` | • | • | | | Tasks |
| `/v0/tasks/{id}` | • | | • | • | |
| `/v0/alarms` | • | • | | | Reminders on a task, by time or by location |
| `/v0/alarms/{id}` | • | | • | • | |
| `/v0/task_tags` | • | • | | • | Which tags are on which tasks |
| `/v0/task_tags/{id}` | • | | | • | |
| `/v0/lists` | • | • | | | Task lists, local and synced |
| `/v0/lists/{id}` | • | | • | • | |
| `/v0/tags` | • | • | | | Tags |
| `/v0/tags/{id}` | • | | • | • | |
| `/v0/places` | • | • | | | Saved locations |
| `/v0/places/{id}` | • | | • | • | |
| `/v0/accounts` | • | | | | Sync accounts |
| `/v0/accounts/{id}` | • | | | | |

## Conventions

### Query parameters, not SQL

`selection`, `selectionArgs`, `sortOrder` and `QUERY_ARG_SQL_*` throw
`IllegalArgumentException`, including the `sortOrder` argument of the five-argument
`query()`. Pass `null` for all of them and use `sort`. Filter with the named parameters
listed per endpoint, as URI query parameters or in the `Bundle` of
`query(uri, projection, queryArgs, signal)`.

`projection` is supported: pass the columns you want, or `null` for all. See
[Projections](#projections).

URL-encode parameter values with `Uri.encode()`. Every call is a binder round trip; never
make one on the main thread.

Repeatable parameters OR within themselves and AND with everything else. Repeatable wherever
they appear: `_id`, `task_id`, `list_id`, `tag_id`, `place_id`, `account_id`, `parent_id`, `priority`,
`type`, `access`.

### Projections

Pass `null` to fetch every column, or an array of names for a subset. Item URIs take a
projection but not query parameters: `/v0/tasks/42?limit=1` throws.

```kotlin
resolver.query(uri, arrayOf("title", "due_date"), null, null, null)!!.use { c ->
    val columns = c.columnNames.toSet()
    val due = c.getColumnIndex("due_date")
}
```

### Ranges are exclusive

`_before` and `_after` are not inclusive.

### Paging

Every collection pages. `limit` defaults to 100, with no maximum.
`ContentResolver.QUERY_ARG_LIMIT` and `QUERY_ARG_OFFSET` work as equivalents.

Every collection query carries `ContentResolver.EXTRA_TOTAL_COUNT`: rows matching the query,
ignoring `limit` and `offset`. `cursor.count` is at most `limit`.

```kotlin
var offset = 0
var total = Int.MAX_VALUE

while (offset < total) {
    val uri = "content://org.tasks.api/v0/tasks?limit=500&offset=$offset".toUri()
    val page = resolver.query(uri, null, null, null, null) ?: break
    page.use { c ->
        total = c.extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT, 0)
        while (c.moveToNext()) {
            // ...consume row...
        }
        offset += c.count
        if (c.count == 0) total = offset
    }
}
```

### Ordering

`_id` ascending is the default and the final tiebreaker under every `sort`.

`sort` and `sort_desc` are on `/v0/tasks`; every other collection is ordered by `_id`.
`ContentResolver.QUERY_ARG_SORT_COLUMNS` and `QUERY_ARG_SORT_DIRECTION` work as equivalents,
with one column.

`sort=priority` ascending puts `high` first, then `medium`, `low`, `none`. Under `sort=due`
and `sort=start`, unset dates sort first ascending, last descending.

### Counting without fetching

`limit=0` returns an empty cursor with `EXTRA_TOTAL_COUNT` set.

```kotlin
val overdue = resolver.query(
    "content://org.tasks.api/v0/tasks?limit=0&due_after=0&due_before=$now&completed=0".toUri(),
    null, null, null, null
)?.use { it.extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT, 0) } ?: 0
```

### Watching for changes

Every collection is observable. Changes fire from this API, from background sync and from
edits in the app, and survive the Tasks process restarting.

```kotlin
val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
    override fun onChange(selfChange: Boolean) = reload()
}

resolver.registerContentObserver(
    "content://org.tasks.api/v0/tasks".toUri(),
    true,
    observer,
)
```

The notification carries no payload.

- Notifications go to collection URIs only; registering on `/v0/tasks/42` never fires.
- `selfChange` is always `false`; a client that writes wakes itself.
- Anything that touches a task notifies `/v0/tasks`, `/v0/alarms` and `/v0/task_tags`.
  `/v0/tags`, `/v0/places`, `/v0/lists` and `/v0/accounts` are independent.

Register on the base URI for one signal covering everything:

```kotlin
resolver.registerContentObserver("content://org.tasks.api/v0".toUri(), true, observer)
```

### Empty values

**No column is ever null.** Unset is the type's empty value:

| Type | Unset is | Example |
| --- | --- | --- |
| Text | `""` | a task with no notes has `notes == ""` |
| Timestamp | `0` | an unscheduled task has `due_date == 0` |
| Number | `0` | a list with no color has `color == 0` |
| Reference (`*_id`) | `0` | a top-level task has `parent_id == 0` |
| 0/1 flag | `0` | |

On write, `null` and the empty value both clear a field.

A task's `title`, a list's `title` and a tag's `name` cannot be cleared: `""` or `null` on
`insert` or `update` throws `IllegalArgumentException`. A place's `name` is optional.

### Icons

Lists, tags and places carry an `icon`: a [Material Symbols](https://fonts.google.com/icons)
name in snake_case, such as `beach_access`, `luggage`, `shopping_cart` or `location_on`.
A machine-readable list of every name is at
[fonts.google.com/metadata/icons](https://fonts.google.com/metadata/icons).

A leading `gmo_` is accepted and ignored, so `gmo_beach_access` and `beach_access` are the
same icon.

Names are not validated on write: an unrecognized one is stored and read back unchanged, and
renders as no icon. `""` is unset.

### Looking something up by name

`/v0/tasks` has `search`. `/v0/lists`, `/v0/tags`, `/v0/places` and `/v0/accounts` do not;
fetch the collection and match names yourself.

```kotlin
val listId = resolver.query(
    "content://org.tasks.api/v0/lists".toUri(), null, null, null, null
)?.use { c ->
    val id = c.getColumnIndexOrThrow("_id")
    val title = c.getColumnIndexOrThrow("title")
    generateSequence { if (c.moveToNext()) c else null }
        .firstOrNull { it.getString(title).equals("Groceries", ignoreCase = true) }
        ?.getLong(id)
}
```

### Timestamps

All timestamps are epoch milliseconds. `0` means unset.

A date with no time of day is flagged by a companion column: `due_date` + `due_all_day`,
`start_date` + `start_all_day`. The flags default to `0` on write, so always write the pair
together.

All-day dates are in the device's local time zone; only the calendar day survives. Compute
from local midnight, not UTC.

### Errors

| Exception | Means |
| --- | --- |
| `SecurityException` | Missing or revoked permission |
| `IllegalArgumentException` | Unknown URI, verb not supported on this URI, unknown parameter, bad value, or SQL supplied |
| `UnsupportedOperationException` | The target is read-only — the list, or a task on it |
| `IllegalStateException` | A remote operation failed; the message carries the server's response |
| `OperationApplicationException` | A batch failed; nothing in it was applied |
| `TransactionTooLargeException` | The batch or its results exceeded the binder transaction limit. Often arrives wrapped in a `RuntimeException`. Split the batch |

Every query returns a cursor, possibly with no rows, or throws. A null return means the
request never reached the provider: Tasks is not installed, or `<queries>` is missing.

---

# Tasks

## List tasks

```
content://org.tasks.api/v0/tasks
```

### Parameters

| Parameter | Type | Description |
| --- | --- | --- |
| `_id` | long | Fetch specific tasks. Repeatable — `?_id=12&_id=19&_id=23` returns those three |
| `search` | string | Substring match on **title and notes only** — see below |
| `list_id` | long | Repeatable |
| `tag_id` | long | Repeatable |
| `place_id` | long | Repeatable |
| `priority` | enum | `high`, `medium`, `low`, `none`. Repeatable |
| `parent_id` | long | Children of that task. `parent_id=0` returns top-level tasks. Repeatable |
| `completed` | 0/1 | `0` for open tasks, `1` for completed ones. Omit for both |
| `due_before` / `due_after` | long | Epoch millis, exclusive |
| `start_before` / `start_after` | long | Epoch millis, exclusive |
| `completed_before` / `completed_after` | long | Epoch millis, exclusive |
| `created_before` / `created_after` | long | Epoch millis, exclusive |
| `modified_before` / `modified_after` | long | Epoch millis, exclusive |
| `sort` | enum | `due`, `start`, `created`, `modified`, `priority`, `title`. `_id` is always the final tiebreaker |
| `sort_desc` | 0/1 | 1 reverses the sort. Default 0 |
| `limit` | int | Default 100, no maximum |
| `offset` | int | Rows to skip. Default 0 |

`search` is case-insensitive and matched literally; `%` and `_` are not wildcards.

`completed` is the flag; `completed_before` / `completed_after` filter *when* a task was
completed.

Returns everything not deleted, including completed and hidden tasks. Common combinations:

| You want | Query |
| --- | --- |
| Not completed | `completed=0` |
| Completed | `completed=1` |
| Not hidden | `start_before=<now>` |
| Has a due date | `due_after=0` |
| Today's unfinished tasks | `due_after=<start of day>&due_before=<end of day>&completed=0` |
| Recently modified | `modified_after=<timestamp>` |
| Snoozed | `/v0/alarms?type=snooze`, then read the `task_id`s |
| In a list, tag or place | `list_id=`, `tag_id=`, `place_id=` |
| Has a place or tags | read `place_id` and `tag_ids` off the row |
| Has reminders | `/v0/alarms?task_id=` |

### Columns

`W` marks what you may write: `•` on insert and update, `insert` on insert only, blank for
read-only.

| Column | Type | W | Description |
| --- | --- | :-: | --- |
| `_id` | long |  | Task id. Local to this install |
| `title` | string | • | Task title |
| `notes` | string | • | Markdown, as the user typed it |
| `priority` | string | • | `high`, `medium`, `low`, `none` |
| `due_date` | long | • | `0` when unset |
| `due_all_day` | 0/1 | • | 1 means the date carries no time of day |
| `start_date` | long | • | Task is hidden until this time. `0` when unset |
| `start_all_day` | 0/1 | • | 1 means the start date carries no time of day |
| `completed_at` | long | • | `0` when not completed. Writable — see [Update a task](#update-a-task) |
| `created_at` | long |  | When the task was created |
| `modified_at` | long |  | When the task row last changed |
| `recurrence` | string | • | RFC 5545 `RRULE`. `""` when the task does not repeat — see [Recurring tasks](#recurring-tasks) |
| `repeat_from` | string | • | `due_date`, `completion_date` |
| `parent_id` | long | • | `0` for a top-level task |
| `list_id` | long | • | The list the task belongs to |
| `tag_ids` | string |  | The task's tags, comma-joined ids. `""` when it has none |
| `place_id` | long | • | The task's place. `0` when it has none — see below |
| `child_count` | int |  | Direct children, the same set `?parent_id=<id>` returns |
| `uncompleted_child_count` | int |  | Of those, the ones not completed |
| `is_read_only` | 0/1 |  | Writes will be refused |

Reminders are not on the row; query [`/v0/alarms`](#alarms) with `?task_id=`.

Change a task's tags on `/v0/task_tags`, or a child's parent by writing the child's
`parent_id`. `tag_ids` is comma-joined in no particular order.

A place implies no reminder on its own; arrival and departure triggers are
[location reminders](#location-reminders).

### Example

```kotlin
// Unfinished tasks due today in the Work list, most urgent first
val uri = ("content://org.tasks.api/v0/tasks?" +
        "list_id=$workListId" +
        "&due_after=$startOfDay&due_before=$endOfDay" +
        "&completed=0" +
        "&sort=priority&limit=50").toUri()

resolver.query(uri, null, null, null, null)?.use { c ->
    val title = c.getColumnIndexOrThrow("title")
    val due = c.getColumnIndexOrThrow("due_date")
    val allDay = c.getColumnIndexOrThrow("due_all_day")
    while (c.moveToNext()) {
        println("${c.getString(title)} — ${c.getLong(due)} allDay=${c.getInt(allDay) == 1}")
    }
}
```

## Get one task

```
content://org.tasks.api/v0/tasks/{id}
```

Same columns. Returns an empty cursor if the id does not exist or has been deleted.

## Create a task

```
insert content://org.tasks.api/v0/tasks
```

Requires `WRITE_TASKS`. Returns the new task's item URI, or throws.

| Value | Required | Notes |
| --- | --- | --- |
| `title` | yes | Task title |
| `list_id` | no | Defaults to the user's default list |
| `place_id` | no | Files the task under a saved place, with no reminder. The place must already exist — create one on `/v0/places` |
| `completed_at` | no | A timestamp creates the task already completed. On a recurring task the series advances immediately, so the row reads back uncompleted — see [Recurring tasks](#recurring-tasks) |
| `notes`, `priority`, `due_date`, `due_all_day`, `start_date`, `start_all_day`, `recurrence`, `repeat_from`, `parent_id` | no | See Columns |

The title is stored as sent, minus surrounding whitespace. Dates, tags and priority are not
parsed out of it.

Tags and reminders are separate rows; create them with the task in
[a batch](#batch-operations).

```kotlin
val values = contentValuesOf(
    "title" to "Renew passport",
    "due_date" to dueMillis,
    "due_all_day" to 1,
    "priority" to "high",
    "list_id" to personalListId,
)
val uri = resolver.insert("content://org.tasks.api/v0/tasks".toUri(), values)
val id = ContentUris.parseId(uri!!)
```

### What a new task inherits

Nothing but the list. A task created here has no priority, no due or start date, no
recurrence, no reminders, no tags and no place unless you send them; the user's configured
defaults for new tasks do not apply. Leave `list_id` out and the task lands in the user's
default list.

## Update a task

```
update content://org.tasks.api/v0/tasks/{id}
```

Requires `WRITE_TASKS`. A **patch**: only the keys you supply change, and a key mapped to
`null` or to the column's empty value clears that field. Returns the number of rows changed.

| Parameter | Description |
| --- | --- |
| `if_modified_at` | Optional. Returns 0 without writing if the task's `modified_at` differs — use it to make read-modify-write safe against concurrent edits |

A return of 0 covers both a stale `if_modified_at` and an id that no longer exists; re-read
the item URI to tell them apart.

Four values are not plain column writes:

| Value | Effect |
| --- | --- |
| `completed_at` | Completing, not a column assignment. A timestamp completes the task with that date; `0` uncompletes it. Cascades to subtasks, un-completes parents when uncompleting, cancels pending notifications, and advances a recurring task rather than leaving it done — see [Recurring tasks](#recurring-tasks) |
| `list_id` | Moves the task, and its subtasks, to another list. `0` throws `IllegalArgumentException` |
| `place_id` | Files the task under a place, keeping any reminders it already had. `0` removes the place and its reminders |
| `parent_id` | Re-parents the task. `0` moves it to the top level. |

Writes to a task on a read-only list throw `UnsupportedOperationException`.

```kotlin
val task = "content://org.tasks.api/v0/tasks/$id".toUri()

resolver.update(task, contentValuesOf("title" to "Renew passport", "priority" to "high"), null, null)

// Move to another list and file under a place
resolver.update(task, contentValuesOf("list_id" to listId, "place_id" to placeId), null, null)

// Complete, only if untouched since it was read
val changed = resolver.update(
    "$task?if_modified_at=$lastSeen".toUri(),
    contentValuesOf("completed_at" to System.currentTimeMillis()),
    null, null
)
```

## Recurring tasks

A task repeats when `recurrence` holds an RFC 5545 `RRULE`. `repeat_from` decides what the
next occurrence is measured from: `due_date` (the default) or `completion_date`.

Completing a recurring task does not leave it completed. There is one row per series;
completing it advances that row to the next occurrence, keeping its `_id`. The update
returns 1:

| After completing | Reads back as |
| --- | --- |
| `completed_at` | `0` — the task is not completed |
| `due_date`, `start_date` | moved forward to the next occurrence |
| `recurrence` | **rewritten** if the rule is `COUNT`-limited, with the count decremented |
| Alarms | rescheduled against the new dates; a pending snooze is canceled |

Retrying a completion advances the series again.

The series ends at the final occurrence of a `COUNT` rule, or the first past `UNTIL`.
Completion then sticks.

On accounts with `repeats_on_server == 1` (see [`/v0/accounts`](#accounts)), completion leaves
`completed_at` set and the dates unchanged; the next occurrence arrives on the next sync.

To stop a series early, clear `recurrence` before completing. Deleting a recurring task
deletes the whole series.

## Delete a task

```
delete content://org.tasks.api/v0/tasks/{id}
```

Requires `WRITE_TASKS`. Deletes the task **and all of its subtasks**; there is no undo.
Deleting a task on a read-only list throws `UnsupportedOperationException`.

---

# Alarms

Reminders attached to a task. Time reminders fire at a time; location reminders fire on
arriving at or leaving the task's place.

## List alarms

```
content://org.tasks.api/v0/alarms
content://org.tasks.api/v0/alarms/{id}
```

| Parameter | Type | Description |
| --- | --- | --- |
| `task_id` | long | Repeatable |
| `type` | enum | `date_time`, `relative_start`, `relative_due`, `random`, `snooze`, `location_arrival`, `location_departure`. Repeatable |
| `place_id` | long | Only ever matches location reminders. Repeatable |
| `limit` / `offset` | int | `limit` defaults to 100; no maximum |

### Columns

| Column | Type | W | Description |
| --- | --- | :-: | --- |
| `_id` | long |  | Row id. Local to this install |
| `task_id` | long | insert | The task this row belongs to |
| `type` | string | insert | `date_time`, `relative_start`, `relative_due`, `random`, `snooze`, `location_arrival`, `location_departure` |
| `trigger_at` | long | • | Absolute time. `date_time` and `snooze` only |
| `offset_ms` | long | • | Signed offset from the start or due date; negative is *before*. Relative and random types only |
| `repeat_count` | int | • | How many times to repeat after the first trigger |
| `interval_ms` | long | • | Gap between repeats |
| `place_id` | long | insert | The place that triggers this reminder. Location types only; `0` on a time reminder |

Which columns apply depends on the type. Sending one that does not apply throws:

| Type | Timing column | Also |
| --- | --- | --- |
| `date_time`, `snooze` | `trigger_at` | |
| `relative_start`, `relative_due`, `random` | `offset_ms` | `repeat_count`, `interval_ms` |
| `location_arrival`, `location_departure` | none | `place_id`, required |

## Create, update, delete an alarm

```
insert content://org.tasks.api/v0/alarms
update content://org.tasks.api/v0/alarms/{id}
delete content://org.tasks.api/v0/alarms/{id}
```

Requires `WRITE_TASKS`. `task_id` and `type` are required on insert. An insert matching an
existing alarm on the same task in every field returns that row's URI.

```kotlin
// Two hours before it's due
resolver.insert("content://org.tasks.api/v0/alarms".toUri(), contentValuesOf(
    "task_id" to taskId,
    "type" to "relative_due",
    "offset_ms" to -TimeUnit.HOURS.toMillis(2),
))

// A day after it's due, then daily for six more days
resolver.insert("content://org.tasks.api/v0/alarms".toUri(), contentValuesOf(
    "task_id" to taskId,
    "type" to "relative_due",
    "offset_ms" to TimeUnit.DAYS.toMillis(1),
    "repeat_count" to 6,
    "interval_ms" to TimeUnit.DAYS.toMillis(1),
))
```

## Location reminders

The place must exist first — see [Places](#places).

```kotlin
// "Remind me when I leave home"
resolver.insert("content://org.tasks.api/v0/alarms".toUri(), contentValuesOf(
    "task_id" to taskId,
    "type" to "location_departure",
    "place_id" to homeId,
))
```

- Inserting a reminder sets the task's `place_id` if it has none. A different place throws
  `IllegalArgumentException`; move the task with `place_id` first, and its reminders follow.
- Arrival and departure are independent rows. A repeated insert returns the existing row.
- Deleting a reminder keeps the place. Write `place_id = 0` on the task to remove the place
  and its reminders. Deleting a reminder that was not set returns `0`.
- `update` throws `IllegalArgumentException`. Delete and insert the other type.

## Worked example: "remind me when I get home"

```kotlin
// Match a saved place by name
val home = resolver.query(
    "content://org.tasks.api/v0/places".toUri(), null, null, null, null
)?.use { c ->
    val id = c.getColumnIndexOrThrow("_id")
    val name = c.getColumnIndexOrThrow("display_name")
    generateSequence { if (c.moveToNext()) c else null }
        .firstOrNull { it.getString(name).contains("home", ignoreCase = true) }
        ?.getLong(id)
} ?: return

resolver.applyBatch("org.tasks.api", arrayListOf(
    ContentProviderOperation.newInsert("content://org.tasks.api/v0/tasks".toUri())
        .withValue("title", "Water the plants")
        .build(),
    ContentProviderOperation.newInsert("content://org.tasks.api/v0/alarms".toUri())
        .withValueBackReference("task_id", 0)
        .withValue("type", "location_arrival")
        .withValue("place_id", home)
        .build(),
))
```

---

# Task tags

The join between tasks and tags. Create the tag itself via [Tags](#tags).

```
content://org.tasks.api/v0/task_tags
content://org.tasks.api/v0/task_tags/{id}
```

| Parameter | Type | Description |
| --- | --- | --- |
| `tag_id` | long | Repeatable |
| `limit` / `offset` | int | `limit` defaults to 100; no maximum |

There is no `task_id` read parameter; a task's tags are the `tag_ids` column on the task.
`task_id` is a delete key only.

| Column | Type | W | Description |
| --- | --- | :-: | --- |
| `_id` | long |  | Row id. Local to this install |
| `task_id` | long | insert | The task this row belongs to |
| `tag_id` | long | insert | The tag applied to the task |

```
insert content://org.tasks.api/v0/task_tags              // task_id + tag_id
delete content://org.tasks.api/v0/task_tags/{id}
delete content://org.tasks.api/v0/task_tags?task_id={id}&tag_id={id}
```

Inserting a pair that already exists returns the existing row's URI.

## Example

```kotlin
// Add
val row = resolver.insert(
    "content://org.tasks.api/v0/task_tags".toUri(),
    contentValuesOf("task_id" to taskId, "tag_id" to tagId),
)!!

// Read back off the task
val tags = resolver.query(
    "content://org.tasks.api/v0/tasks/$taskId".toUri(), arrayOf("tag_ids"), null, null, null
)!!.use { it.moveToFirst(); it.getString(0) }.split(",").filter { it.isNotEmpty() }.map(String::toLong)

// Remove by natural key
resolver.delete(
    "content://org.tasks.api/v0/task_tags?task_id=$taskId&tag_id=$tagId".toUri(), null, null,
)
```

---

# Lists

```
content://org.tasks.api/v0/lists
content://org.tasks.api/v0/lists/{id}
```

| Parameter | Type | Description |
| --- | --- | --- |
| `_id` | long | Fetch specific lists. Repeatable |
| `account_id` | long | Repeatable |
| `access` | enum | `owner`, `read_write`, `read_only`. Repeatable |
| `limit` / `offset` | int | `limit` defaults to 100; no maximum |

### Columns

| Column | Type | W | Description |
| --- | --- | :-: | --- |
| `_id` | long |  | Row id. Local to this install |
| `title` | string | • | List name |
| `color` | int | • | ARGB, 0 when unset |
| `icon` | string | • | [Icon name](#icons), `""` when unset |
| `order` | int |  | Display order |
| `access` | string |  | `owner`, `read_write`, `read_only` |
| `account_id` | long | insert | Account the list belongs to |

## Create, update, delete a list

```
insert content://org.tasks.api/v0/lists          // account_id + title required
update content://org.tasks.api/v0/lists/{id}     // title, color, icon
delete content://org.tasks.api/v0/lists/{id}
```

Requires `WRITE_TASKS`. On CalDAV, Google, Microsoft and Etebase accounts, all three block
on a network round trip. Failures, including timeouts, throw `IllegalStateException`
carrying the server's message. Remote calls run one at a time, so a call can block behind
another.

This is the one insert that is not idempotent: a retry after a timeout can create a second
list. Re-list `/v0/lists` and match on `title` before retrying.

## Example

```kotlin
// Pick an account
val accountId = resolver.query(
    "content://org.tasks.api/v0/accounts".toUri(), null, null, null, null
)!!.use { c ->
    val id = c.getColumnIndexOrThrow("_id")
    val type = c.getColumnIndexOrThrow("type")
    generateSequence { if (c.moveToNext()) c else null }
        .first { it.getString(type) == "local" }
        .getLong(id)
}

// Create
val created = resolver.insert(
    "content://org.tasks.api/v0/lists".toUri(),
    contentValuesOf("account_id" to accountId, "title" to "Groceries", "color" to 0),
)!!
val listId = ContentUris.parseId(created)

// Rename or recolor
resolver.update(created, contentValuesOf("title" to "Shopping"), null, null)

// Delete, along with its tasks
resolver.delete(created, null, null)
```

---

# Tags

```
content://org.tasks.api/v0/tags
content://org.tasks.api/v0/tags/{id}
```

| Parameter | Type | Description |
| --- | --- | --- |
| `_id` | long | Fetch specific tags. Repeatable |
| `limit` / `offset` | int | `limit` defaults to 100; no maximum |

| Column | Type | W | Description |
| --- | --- | :-: | --- |
| `_id` | long |  | Row id. Local to this install |
| `name` | string | • | Tag name |
| `color` | int | • | ARGB, 0 when unset |
| `icon` | string | • | [Icon name](#icons), `""` when unset |
| `order` | int |  | Display order in the app's drawer |

```
insert content://org.tasks.api/v0/tags        // name required; color, icon optional
update content://org.tasks.api/v0/tags/{id}   // rename, recolor, change icon
delete content://org.tasks.api/v0/tags/{id}   // also removes it from every task
```

Tag names are unique case-insensitively. Inserting an existing name returns that tag's URI
and leaves its color and icon unchanged.

Renaming onto a name another tag holds throws `IllegalArgumentException`, with that tag's
`_id` in the message. There is no merge.

## Example

```kotlin
// Create
val created = resolver.insert(
    "content://org.tasks.api/v0/tags".toUri(),
    contentValuesOf("name" to "admin", "color" to 0xFF4CAF50.toInt()),
)!!
val tagId = ContentUris.parseId(created)

// Rename or recolor
resolver.update(created, contentValuesOf("name" to "ops", "color" to 0), null, null)

// Delete. Does not come back on the next sync
resolver.delete(created, null, null)
```

---

# Places

Saved locations.

```
content://org.tasks.api/v0/places
content://org.tasks.api/v0/places/{id}
```

| Parameter | Type | Description |
| --- | --- | --- |
| `_id` | long | Fetch specific places. Repeatable |
| `limit` / `offset` | int | `limit` defaults to 100; no maximum |

| Column | Type | W | Description |
| --- | --- | :-: | --- |
| `_id` | long |  | Row id. Local to this install |
| `name` | string | • | `""` when unset |
| `display_name` | string |  | Name, falling back to address, falling back to coordinates |
| `address` | string | • | Street address as saved |
| `phone` | string | • | Phone number saved on the place |
| `url` | string | • | URL saved on the place |
| `latitude` | double | insert | Decimal degrees |
| `longitude` | double | insert | Decimal degrees |
| `radius` | int | • | Trigger radius in meters for location reminders, default 250 |
| `color` | int | • | ARGB, 0 when unset |
| `icon` | string | • | [Icon name](#icons), `""` when unset |
| `order` | int |  | Display order in the app's drawer |

```
insert content://org.tasks.api/v0/places       // latitude + longitude required
update content://org.tasks.api/v0/places/{id}
delete content://org.tasks.api/v0/places/{id}  // also unfiles its tasks and drops their reminders
```

Places are unique by coordinate. Inserting coordinates that match a saved place returns that
place's URI. There is no geocoding; supply coordinates.

## Example

```kotlin
// Create
val created = resolver.insert(
    "content://org.tasks.api/v0/places".toUri(),
    contentValuesOf(
        "name" to "Home",
        "address" to "221B Baker Street",
        "latitude" to 51.5237,
        "longitude" to -0.1585,
        "radius" to 150,
    ),
)!!
val placeId = ContentUris.parseId(created)

// Update
resolver.update(created, contentValuesOf("name" to "Home (new)"), null, null)

// Delete
resolver.delete(created, null, null)
```

---

# Accounts

Read-only.

```
content://org.tasks.api/v0/accounts
content://org.tasks.api/v0/accounts/{id}
```

| Parameter | Type | Description |
| --- | --- | --- |
| `_id` | long | Fetch specific accounts. Repeatable |
| `limit` / `offset` | int | `limit` defaults to 100; no maximum |

| Column | Type | W | Description |
| --- | --- | :-: | --- |
| `_id` | long |  | Row id. Local to this install |
| `name` | string |  | Display name |
| `type` | string |  | `caldav`, `tasks_org`, `google_tasks`, `microsoft`, `etebase`, `opentasks`, `local` |
| `username` | string |  | Account username, `""` for local accounts |
| `url` | string |  | Server URL, `""` for local |
| `error` | string |  | Why sync last failed, `""` when healthy — see below |
| `repeats_on_server` | 0/1 |  | The server advances recurring tasks itself — see [Recurring tasks](#recurring-tasks) |

Accounts are added and removed in the app.

`error` is a coarse code, not the server's message:

| Value | Means |
| --- | --- |
| `""` | Healthy |
| `unauthorized` | The account has to sign in again |
| `payment_required` | The subscription lapsed |
| `terms_required` | The server is waiting on terms being accepted |
| `failed` | Anything else — network, server, or a bad response |

## Example

```kotlin
resolver.query("content://org.tasks.api/v0/accounts".toUri(), null, null, null, null)!!.use { c ->
    val name = c.getColumnIndexOrThrow("name")
    val type = c.getColumnIndexOrThrow("type")
    val error = c.getColumnIndexOrThrow("error")
    while (c.moveToNext()) {
        val loggedOut = c.getString(error).isNotEmpty()
        println("${c.getString(name)} (${c.getString(type)})${if (loggedOut) " — needs attention" else ""}")
    }
}
```

---

# Batch operations

`applyBatch` runs operations in one transaction (all apply or none) and lets later
operations reference ids from earlier ones with `withValueBackReference`.

A back-reference resolves into a *column*. Reference inserts only: an update or delete
returns a row count.

Target `task_id`, `tag_id`, `place_id` and `parent_id`. `list_id` takes a literal.

```kotlin
val ops = arrayListOf(
    // 0 — the tag
    ContentProviderOperation.newInsert("content://org.tasks.api/v0/tags".toUri())
        .withValue("name", "admin")
        .build(),
    // 1 — the task
    ContentProviderOperation.newInsert("content://org.tasks.api/v0/tasks".toUri())
        .withValue("title", "Renew passport")
        .withValue("due_date", dueMillis)
        .withValue("due_all_day", 1)
        .withValue("list_id", listId)
        .build(),
    ContentProviderOperation.newInsert("content://org.tasks.api/v0/alarms".toUri())
        .withValueBackReference("task_id", 1)      // the id from operation 1
        .withValue("type", "relative_due")
        .withValue("offset_ms", -TimeUnit.DAYS.toMillis(7))
        .build(),
    ContentProviderOperation.newInsert("content://org.tasks.api/v0/task_tags".toUri())
        .withValueBackReference("task_id", 1)
        .withValueBackReference("tag_id", 0)       // the tag from operation 0
        .build(),
)
val results = resolver.applyBatch("org.tasks.api", ops)
```

Change notifications fire once at the end of a batch, not per operation.

A batch is one binder transaction, limited to roughly a megabyte for the operations and
their results together. Exceeding it throws `TransactionTooLargeException`, often wrapped in
a `RuntimeException`.

An update that changes nothing (a stale `if_modified_at`, or a missing row) returns 0 and
does not abort the batch. Add `.withExpectedCount(1)` to any operation whose failure should
roll it back.

Insert, update and delete on `/v0/lists` throw `IllegalArgumentException` in a batch.

---

# Fetching related rows

Reminders need a second query, scoped to the ids from the first:

```kotlin
val tasks = resolver.query(
    "content://org.tasks.api/v0/tasks?completed=0&limit=100".toUri(),
    null, null, null, null
)!!.use { c ->
    val id = c.getColumnIndexOrThrow("_id")
    val title = c.getColumnIndexOrThrow("title")
    val tags = c.getColumnIndexOrThrow("tag_ids")
    buildList {
        while (c.moveToNext()) {
            add(Triple(
                c.getLong(id),
                c.getString(title),
                c.getString(tags).split(",").filter { it.isNotEmpty() },
            ))
        }
    }
}

// Size the limit for the rows coming back, not the ids going out
val idParams = tasks.joinToString("&") { (id, _, _) -> "task_id=$id" }
val alarmsByTask: Map<Long, List<String>> = resolver.query(
    "content://org.tasks.api/v0/alarms?$idParams&limit=1000".toUri(), null, null, null, null
)!!.use { c ->
    val taskId = c.getColumnIndexOrThrow("task_id")
    val type = c.getColumnIndexOrThrow("type")
    buildMap<Long, MutableList<String>> {
        while (c.moveToNext()) {
            getOrPut(c.getLong(taskId)) { mutableListOf() }.add(c.getString(type))
        }
    }
}
```

---

# Changelog

Newest first, headed `versionName (versionCode)`.

## Unreleased

- Initial `v0` API.
