package org.tasks.filters

import android.content.Context
import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.Criterion.Companion.or
import com.todoroo.andlib.sql.Field.Companion.field
import com.todoroo.andlib.sql.Join.Companion.inner
import com.todoroo.andlib.sql.Join.Companion.left
import com.todoroo.andlib.sql.Query.Companion.select
import com.todoroo.andlib.sql.UnaryCriterion.Companion.isNotNull
import com.todoroo.astrid.api.*
import com.todoroo.astrid.data.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.data.*
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible
import java.util.*
import javax.inject.Inject

class FilterCriteriaProvider @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val tagDataDao: TagDataDao,
        private val googleTaskListDao: GoogleTaskListDao,
        private val caldavDao: CaldavDao) {
    private val r = context.resources

    suspend fun getFilterCriteria(identifier: String): CustomFilterCriterion = when (identifier) {
        IDENTIFIER_UNIVERSE -> startingUniverse
        IDENTIFIER_TITLE -> taskTitleContainsFilter
        IDENTIFIER_IMPORTANCE -> priorityFilter
        IDENTIFIER_DUEDATE -> dueDateFilter
        IDENTIFIER_GTASKS -> gtasksFilterCriteria()
        IDENTIFIER_CALDAV -> caldavFilterCriteria()
        IDENTIFIER_TAG_IS -> tagFilter()
        IDENTIFIER_TAG_CONTAINS -> tagNameContainsFilter
        IDENTIFIER_RECUR -> recurringFilter
        IDENTIFIER_COMPLETED -> completedFilter
        IDENTIFIER_HIDDEN -> hiddenFilter
        IDENTIFIER_PARENT -> parentFilter
        IDENTIFIER_SUBTASK -> subtaskFilter
        else -> throw RuntimeException("Unknown identifier: $identifier")
    }

    val startingUniverse: CustomFilterCriterion
        get() = MultipleSelectCriterion(
                IDENTIFIER_UNIVERSE,
                context.getString(R.string.BFE_Active),
                null,
                null,
                null,
                null,
                null)

    suspend fun all(): List<CustomFilterCriterion> {
        val result: MutableList<CustomFilterCriterion> = ArrayList()
        with(result) {
            add(tagFilter())
            add(tagNameContainsFilter)
            add(dueDateFilter)
            add(priorityFilter)
            add(taskTitleContainsFilter)
            if (googleTaskListDao.getAccounts().isNotEmpty()) {
                add(gtasksFilterCriteria())
            }
            add(caldavFilterCriteria())
            add(recurringFilter)
            add(completedFilter)
            add(hiddenFilter)
            add(parentFilter)
            add(subtaskFilter)
        }
        return result
    }

    // TODO: adding to hash set because duplicate tag name bug hasn't been fixed yet
    private suspend fun tagFilter(): CustomFilterCriterion {
        // TODO: adding to hash set because duplicate tag name bug hasn't been fixed yet
        val tagNames = tagDataDao
                .tagDataOrderedByName()
                .map(TagData::name)
                .distinct()
                .toTypedArray()
        val values: MutableMap<String, Any> = HashMap()
        values[Tag.KEY] = "?"
        return MultipleSelectCriterion(
                IDENTIFIER_TAG_IS,
                context.getString(R.string.CFC_tag_text),
                select(Tag.TASK)
                        .from(Tag.TABLE)
                        .join(inner(Task.TABLE, Tag.TASK.eq(Task.ID)))
                        .where(and(activeAndVisible(), Tag.NAME.eq("?")))
                        .toString(),
                values,
                tagNames,
                tagNames,
                context.getString(R.string.CFC_tag_name))
    }

    private val recurringFilter: CustomFilterCriterion
        get() = BooleanCriterion(
                    IDENTIFIER_RECUR,
                    context.getString(R.string.repeats_single, "").trim(),
                    select(Task.ID)
                            .from(Task.TABLE)
                            .where(field("LENGTH(${Task.RECURRENCE})>0").eq(1))
                            .toString()
            )

    private val completedFilter: CustomFilterCriterion
        get() = BooleanCriterion(
                IDENTIFIER_COMPLETED,
                context.getString(R.string.rmd_NoA_done),
                select(Task.ID)
                        .from(Task.TABLE)
                        .where(field("${Task.COMPLETION_DATE.lt(1)}").eq(0))
                        .toString()
        )

    private val hiddenFilter: CustomFilterCriterion
        get() = BooleanCriterion(
                IDENTIFIER_HIDDEN,
                context.getString(R.string.widget_due_date_hidden),
                select(Task.ID)
                        .from(Task.TABLE)
                        .where(field("${Task.HIDE_UNTIL.gt(PermaSql.VALUE_NOW)}").eq(1))
                        .toString()
        )

    private val parentFilter: CustomFilterCriterion
    get() = BooleanCriterion(
            IDENTIFIER_PARENT,
            context.getString(R.string.custom_filter_has_subtask),
            select(Task.ID)
                    .from(Task.TABLE)
                    .join(left(Task.TABLE.`as`("children"), Task.ID.eq(field("children.parent"))))
                    .join(left(GoogleTask.TABLE, GoogleTask.PARENT.eq(Task.ID)))
                    .where(or(
                            isNotNull(field("children._id")),
                            isNotNull(GoogleTask.ID)
                    ))
                    .toString()
    )

    private val subtaskFilter: CustomFilterCriterion
        get() = BooleanCriterion(
                IDENTIFIER_SUBTASK,
                context.getString(R.string.custom_filter_is_subtask),
                select(Task.ID)
                        .from(Task.TABLE)
                        .join(left(GoogleTask.TABLE, GoogleTask.TASK.eq(Task.ID)))
                        .where(or(
                                field("${Task.PARENT}>0").eq(1),
                                field("${GoogleTask.PARENT}>0").eq(1)
                        ))
                        .toString()
        )

    val tagNameContainsFilter: CustomFilterCriterion
        get() = TextInputCriterion(
                IDENTIFIER_TAG_CONTAINS,
                context.getString(R.string.CFC_tag_contains_text),
                select(Tag.TASK)
                        .from(Tag.TABLE)
                        .join(inner(Task.TABLE, Tag.TASK.eq(Task.ID)))
                        .where(and(activeAndVisible(), Tag.NAME.like("%?%")))
                        .toString(),
                context.getString(R.string.CFC_tag_contains_name),
                "",
                context.getString(R.string.CFC_tag_contains_name))

    val dueDateFilter: CustomFilterCriterion
        get() {
            val entryValues = arrayOf(
                    "0",
                    PermaSql.VALUE_EOD_YESTERDAY,
                    PermaSql.VALUE_EOD,
                    PermaSql.VALUE_EOD_TOMORROW,
                    PermaSql.VALUE_EOD_DAY_AFTER,
                    PermaSql.VALUE_EOD_NEXT_WEEK,
                    PermaSql.VALUE_EOD_NEXT_MONTH)
            val values: MutableMap<String?, Any> = HashMap()
            values[Task.DUE_DATE.name] = "?"
            return MultipleSelectCriterion(
                    IDENTIFIER_DUEDATE,
                    r.getString(R.string.CFC_dueBefore_text),
                    select(Task.ID)
                            .from(Task.TABLE)
                            .where(
                                    and(
                                            activeAndVisible(),
                                            or(field("?").eq(0), Task.DUE_DATE.gt(0)),
                                            Task.DUE_DATE.lte("?")))
                            .toString(),
                    values,
                    r.getStringArray(R.array.CFC_dueBefore_entries),
                    entryValues,
                    r.getString(R.string.CFC_dueBefore_name))
        }

    val priorityFilter: CustomFilterCriterion
        get() {
            val entryValues = arrayOf(
                    Task.Priority.HIGH.toString(),
                    Task.Priority.MEDIUM.toString(),
                    Task.Priority.LOW.toString(),
                    Task.Priority.NONE.toString())
            val entries = arrayOf("!!!", "!!", "!", "o")
            val values: MutableMap<String?, Any> = HashMap()
            values[Task.IMPORTANCE.name] = "?"
            return MultipleSelectCriterion(
                    IDENTIFIER_IMPORTANCE,
                    r.getString(R.string.CFC_importance_text),
                    select(Task.ID)
                            .from(Task.TABLE)
                            .where(and(activeAndVisible(), Task.IMPORTANCE.lte("?")))
                            .toString(),
                    values,
                    entries,
                    entryValues,
                    r.getString(R.string.CFC_importance_name))
        }

    private val taskTitleContainsFilter: CustomFilterCriterion
        get() = TextInputCriterion(
                IDENTIFIER_TITLE,
                r.getString(R.string.CFC_title_contains_text),
                select(Task.ID)
                        .from(Task.TABLE)
                        .where(and(activeAndVisible(), Task.TITLE.like("%?%")))
                        .toString(),
                r.getString(R.string.CFC_title_contains_name),
                "",
                r.getString(R.string.CFC_title_contains_name))

    private suspend fun gtasksFilterCriteria(): CustomFilterCriterion {
        val lists = googleTaskListDao.getAllLists()
        val listNames = arrayOfNulls<String>(lists.size)
        val listIds = arrayOfNulls<String>(lists.size)
        for (i in lists.indices) {
            listNames[i] = lists[i].title
            listIds[i] = lists[i].remoteId
        }
        val values: MutableMap<String, Any> = HashMap()
        values[GoogleTask.KEY] = "?"
        return MultipleSelectCriterion(
                IDENTIFIER_GTASKS,
                context.getString(R.string.CFC_gtasks_list_text),
                select(GoogleTask.TASK)
                        .from(GoogleTask.TABLE)
                        .join(inner(Task.TABLE, GoogleTask.TASK.eq(Task.ID)))
                        .where(
                                and(
                                        activeAndVisible(),
                                        GoogleTask.DELETED.eq(0),
                                        GoogleTask.LIST.eq("?")))
                        .toString(),
                values,
                listNames,
                listIds,
                context.getString(R.string.CFC_gtasks_list_name))
    }

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
                context.getString(R.string.CFC_gtasks_list_text),
                select(CaldavTask.TASK)
                        .from(CaldavTask.TABLE)
                        .join(inner(Task.TABLE, CaldavTask.TASK.eq(Task.ID)))
                        .where(
                                and(
                                        activeAndVisible(),
                                        CaldavTask.DELETED.eq(0),
                                        CaldavTask.CALENDAR.eq("?")))
                        .toString(),
                values,
                names,
                ids,
                context.getString(R.string.CFC_list_name))
    }

    companion object {
        private const val IDENTIFIER_UNIVERSE = "active"
        private const val IDENTIFIER_TITLE = "title"
        private const val IDENTIFIER_IMPORTANCE = "importance"
        private const val IDENTIFIER_DUEDATE = "dueDate"
        private const val IDENTIFIER_GTASKS = "gtaskslist"
        private const val IDENTIFIER_CALDAV = "caldavlist"
        private const val IDENTIFIER_TAG_IS = "tag_is"
        private const val IDENTIFIER_TAG_CONTAINS = "tag_contains"
        private const val IDENTIFIER_RECUR = "recur"
        private const val IDENTIFIER_COMPLETED = "completed"
        private const val IDENTIFIER_HIDDEN = "hidden"
        private const val IDENTIFIER_PARENT = "parent"
        private const val IDENTIFIER_SUBTASK = "subtask"
    }
}