package org.tasks.filters

import org.tasks.data.UUIDHelper

/**
 * Desktop port of the Android app's custom-filter criterion model
 * (`com.todoroo.astrid.api.CustomFilterCriterion` and friends, `app` module).
 *
 * The fields and semantics are a field-for-field copy; only the Android `Parcelable`
 * machinery is dropped. Filters are stored locally per device (no backend syncs the
 * `filters` table), so this copy must stay semantically identical to the Android
 * original: a filter created here and an identically-built one on Android must produce
 * the same SQL. The serialization format (`CriterionInstance.Companion.serialize`) is
 * deliberately byte-compatible with the Android one.
 */
abstract class CustomFilterCriterion {

    /**
     * Values to apply to a task when quick-adding a task from a filter created from this
     * criterion. `?` will be replaced with the entry value. If empty, no additional
     * values will be stored for a task.
     */
    val valuesForNewTasks: MutableMap<String, Any> = mutableMapOf()

    /**
     * Criteria identifier. This identifier allows saved filters to be reloaded.
     * e.g. "duedate"
     */
    var identifier: String? = null

    /**
     * Criteria title. If the title contains `?`, it is replaced by the entry label
     * selected. e.g. "Due: ?"
     */
    var text: String? = null

    /**
     * Criterion SQL. This query should return task id's. If it contains `?`, the `?`
     * will be replaced by the entry value, e.g.
     * `SELECT _id FROM tasks WHERE dueDate <= ?`
     */
    var sql: String? = null

    /** Criteria name. Displayed when users are selecting a criterion. */
    var name: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomFilterCriterion) return false
        return valuesForNewTasks == other.valuesForNewTasks
            && identifier == other.identifier
            && text == other.text
            && sql == other.sql
            && name == other.name
    }

    override fun hashCode(): Int {
        var result = valuesForNewTasks.hashCode()
        result = 31 * result + (identifier?.hashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (sql?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }
}

/**
 * A criterion whose value is chosen from a fixed list of entries
 * (port of `com.todoroo.astrid.api.MultipleSelectCriterion`).
 */
@Suppress("LongParameterList")
class MultipleSelectCriterion(
    identifier: String,
    title: String,
    sql: String?,
    valuesForNewTasks: Map<String, Any>?,
    val entryTitles: Array<out String?>?,
    val entryValues: Array<out String?>?,
    name: String?,
) : CustomFilterCriterion() {

    init {
        this.identifier = identifier
        this.text = title
        this.sql = sql
        this.name = name
        valuesForNewTasks?.let { this.valuesForNewTasks.putAll(it) }
    }

    override fun toString(): String =
        "MultipleSelectCriterion{entryTitles=${entryTitles?.toList()}, " +
            "entryValues=${entryValues?.toList()}, valuesForNewTasks=$valuesForNewTasks, " +
            "identifier=$identifier, text=$text, sql=$sql, name=$name}"
}

/**
 * A criterion whose value is free-form text (port of
 * `com.todoroo.astrid.api.TextInputCriterion`).
 */
class TextInputCriterion(
    identifier: String,
    title: String,
    sql: String,
    val prompt: String,
    val hint: String,
    name: String,
) : CustomFilterCriterion() {

    init {
        this.identifier = identifier
        this.text = title
        this.sql = sql
        this.name = name
    }

    override fun toString(): String =
        "TextInputCriterion{hint=$hint, prompt=$prompt, valuesForNewTasks=$valuesForNewTasks, " +
            "identifier=$identifier, text=$text, sql=$sql, name=$name}"
}

/**
 * A boolean criterion with no selectable value (port of
 * `com.todoroo.astrid.api.BooleanCriterion`).
 */
class BooleanCriterion(
    identifier: String,
    title: String,
    sql: String,
) : CustomFilterCriterion() {

    init {
        this.identifier = identifier
        this.text = title
        this.sql = sql
        this.name = title
    }
}

/**
 * One row of a custom filter: a criterion plus the user's selection and the set
 * operation (AND/OR/NOT/universe) that combines it with the other rows
 * (port of `com.todoroo.astrid.core.CriterionInstance`).
 */
class CriterionInstance {
    lateinit var criterion: CustomFilterCriterion
    var selectedIndex = -1
    var selectedText: String? = null
    var type = TYPE_INTERSECT
    var end = 0
    var start = 0
    var max = 0
    var id: String = UUIDHelper.newUUID()
        private set

    constructor()

    constructor(other: CriterionInstance) {
        id = other.id
        criterion = other.criterion
        selectedIndex = other.selectedIndex
        selectedText = other.selectedText
        type = other.type
        end = other.end
        start = other.start
        max = other.max
    }

    val titleFromCriterion: String
        get() {
            val c = criterion
            when (c) {
                is MultipleSelectCriterion -> {
                    val titles = c.entryTitles
                    if (selectedIndex >= 0 && titles != null && selectedIndex < titles.size) {
                        return c.text!!.replace("?", titles[selectedIndex] ?: "")
                    }
                    return c.text!!
                }

                is TextInputCriterion -> {
                    return if (selectedText == null) {
                        c.text!!
                    } else c.text!!.replace("?", selectedText!!)
                }

                is BooleanCriterion -> {
                    return c.name!!
                }

                else -> throw UnsupportedOperationException("Unknown criterion type")
            }
        }

    val valueFromCriterion: String?
        get() {
            if (type == TYPE_UNIVERSE) {
                return null
            }
            val c = criterion
            when (c) {
                is MultipleSelectCriterion -> {
                    val values = c.entryValues
                    return if (selectedIndex >= 0 && values != null && selectedIndex < values.size) {
                        values[selectedIndex]
                    } else c.text
                }

                is TextInputCriterion -> {
                    return selectedText
                }

                is BooleanCriterion -> {
                    return c.name
                }

                else -> throw UnsupportedOperationException("Unknown criterion type")
            }
        }

    private fun serialize(): String {
        // criterion|entry|text|type|sql
        return listOf(
            escape(criterion.identifier),
            escape(valueFromCriterion),
            escape(criterion.text),
            type.toString(),
            criterion.sql ?: "",
        ).joinToString(SERIALIZATION_SEPARATOR)
    }

    override fun toString(): String =
        "CriterionInstance(criterion=$criterion, selectedIndex=$selectedIndex, " +
            "selectedText=$selectedText, type=$type, end=$end, start=$start, max=$max, id='$id')"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CriterionInstance) return false
        if (criterion != other.criterion) return false
        if (selectedIndex != other.selectedIndex) return false
        if (selectedText != other.selectedText) return false
        if (type != other.type) return false
        if (end != other.end) return false
        if (start != other.start) return false
        if (max != other.max) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        var result = criterion.hashCode()
        result = 31 * result + selectedIndex
        result = 31 * result + (selectedText?.hashCode() ?: 0)
        result = 31 * result + type
        result = 31 * result + end
        result = 31 * result + start
        result = 31 * result + max
        result = 31 * result + id.hashCode()
        return result
    }

    companion object {
        const val TYPE_ADD = 0
        const val TYPE_SUBTRACT = 1
        const val TYPE_INTERSECT = 2
        const val TYPE_UNIVERSE = 3

        private fun escape(item: String?): String =
            item?.replace(SERIALIZATION_SEPARATOR, SEPARATOR_ESCAPE) ?: ""

        /**
         * Newline-separated rows, byte-compatible with the Android app's format.
         */
        fun serialize(criterion: List<CriterionInstance>): String =
            criterion.joinToString("\n") { it.serialize() }.trim()
    }
}
