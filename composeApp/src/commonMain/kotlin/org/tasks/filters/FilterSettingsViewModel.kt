package org.tasks.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.billing.PurchaseState
import org.tasks.compose.settings.PickerColor
import org.tasks.compose.settings.buildPickerColors
import org.tasks.data.NO_ORDER
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.entity.Filter
import org.tasks.data.entity.Task
import org.tasks.data.sql.Field
import org.tasks.data.sql.Query
import org.tasks.data.sql.UnaryCriterion
import org.tasks.db.QueryUtils
import org.tasks.themes.TasksIcons
import com.todoroo.astrid.api.PermaSql
import kotlin.math.max

/**
 * Desktop view-model for creating and editing a custom filter (port of the Android
 * `org.tasks.activities.FilterSettingsViewModel`, which was Hilt-bound).
 *
 * Holds the editable state — title, colour, icon, and the list of criterion rows — and
 * performs the criterion manipulation, the live per-row task-count preview, and the
 * save/delete against [FilterDao]. It is written fresh for the compose app (KMP/Compose
 * friendly); the Android Hilt view-model is left untouched.
 *
 * Entry paths, mirroring the Android activity:
 * - edit: [initialFilter] non-null — criteria reloaded from `filter.criterion`;
 * - create from a template: [templateIndex] non-null — criteria from [FilterTemplates];
 * - create custom / empty: neither — starts with just the locked universe row.
 */
@Suppress("TooManyFunctions", "LongParameterList")
class FilterSettingsViewModel(
    private val filterCriteriaProvider: FilterCriteriaProvider,
    private val filterDao: FilterDao,
    private val taskDao: TaskDao,
    private val purchaseState: PurchaseState,
    isDark: Boolean,
    private val initialFilter: Filter?,
    private val templateIndex: Int?,
) : ViewModel() {

    data class State(
        val title: String = "",
        val color: Int = 0,
        val icon: String = TasksIcons.FILTER_LIST,
        val criteria: List<CriterionInstance> = emptyList(),
        val error: String? = null,
        val pickerColors: List<PickerColor> = emptyList(),
        val hasPro: Boolean = false,
        val showColorPicker: Boolean = false,
        val showIconPicker: Boolean = false,
        val newCriterionTypes: List<CustomFilterCriterion> = emptyList(),
        val showTypePicker: Boolean = false,
        val newCriterionOptions: CriterionInstance? = null,
    )

    /** The original filter being edited, or null when creating a new one. */
    val filter: Filter? = initialFilter

    /** True when this editor is creating a new filter rather than editing an existing one. */
    val isNew: Boolean = initialFilter == null

    private val _state = MutableStateFlow(
        State(
            title = initialFilter?.title?.trim() ?: "",
            color = initialFilter?.color ?: 0,
            icon = initialFilter?.icon ?: TasksIcons.FILTER_LIST,
            pickerColors = buildPickerColors(isDark),
            hasPro = purchaseState.purchasedThemes(),
        )
    )
    val state: StateFlow<State> = _state

    private var universeRow: CriterionInstance? = null

    init {
        viewModelScope.launch {
            universeRow = CriterionInstance().apply {
                criterion = filterCriteriaProvider.startingUniverse()
                type = CriterionInstance.TYPE_UNIVERSE
            }
            val criteria = if (initialFilter != null) {
                filterCriteriaProvider.fromString(initialFilter.criterion)
            } else if (templateIndex != null) {
                FilterTemplates.withUniverse(templateIndex, filterCriteriaProvider)
            } else {
                emptyList()
            }
            setCriteria(criteria)
        }
    }

    fun setTitle(value: String) = _state.update { it.copy(title = value, error = null) }

    fun setError(error: String?) = _state.update { it.copy(error = error) }

    fun setColor(value: Int) = _state.update { it.copy(color = value) }

    fun setIcon(value: String) = _state.update { it.copy(icon = value) }

    fun openColorPicker() = _state.update { it.copy(showColorPicker = true) }

    fun closeColorPicker() = _state.update { it.copy(showColorPicker = false) }

    fun selectColor(color: PickerColor) =
        _state.update { it.copy(color = color.originalColor, showColorPicker = false) }

    fun openIconPicker() = _state.update { it.copy(showIconPicker = true) }

    fun closeIconPicker() = _state.update { it.copy(showIconPicker = false) }

    fun selectIcon(name: String) = _state.update { it.copy(icon = name, showIconPicker = false) }

    /** Loads the full criterion catalog and shows the "pick a criterion type" list. */
    fun openAddCriterion() = viewModelScope.launch {
        val all = filterCriteriaProvider.all()
        _state.update { it.copy(newCriterionTypes = all, showTypePicker = true) }
    }

    fun cancelTypePicker() =
        _state.update { it.copy(newCriterionTypes = emptyList(), showTypePicker = false) }

    /**
     * A criterion type was chosen. Boolean criteria are added immediately (no value to
     * pick); multi-select and text-input criteria open their value picker.
     */
    fun onTypePicked(which: Int) {
        val list = _state.value.newCriterionTypes
        if (which !in list.indices) return
        val instance = CriterionInstance().apply { criterion = list[which] }
        _state.update { it.copy(newCriterionTypes = emptyList(), showTypePicker = false) }
        if (instance.criterion is BooleanCriterion) {
            addCriteria(instance)
        } else {
            _state.update { it.copy(newCriterionOptions = instance) }
        }
    }

    /** A value was chosen for a pending multi-select criterion. */
    fun onValuePicked(which: Int) {
        val instance = _state.value.newCriterionOptions ?: return
        instance.selectedIndex = which
        _state.update { it.copy(newCriterionOptions = null) }
        addCriteria(instance)
    }

    /** A value was entered for a pending text-input criterion (empty input cancels). */
    fun onTextDone(text: String) {
        val instance = _state.value.newCriterionOptions
        _state.update { it.copy(newCriterionOptions = null) }
        val value = text.trim().takeIf { it.isNotEmpty() } ?: return
        instance?.selectedText = value
        if (instance != null) addCriteria(instance)
    }

    fun cancelValuePicker() = _state.update { it.copy(newCriterionOptions = null) }

    private fun setCriteria(criteria: List<CriterionInstance>) {
        val row = universeRow
        _state.update {
            it.copy(criteria = criteria.ifEmpty { row?.let { r -> listOf(r) } ?: emptyList() })
        }
        viewModelScope.launch { updateCounts() }
    }

    fun removeAt(index: Int) {
        if (index <= 0) return // the universe row is locked
        setCriteria(_state.value.criteria.toMutableList().apply { removeAt(index) })
    }

    fun move(from: Int, to: Int) {
        if (from == to || from <= 0 || to <= 0) return
        setCriteria(
            _state.value.criteria.toMutableList().apply {
                val criterion = removeAt(from)
                add(to, criterion)
            }
        )
    }

    fun addCriteria(instance: CriterionInstance) {
        setCriteria(_state.value.criteria.toMutableList().apply { add(instance) })
    }

    fun setCriterion(index: Int, criterionInstance: CriterionInstance) {
        if (index <= 0) return
        setCriteria(
            _state.value.criteria.toMutableList().apply {
                set(index, criterionInstance)
            }
        )
    }

    private suspend fun updateCounts() {
        val criteria = _state.value.criteria
        val newList = mutableListOf<CriterionInstance>()
        var maxCount = 0
        var last = -1
        val sql = StringBuilder(Query.select(Field.COUNT).from(Task.TABLE).toString())
            .append(" WHERE ")

        for (instance in criteria.map { CriterionInstance(it) }) {
            when (instance.type) {
                CriterionInstance.TYPE_ADD -> sql.append(" OR ")
                CriterionInstance.TYPE_SUBTRACT -> sql.append(" AND NOT ")
                CriterionInstance.TYPE_INTERSECT -> sql.append(" AND ")
                else -> Unit
            }
            val cSql = instance.criterion.sql
            if (instance.type == CriterionInstance.TYPE_UNIVERSE || cSql == null) {
                sql.append(activeAndVisible()).append(' ')
            } else {
                var subSql = cSql
                    .replace("?", UnaryCriterion.sanitize(instance.valueFromCriterion!!))
                subSql = PermaSql.replacePlaceholdersForQuery(subSql)
                sql.append(Task.ID).append(" IN (").append(subSql).append(")")
            }
            val count = taskDao.count(QueryUtils.showHiddenAndCompleted(sql.toString()))
            instance.start = if (last == -1) count else last
            instance.end = count
            last = instance.end
            maxCount = max(maxCount, last)
            newList.add(instance)
        }
        for (instance in newList) {
            instance.max = maxCount
        }
        _state.update { state -> state.copy(criteria = newList) }
    }

    fun hasChanges(): Boolean {
        val viewState = _state.value
        val criteria = viewState.criteria
        return if (isNew) {
            viewState.title.isNotBlank() ||
                viewState.color != 0 ||
                viewState.icon.isNotBlank() ||
                criteria.size > 1
        } else {
            val original = filter!!
            viewState.title.trim() != original.title ||
                viewState.color != (original.color ?: 0) ||
                viewState.icon != original.icon ||
                CriterionInstance.serialize(criteria) != original.criterion?.trim() ||
                criteria.values != original.values ||
                criteria.sql != original.sql
        }
    }

    /**
     * Persists the filter. Returns the saved [Filter] (with its id) and throws
     * [IllegalArgumentException] when the title is blank, mirroring the Android
     * activity's save behaviour. The caller is responsible for refreshing the drawer
     * and (for new filters) selecting the result.
     */
    suspend fun save(): Filter {
        val title = _state.value.title.trim { it <= ' ' }
        if (title.isEmpty()) {
            throw IllegalArgumentException("name_cannot_be_empty")
        }
        val criteria = _state.value.criteria
        var f = Filter(
            id = initialFilter?.id ?: 0L,
            title = title,
            color = _state.value.color,
            icon = _state.value.icon,
            values = criteria.values,
            criterion = CriterionInstance.serialize(criteria),
            sql = criteria.sql,
            order = initialFilter?.order ?: NO_ORDER,
        )
        if (f.criterion.isNullOrBlank()) {
            throw IllegalStateException("Criterion cannot be empty")
        }
        return if (isNew) {
            f.copy(id = filterDao.insert(f))
        } else {
            filterDao.update(f)
            f
        }
    }

    suspend fun delete(onCompleted: () -> Unit) {
        initialFilter?.let { filterDao.delete(it.id) }
        onCompleted()
    }
}
