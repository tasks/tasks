package com.todoroo.astrid.data

import android.os.Parcelable
import androidx.annotation.IntDef
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.todoroo.andlib.data.Table
import com.todoroo.andlib.sql.Field
import com.todoroo.andlib.utility.DateUtilities
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import net.fortuna.ical4j.model.Recur
import org.tasks.Strings
import org.tasks.data.Tag
import org.tasks.date.DateTimeUtils
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.time.DateTimeUtils.startOfDay
import timber.log.Timber

@Parcelize
@Entity(
        tableName = Task.TABLE_NAME,
        indices = [
            Index(name = "t_rid", value = ["remoteId"], unique = true),
            Index(name = "active_and_visible", value = ["completed", "deleted", "hideUntil"])])
data class Task(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @Transient
    var id: Long = NO_ID,
    @ColumnInfo(name = "title")
    var title: String? = null,
    @ColumnInfo(name = "importance")
    var priority: Int = Priority.NONE,
    @ColumnInfo(name = "dueDate")
    var dueDate: Long = 0L,
    @ColumnInfo(name = "hideUntil")
    var hideUntil: Long = 0L,
    @ColumnInfo(name = "created")
    var creationDate: Long = 0L,
    @ColumnInfo(name = "modified")
    var modificationDate: Long = 0L,
    @ColumnInfo(name = "completed")
    var completionDate: Long = 0L,
    @ColumnInfo(name = "deleted")
    var deletionDate: Long = 0L,
    @ColumnInfo(name = "notes")
    var notes: String? = null,
    @ColumnInfo(name = "estimatedSeconds")
    var estimatedSeconds: Int = 0,
    @ColumnInfo(name = "elapsedSeconds")
    var elapsedSeconds: Int = 0,
    @ColumnInfo(name = "timerStart")
    var timerStart: Long = 0L,
    @ColumnInfo(name = "notificationFlags")
    @SerializedName("ringFlags", alternate = ["reminderFlags"])
    var ringFlags: Int = 0,
    @ColumnInfo(name = "lastNotified")
    var reminderLast: Long = 0L,
    @ColumnInfo(name = "recurrence")
    var recurrence: String? = null,
    @ColumnInfo(name = "repeat_from", defaultValue = RepeatFrom.DUE_DATE.toString())
    var repeatFrom: Int = RepeatFrom.DUE_DATE,
    @ColumnInfo(name = "calendarUri")
    var calendarURI: String? = null,
    @ColumnInfo(name = "remoteId")
    var remoteId: String? = NO_UUID,
    @ColumnInfo(name = "collapsed")
    var isCollapsed: Boolean = false,
    @ColumnInfo(name = "parent")
    @Transient
    var parent: Long = 0L,
    @ColumnInfo(name = "order")
    var order: Long? = null,
    @ColumnInfo(name = "read_only", defaultValue = "0")
    var readOnly: Boolean = false,
    @Ignore
    @Transient
    private var transitoryData: @RawValue HashMap<String, Any>? = null,
) : Parcelable {
    var uuid: String
        get() = if (Strings.isNullOrEmpty(remoteId)) NO_UUID else remoteId!!
        set(uuid) {
            remoteId = uuid
        }

    /** Checks whether task is done. Requires COMPLETION_DATE  */
    val isCompleted
        get() = completionDate > 0

    /** Checks whether task is deleted. Will return false if DELETION_DATE not read  */
    val isDeleted
        get() = deletionDate > 0

    /** Checks whether task is hidden. Requires HIDDEN_UNTIL  */
    val isHidden
        get() = hideUntil > DateUtilities.now()

    fun hasStartTime() = hasDueTime(hideUntil)

    fun hasStartDate() = hideUntil > 0

    /** Checks whether task is done. Requires DUE_DATE  */
    fun hasDueDate() = dueDate > 0

    /**
     * Create hide until for this task.
     *
     * @param setting one of the HIDE_UNTIL_* constants
     * @param customDate if specific day is set, this value
     */
    fun createHideUntil(setting: Int, customDate: Long): Long {
        val date: Long = when (setting) {
            HIDE_UNTIL_NONE -> return 0
            HIDE_UNTIL_DUE, HIDE_UNTIL_DUE_TIME -> dueDate
            HIDE_UNTIL_DAY_BEFORE -> dueDate - DateUtilities.ONE_DAY
            HIDE_UNTIL_WEEK_BEFORE -> dueDate - DateUtilities.ONE_WEEK
            HIDE_UNTIL_SPECIFIC_DAY, HIDE_UNTIL_SPECIFIC_DAY_TIME -> customDate
            else -> throw IllegalArgumentException("Unknown setting $setting")
        }
        if (date <= 0) {
            return date
        }
        return if (setting == HIDE_UNTIL_SPECIFIC_DAY_TIME ||
                setting == HIDE_UNTIL_DUE_TIME && hasDueTime(dueDate)) {
            date.toDateTime().withSecondOfMinute(1).withMillisOfSecond(0).millis
        } else {
            date.startOfDay()
        }
    }

    /** Checks whether this due date has a due time or only a date  */
    fun hasDueTime(): Boolean = hasDueTime(dueDate)

    val isOverdue: Boolean
        get() {
            if (isCompleted || !hasDueDate()) {
                return false
            }
            val compareTo = if (hasDueTime()) DateUtilities.now() else DateTimeUtils.newDateTime().startOfDay().millis
            return dueDate < compareTo
        }

    fun repeatAfterCompletion(): Boolean = repeatFrom == RepeatFrom.COMPLETION_DATE

    fun setDueDateAdjustingHideUntil(newDueDate: Long) {
        if (dueDate > 0) {
            if (hideUntil > 0) {
                hideUntil = if (newDueDate > 0) hideUntil + newDueDate - dueDate else 0
            }
        }
        dueDate = newDueDate
    }

    val isRecurring: Boolean
        get() = recurrence?.isNotBlank() == true

    fun setRecurrence(rrule: Recur?) {
        recurrence = rrule?.toString()
    }

    fun hasNotes(): Boolean {
        return !Strings.isNullOrEmpty(notes)
    }

    val isNotifyModeNonstop: Boolean
        get() = ringFlags == NOTIFY_MODE_NONSTOP

    val isNotifyModeFive: Boolean
        get() = ringFlags == NOTIFY_MODE_FIVE

    val isNotifyAfterDeadline: Boolean
        get() = isReminderSet(NOTIFY_AFTER_DEADLINE)

    val isNotifyAtStart: Boolean
        get() = isReminderSet(NOTIFY_AT_START)

    val isNotifyAtDeadline: Boolean
        get() = isReminderSet(NOTIFY_AT_DEADLINE)

    private fun isReminderSet(flag: Int): Boolean {
        return ((transitoryData?.get(TRANS_REMINDERS) as? Int) ?: 0) and flag > 0
    }

    val isNew: Boolean
        get() = id == NO_ID

    fun insignificantChange(task: Task?): Boolean {
        if (this === task) {
            return true
        }
        return if (task == null) {
            false
        } else id == task.id
                && title == task.title
                && priority == task.priority
                && dueDate == task.dueDate
                && hideUntil == task.hideUntil
                && creationDate == task.creationDate
                && modificationDate == task.modificationDate
                && completionDate == task.completionDate
                && deletionDate == task.deletionDate
                && notes == task.notes
                && estimatedSeconds == task.estimatedSeconds
                && elapsedSeconds == task.elapsedSeconds
                && ringFlags == task.ringFlags
                && recurrence == task.recurrence
                && calendarURI == task.calendarURI
                && parent == task.parent
                && remoteId == task.remoteId
                && order == task.order
    }

    fun googleTaskUpToDate(original: Task?): Boolean {
        if (this === original) {
            return true
        }
        return if (original == null) {
            false
        } else title == original.title
                && dueDate == original.dueDate
                && completionDate == original.completionDate
                && deletionDate == original.deletionDate
                && parent == original.parent
                && notes == original.notes
                && order == original.order
    }

    fun caldavUpToDate(original: Task?): Boolean {
        if (this === original) {
            return true
        }
        return if (original == null) {
            false
        } else title == original.title
                && priority == original.priority
                && hideUntil == original.hideUntil
                && dueDate == original.dueDate
                && completionDate == original.completionDate
                && deletionDate == original.deletionDate
                && notes == original.notes
                && recurrence == original.recurrence
                && parent == original.parent
                && isCollapsed == original.isCollapsed
                && order == original.order
    }

    @Synchronized
    fun suppressSync() {
        putTransitory(SyncFlags.SUPPRESS_SYNC, true)
    }

    @Synchronized
    fun suppressRefresh() {
        putTransitory(TRANS_SUPPRESS_REFRESH, true)
    }

    fun isSuppressRefresh() = checkTransitory(TRANS_SUPPRESS_REFRESH)

    fun defaultReminders(flags: Int) {
        putTransitory(TRANS_REMINDERS, flags)
    }

    var randomReminder: Long
        get() = getTransitory(TRANS_RANDOM) ?: 0L
        set(value) = putTransitory(TRANS_RANDOM, value)

    @Synchronized
    fun putTransitory(key: String, value: Any) {
        if (transitoryData == null) {
            transitoryData = HashMap()
        }
        transitoryData!![key] = value
    }

    val tags: ArrayList<String>
        get() {
            return getTransitory(Tag.KEY) ?: ArrayList()
        }

    fun setTags(tags: ArrayList<String>) {
        if (transitoryData == null) {
            transitoryData = HashMap()
        }
        transitoryData!![Tag.KEY] = tags
    }

    fun hasTransitory(key: String?): Boolean {
        return transitoryData != null && transitoryData!!.containsKey(key)
    }

    fun <T> getTransitory(key: String?): T? = transitoryData?.get(key) as T?

    // --- Convenience wrappers for using transitories as flags
    fun checkTransitory(flag: String?): Boolean {
        val trans = getTransitory<Any>(flag)
        return trans != null
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.NONE)
    annotation class Priority {
        companion object {
            const val HIGH = 0
            const val MEDIUM = 1
            const val LOW = 2
            const val NONE = 3
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(RepeatFrom.DUE_DATE, RepeatFrom.COMPLETION_DATE)
    annotation class RepeatFrom {
        companion object {
            const val DUE_DATE = 0
            const val COMPLETION_DATE = 1
        }
    }

    companion object {
        const val TABLE_NAME = "tasks"
        // --- table and uri
        /** table for this model  */
        val TABLE = Table(TABLE_NAME)
        val FIELDS = Field.field("$TABLE_NAME.*")
        const val NO_ID: Long = 0

        // --- properties
        @JvmField val ID = TABLE.column("_id")
        @JvmField val TITLE = TABLE.column("title")
        val IMPORTANCE = TABLE.column("importance")
        val DUE_DATE = TABLE.column("dueDate")
        val HIDE_UNTIL = TABLE.column("hideUntil")
        @JvmField val MODIFICATION_DATE = TABLE.column("modified")
        @JvmField val CREATION_DATE = TABLE.column("created")
        val COMPLETION_DATE = TABLE.column("completed")
        @JvmField val DELETION_DATE = TABLE.column("deleted")
        val NOTES = TABLE.column("notes")
        val TIMER_START = TABLE.column("timerStart")
        val PARENT = TABLE.column("parent")
        val RECURRENCE = TABLE.column("recurrence")

        /** constant value for no uuid  */
        const val NO_UUID = "0" // $NON-NLS-1$
        val UUID = TABLE.column("remoteId")

        /** whether to send a reminder at deadline  */
        const val NOTIFY_AT_DEADLINE = 1 shl 1

        /** whether to send reminders while task is overdue  */
        const val NOTIFY_AFTER_DEADLINE = 1 shl 2

        /** reminder mode non-stop  */
        const val NOTIFY_MODE_NONSTOP = 1 shl 3

        /** reminder mode five times (exclusive with non-stop)  */
        const val NOTIFY_MODE_FIVE = 1 shl 4

        const val NOTIFY_AT_START = 1 shl 5

        /** urgency array index -> significance  */
        const val URGENCY_NONE = 0
        const val URGENCY_SPECIFIC_DAY = 7
        const val URGENCY_SPECIFIC_DAY_TIME = 8

        /** hide until array index -> significance  */
        const val HIDE_UNTIL_NONE = 0
        const val HIDE_UNTIL_DUE = 1
        const val HIDE_UNTIL_DAY_BEFORE = 2
        const val HIDE_UNTIL_WEEK_BEFORE = 3
        const val HIDE_UNTIL_SPECIFIC_DAY = 4

        // --- for astrid.com
        const val HIDE_UNTIL_SPECIFIC_DAY_TIME = 5
        const val HIDE_UNTIL_DUE_TIME = 6
        const val URGENCY_TODAY = 1
        const val URGENCY_TOMORROW = 2

        // --- notification flags
        const val URGENCY_DAY_AFTER = 3
        const val URGENCY_NEXT_WEEK = 4
        const val URGENCY_IN_TWO_WEEKS = 5

        private const val TRANS_SUPPRESS_REFRESH = "suppress-refresh"
        const val TRANS_REMINDERS = "reminders"
        const val TRANS_RANDOM = "random"

        private val INVALID_COUNT = ";?COUNT=(-1|0)".toRegex()

        /**
         * Creates due date for this task. If this due date has no time associated, we move it to the last
         * millisecond of the day.
         *
         * @param setting one of the URGENCY_* constants
         * @param customDate if specific day or day & time is set, this value
         */
        fun createDueDate(setting: Int, customDate: Long): Long {
            val date: Long = when (setting) {
                URGENCY_NONE -> 0
                URGENCY_TODAY -> DateUtilities.now()
                URGENCY_TOMORROW -> DateUtilities.now() + DateUtilities.ONE_DAY
                URGENCY_DAY_AFTER -> DateUtilities.now() + 2 * DateUtilities.ONE_DAY
                URGENCY_NEXT_WEEK -> DateUtilities.now() + DateUtilities.ONE_WEEK
                URGENCY_IN_TWO_WEEKS -> DateUtilities.now() + 2 * DateUtilities.ONE_WEEK
                URGENCY_SPECIFIC_DAY, URGENCY_SPECIFIC_DAY_TIME -> customDate
                else -> throw IllegalArgumentException("Unknown setting $setting")
            }
            if (date <= 0) {
                return date
            }
            var dueDate = DateTimeUtils.newDateTime(date).withMillisOfSecond(0)
            dueDate = if (setting != URGENCY_SPECIFIC_DAY_TIME) {
                dueDate
                        .withHourOfDay(12)
                        .withMinuteOfHour(0)
                        .withSecondOfMinute(0) // Seconds == 0 means no due time
            } else {
                dueDate.withSecondOfMinute(1) // Seconds > 0 means due time exists
            }
            return dueDate.millis
        }

        /** Checks whether provided due date has a due time or only a date  */
        @JvmStatic fun hasDueTime(dueDate: Long): Boolean {
            return dueDate > 0 && dueDate % 60000 > 0
        }

        fun isValidUuid(uuid: String): Boolean {
            return try {
                val value = uuid.toLong()
                value > 0
            } catch (e: NumberFormatException) {
                Timber.e(e)
                isUuidEmpty(uuid)
            }
        }

        fun String?.sanitizeRecur(): String? = this
                ?.replace("BYDAY=;", "")
                ?.replace(INVALID_COUNT, "") // ical4j adds COUNT=-1 if there is an UNTIL value

        fun isUuidEmpty(uuid: String?): Boolean {
            return NO_UUID == uuid || Strings.isNullOrEmpty(uuid)
        }
    }
}
