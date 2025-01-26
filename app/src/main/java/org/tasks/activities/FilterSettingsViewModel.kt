package org.tasks.activities

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.core.CriterionInstance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.activities.FilterSettingsActivity.Companion.EXTRA_CRITERIA
import org.tasks.activities.FilterSettingsActivity.Companion.TOKEN_FILTER
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.entity.Task
import org.tasks.data.sql.Field
import org.tasks.data.sql.Query
import org.tasks.data.sql.UnaryCriterion
import org.tasks.db.QueryUtils
import org.tasks.filters.CustomFilter
import org.tasks.filters.FilterCriteriaProvider
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class FilterSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val filterCriteriaProvider: FilterCriteriaProvider,
    private val filterDao: FilterDao,
    private val taskDao: TaskDao,
) : ViewModel() {
    data class ViewState(
        val filter: CustomFilter? = null,
        val fabExtended: Boolean = false,
        val criteria: ImmutableList<CriterionInstance> = persistentListOf(),
    )

    private val _viewState = MutableStateFlow(
        ViewState(
            filter = savedStateHandle.get<CustomFilter>(TOKEN_FILTER),
        )
    )
    val viewState: StateFlow<ViewState> = _viewState

    init {
        _viewState.value.filter
            ?.let { filter ->
                viewModelScope.launch {
                    setCriteria(filterCriteriaProvider.fromString(filter.criterion))
                }
            }
            ?: savedStateHandle.get<String>(EXTRA_CRITERIA)?.let { criteria ->
                viewModelScope.launch {
                    setCriteria(filterCriteriaProvider.fromString(criteria))
                }
            }
            ?: setCriteria(emptyList())
    }

    fun setFabExtended(extended: Boolean) {
        _viewState.update { it.copy(fabExtended = extended) }
    }

    private fun setCriteria(criteria: List<CriterionInstance>) {
        _viewState.update {
            it.copy(
                criteria = criteria.ifEmpty { universe() }.toImmutableList(),
                fabExtended = it.filter == null || criteria.size <= 1,
            )
        }
        viewModelScope.launch {
            _viewState.update {
                it.copy(criteria = updateCounts(it.criteria).toImmutableList())
            }
        }
    }

    fun removeAt(index: Int) {
        setCriteria(_viewState.value.criteria.toMutableList().apply { removeAt(index) })
    }

    fun move(from: Int, to: Int) {
        setCriteria(
            _viewState.value.criteria
                .toMutableList()
                .apply {
                    val criterion = removeAt(from)
                    add(to, criterion)
                }
        )
    }

    fun addCriteria(instance: CriterionInstance) {
        setCriteria(_viewState.value.criteria.toMutableList().apply { add(instance) })
    }

    private suspend fun updateCounts(criteria: List<CriterionInstance>): List<CriterionInstance> {
        val newList = mutableListOf<CriterionInstance>()
        var max = 0
        var last = -1
        val sql = StringBuilder(Query.select(Field.COUNT).from(Task.TABLE).toString())
            .append(" WHERE ")

        for (instance in criteria.map { CriterionInstance(it) }) {
            when (instance.type) {
                CriterionInstance.TYPE_ADD -> sql.append("OR ")
                CriterionInstance.TYPE_SUBTRACT -> sql.append("AND NOT ")
                CriterionInstance.TYPE_INTERSECT -> sql.append("AND ")
            }

            // special code for all tasks universe
            if (instance.type == CriterionInstance.TYPE_UNIVERSE || instance.criterion.sql == null) {
                sql.append(activeAndVisible()).append(' ')
            } else {
                var subSql: String = instance.criterion.sql.replace(
                    "?",
                    UnaryCriterion.sanitize(instance.valueFromCriterion!!)
                )
                subSql = PermaSql.replacePlaceholdersForQuery(subSql)
                sql.append(Task.ID).append(" IN (").append(subSql).append(")")
            }
            val sqlString = QueryUtils.showHiddenAndCompleted(sql.toString())
            val count = taskDao.count(sqlString)
            instance.start = if (last == -1) count else last
            instance.end = count
            last = instance.end
            max = max(max, last)
            newList.add(instance)
        }
        for (instance in newList) {
            instance.max = max
        }
        return newList
    }

    private fun universe() = listOf(
        CriterionInstance().apply {
            criterion = filterCriteriaProvider.startingUniverse
            type = CriterionInstance.TYPE_UNIVERSE
        }
    )

    fun setCriterion(index: Int, criterionInstance: CriterionInstance) {
        setCriteria(
            _viewState.value.criteria.toMutableList().apply {
                set(index, criterionInstance)
            }
        )
    }

    fun delete(onCompleted: () -> Unit) = viewModelScope.launch {
        _viewState.value.filter?.id?.let { filterDao.delete(it) }
        onCompleted()
    }
}
