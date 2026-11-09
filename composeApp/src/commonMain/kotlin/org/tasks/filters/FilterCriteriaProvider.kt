package org.tasks.filters

import com.todoroo.astrid.api.PermaSql
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Filter
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion.Companion.and
import org.tasks.data.sql.Criterion.Companion.exists
import org.tasks.data.sql.Criterion.Companion.or
import org.tasks.data.sql.Field.Companion.field
import org.tasks.data.sql.Join.Companion.inner
import org.tasks.data.sql.Query.Companion.select
import org.tasks.data.sql.UnaryCriterion.Companion.isNotNull
import org.tasks.resources.getString
import org.tasks.resources.getStringArray
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.BFE_Active
import tasks.kmp.generated.resources.CFC_dueBefore_entries
import tasks.kmp.generated.resources.CFC_dueBefore_name
import tasks.kmp.generated.resources.CFC_dueBefore_text
import tasks.kmp.generated.resources.CFC_gtasks_list_text
import tasks.kmp.generated.resources.CFC_importance_name
import tasks.kmp.generated.resources.CFC_importance_text
import tasks.kmp.generated.resources.CFC_list_name
import tasks.kmp.generated.resources.CFC_startBefore_entries
import tasks.kmp.generated.resources.CFC_startBefore_name
import tasks.kmp.generated.resources.CFC_startBefore_text
import tasks.kmp.generated.resources.CFC_tag_contains_name
import tasks.kmp.generated.resources.CFC_tag_contains_text
import tasks.kmp.generated.resources.CFC_tag_name
import tasks.kmp.generated.resources.CFC_tag_text
import tasks.kmp.generated.resources.CFC_title_contains_name
import tasks.kmp.generated.resources.CFC_title_contains_text
import tasks.kmp.generated.resources.custom_filter_has_reminder
import tasks.kmp.generated.resources.custom_filter_has_subtask
import tasks.kmp.generated.resources.custom_filter_is_subtask
import tasks.kmp.generated.resources.filter_criteria_unstarted
import tasks.kmp.generated.resources.repeats_single
import tasks.kmp.generated.resources.rmd_NoA_done

/**
 * The catalog of criterion types a user can add to a custom filter
 * (desktop port of the Android app's `org.tasks.filters.FilterCriteriaProvider`).
 *
 * Semantically identical to the Android original so that filters built on the
 * desktop produce the same SQL as on Android; the only differences are that
 * strings come from the shared compose resources (through the non-composable
 * [getString]/[getStringArray] helpers) instead of Android resources, and
 * there is no `Context` (compose is the platform).
 *
 * All accessors are suspend because string resolution is.
 */
@Suppress("TooManyFunctions")
class FilterCriteriaProvider(
    private val tagDataDao: TagDataDao,
    private val caldavDao: CaldavDao,
) {
    suspend fun rebuildFilter(filter: Filter): Filter {
        val serialized = filter.criterion?.takeIf { it.isNotBlank() }
        val criterion = fromString(serialized)
        return filter.copy(
            sql = criterion.sql,
            criterion = CriterionInstance.serialize(criterion),
        )
    }

    suspend fun fromString(criterion: String?): List<CriterionInstance> {
        if (criterion.isNullOrBlank()) {
            return emptyList()
        }
        val entries = mutableListOf<CriterionInstance>()
        for (row in criterion.trim().split("\n")) {
            val split = row
                .split(SERIALIZATION_SEPARATOR)
                .map { unescape(it) }
            if (split.size != 4 && split.size != 5) {
                return emptyList()
            }
            val entry = CriterionInstance()
            val c = getFilterCriteria(split[0])
            entry.criterion = c
            val value = split[1]
            when (c) {
                is TextInputCriterion -> entry.selectedText = value
                is MultipleSelectCriterion ->
                    entry.selectedIndex = c.entryValues?.indexOf(value) ?: -1
                else -> Unit
            }
            entry.type = split[3].toInt()
            entries.add(entry)
        }
        return entries
    }

    private suspend fun getFilterCriteria(identifier: String): CustomFilterCriterion =
        when (identifier) {
            IDENTIFIER_UNIVERSE -> startingUniverse()
            IDENTIFIER_TITLE -> taskTitleContainsFilter()
            IDENTIFIER_IMPORTANCE -> priorityFilter()
            IDENTIFIER_STARTDATE -> startDateFilter()
            IDENTIFIER_DUEDATE -> dueDateFilter()
            IDENTIFIER_GTASKS, IDENTIFIER_CALDAV -> caldavFilterCriteria()
            IDENTIFIER_TAG_IS -> tagFilter()
            IDENTIFIER_TAG_CONTAINS -> tagNameContainsFilter()
            IDENTIFIER_RECUR -> recurringFilter()
            IDENTIFIER_COMPLETED -> completedFilter()
            IDENTIFIER_HIDDEN -> hiddenFilter()
            IDENTIFIER_PARENT -> parentFilter()
            IDENTIFIER_SUBTASK -> subtaskFilter()
            IDENTIFIER_REMINDERS -> reminderFilter()
            else -> throw IllegalArgumentException("Unknown identifier: $identifier")
        }

    suspend fun startingUniverse(): CustomFilterCriterion = MultipleSelectCriterion(
        IDENTIFIER_UNIVERSE,
        getString(Res.string.BFE_Active),
        null,
        null,
        null,
        null,
        null,
    )

    suspend fun all(): List<CustomFilterCriterion> = listOf(
        tagFilter(),
        tagNameContainsFilter(),
        startDateFilter(),
        dueDateFilter(),
        priorityFilter(),
        taskTitleContainsFilter(),
        caldavFilterCriteria(),
        recurringFilter(),
        completedFilter(),
        hiddenFilter(),
        parentFilter(),
        subtaskFilter(),
        reminderFilter(),
    )

    // TODO: adding to hash set because duplicate tag name bug hasn't been fixed yet
    private suspend fun tagFilter(): CustomFilterCriterion {
        val tagNames = tagDataDao
            .tagDataOrderedByName()
            .map(TagData::name)
            .distinct()
            .toTypedArray()
        val values: MutableMap<String, Any> = HashMap()
        values[Tag.KEY] = "?"
        return MultipleSelectCriterion(
            IDENTIFIER_TAG_IS,
            getString(Res.string.CFC_tag_text),
            select(Tag.TASK)
                .from(Tag.TABLE)
                .join(inner(Task.TABLE, Tag.TASK.eq(Task.ID)))
                .where(and(activeAndVisible(), Tag.NAME.eq("?")))
                .toString(),
            values,
            tagNames,
            tagNames,
            getString(Res.string.CFC_tag_name),
        )
    }

    private suspend fun recurringFilter(): CustomFilterCriterion =
        BooleanCriterion(
            IDENTIFIER_RECUR,
            getString(Res.string.repeats_single, "").trim(),
            select(Task.ID)
                .from(Task.TABLE)
                .where(field("LENGTH(${Task.RECURRENCE})>0").eq(1))
                .toString(),
        )

    private suspend fun completedFilter(): CustomFilterCriterion =
        BooleanCriterion(
            IDENTIFIER_COMPLETED,
            getString(Res.string.rmd_NoA_done),
            select(Task.ID)
                .from(Task.TABLE)
                .where(field("${Task.COMPLETION_DATE.lt(1)}").eq(0))
                .toString(),
        )

    private suspend fun hiddenFilter(): CustomFilterCriterion =
        BooleanCriterion(
            IDENTIFIER_HIDDEN,
            getString(Res.string.filter_criteria_unstarted),
            select(Task.ID)
                .from(Task.TABLE)
                .where(field("${Task.HIDE_UNTIL.gt(PermaSql.VALUE_NOW)}").eq(1))
                .toString(),
        )

    private suspend fun parentFilter(): CustomFilterCriterion =
        BooleanCriterion(
            IDENTIFIER_PARENT,
            getString(Res.string.custom_filter_has_subtask),
            select(Task.ID)
                .from(Task.TABLE)
                .join(inner(Task.TABLE.`as`("children"), Task.ID.eq(field("children.parent"))))
                .where(isNotNull(field("children._id")))
                .toString(),
        )

    private suspend fun subtaskFilter(): CustomFilterCriterion =
        BooleanCriterion(
            IDENTIFIER_SUBTASK,
            getString(Res.string.custom_filter_is_subtask),
            select(Task.ID)
                .from(Task.TABLE)
                .where(field("${Task.PARENT}>0").eq(1))
                .toString(),
        )

    private suspend fun reminderFilter(): CustomFilterCriterion =
        BooleanCriterion(
            IDENTIFIER_REMINDERS,
            getString(Res.string.custom_filter_has_reminder),
            select(Task.ID)
                .from(Task.TABLE)
                .where(exists(select(ONE).from(Alarm.TABLE).where(Alarm.TASK.eq(Task.ID))))
                .toString(),
        )

    suspend fun tagNameContainsFilter(): CustomFilterCriterion = TextInputCriterion(
        IDENTIFIER_TAG_CONTAINS,
        getString(Res.string.CFC_tag_contains_text),
        select(Tag.TASK)
            .from(Tag.TABLE)
            .join(inner(Task.TABLE, Tag.TASK.eq(Task.ID)))
            .where(and(activeAndVisible(), Tag.NAME.like("%?%")))
            .toString(),
        getString(Res.string.CFC_tag_contains_name),
        "",
        getString(Res.string.CFC_tag_contains_name),
    )

    suspend fun dueDateFilter(): CustomFilterCriterion = MultipleSelectCriterion(
        IDENTIFIER_DUEDATE,
        getString(Res.string.CFC_dueBefore_text),
        select(Task.ID)
            .from(Task.TABLE)
            .where(
                and(
                    activeAndVisible(),
                    or(field("?").eq(0), Task.DUE_DATE.gt(0)),
                    // Find tasks that have due dates before the specified date, or all-day
                    // tasks that have due dates before EOD today if the specified date is NOW
                    or(
                        Task.DUE_DATE.lte("?"),
                        and(
                            field("${Task.DUE_DATE} / 1000 % 60").eq(0),
                            field("?").eq(field(PermaSql.VALUE_NOW)),
                            Task.DUE_DATE.lte(PermaSql.VALUE_EOD),
                        ),
                    ),
                ),
            )
            .toString(),
        mapOf(Task.DUE_DATE.name to "?"),
        getStringArray(Res.array.CFC_dueBefore_entries).toTypedArray(),
        arrayOf(
            "0",
            PermaSql.VALUE_EOD_YESTERDAY,
            PermaSql.VALUE_EOD,
            PermaSql.VALUE_EOD_TOMORROW,
            PermaSql.VALUE_EOD_DAY_AFTER,
            PermaSql.VALUE_EOD_NEXT_WEEK,
            PermaSql.VALUE_EOD_NEXT_MONTH,
            PermaSql.VALUE_NOW,
        ),
        getString(Res.string.CFC_dueBefore_name),
    )

    suspend fun startDateFilter(): CustomFilterCriterion = MultipleSelectCriterion(
        IDENTIFIER_STARTDATE,
        getString(Res.string.CFC_startBefore_text),
        select(Task.ID)
            .from(Task.TABLE)
            .where(
                and(
                    activeAndVisible(),
                    or(field("?").eq(0), Task.HIDE_UNTIL.gt(0)),
                    Task.HIDE_UNTIL.lte("?"),
                ),
            )
            .toString(),
        mapOf(Task.HIDE_UNTIL.name to "?"),
        getStringArray(Res.array.CFC_startBefore_entries).toTypedArray(),
        arrayOf(
            "0",
            PermaSql.VALUE_EOD_YESTERDAY,
            PermaSql.VALUE_EOD,
            PermaSql.VALUE_EOD_TOMORROW,
            PermaSql.VALUE_EOD_DAY_AFTER,
            PermaSql.VALUE_EOD_NEXT_WEEK,
            PermaSql.VALUE_EOD_NEXT_MONTH,
            PermaSql.VALUE_NOW,
        ),
        getString(Res.string.CFC_startBefore_name),
    )

    suspend fun priorityFilter(): CustomFilterCriterion = MultipleSelectCriterion(
        IDENTIFIER_IMPORTANCE,
        getString(Res.string.CFC_importance_text),
        select(Task.ID)
            .from(Task.TABLE)
            .where(and(activeAndVisible(), Task.IMPORTANCE.lte("?")))
            .toString(),
        mapOf(Task.IMPORTANCE.name to "?"),
        arrayOf("!!!", "!!", "!", "o"),
        arrayOf(
            Task.Priority.HIGH.toString(),
            Task.Priority.MEDIUM.toString(),
            Task.Priority.LOW.toString(),
            Task.Priority.NONE.toString(),
        ),
        getString(Res.string.CFC_importance_name),
    )

    private suspend fun taskTitleContainsFilter(): CustomFilterCriterion = TextInputCriterion(
        IDENTIFIER_TITLE,
        getString(Res.string.CFC_title_contains_text),
        select(Task.ID)
            .from(Task.TABLE)
            .where(and(activeAndVisible(), Task.TITLE.like("%?%")))
            .toString(),
        getString(Res.string.CFC_title_contains_name),
        "",
        getString(Res.string.CFC_title_contains_name),
    )

    private suspend fun caldavFilterCriteria(): CustomFilterCriterion {
        val calendars = caldavDao.getCalendars()
        val names = arrayOfNulls<String>(calendars.size)
        val ids = arrayOfNulls<String>(calendars.size)
        for (i in calendars.indices) {
            names[i] = calendars[i].name
            ids[i] = calendars[i].uuid
        }
        val values: MutableMap<String, Any> = HashMap()
        values[CaldavTask.KEY] = "?"
        return MultipleSelectCriterion(
            IDENTIFIER_CALDAV,
            getString(Res.string.CFC_gtasks_list_text),
            select(CaldavTask.TASK)
                .from(CaldavTask.TABLE)
                .join(inner(Task.TABLE, CaldavTask.TASK.eq(Task.ID)))
                .where(
                    and(
                        activeAndVisible(),
                        CaldavTask.DELETED.eq(0),
                        CaldavTask.CALENDAR.eq("?"),
                    ),
                )
                .toString(),
            values,
            names,
            ids,
            getString(Res.string.CFC_list_name),
        )
    }

    companion object {
        const val IDENTIFIER_UNIVERSE = "active"
        const val IDENTIFIER_TITLE = "title"
        const val IDENTIFIER_IMPORTANCE = "importance"
        const val IDENTIFIER_STARTDATE = "startDate"
        const val IDENTIFIER_DUEDATE = "dueDate"
        const val IDENTIFIER_GTASKS = "gtaskslist"
        const val IDENTIFIER_CALDAV = "caldavlist"
        const val IDENTIFIER_TAG_IS = "tag_is"
        const val IDENTIFIER_TAG_CONTAINS = "tag_contains"
        const val IDENTIFIER_RECUR = "recur"
        const val IDENTIFIER_COMPLETED = "completed"
        const val IDENTIFIER_HIDDEN = "hidden"
        const val IDENTIFIER_PARENT = "parent"
        const val IDENTIFIER_SUBTASK = "subtask"
        const val IDENTIFIER_REMINDERS = "reminders"

        private val ONE = field("1")

        private fun unescape(item: String): String =
            item.replace(SEPARATOR_ESCAPE, SERIALIZATION_SEPARATOR)
    }
}
