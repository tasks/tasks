package org.tasks.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.api.BooleanCriterion
import com.todoroo.astrid.api.CustomFilterCriterion
import com.todoroo.astrid.api.MultipleSelectCriterion
import com.todoroo.astrid.api.TextInputCriterion
import com.todoroo.astrid.core.CriterionInstance
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings
import org.tasks.compose.DeleteButton
import org.tasks.compose.FilterCondition.FilterCondition
import org.tasks.compose.FilterCondition.InputTextOption
import org.tasks.compose.FilterCondition.NewCriterionFAB
import org.tasks.compose.FilterCondition.SelectCriterionType
import org.tasks.compose.FilterCondition.SelectFromList
import org.tasks.data.NO_ORDER
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.entity.Filter
import org.tasks.data.entity.Task
import org.tasks.data.sql.UnaryCriterion
import org.tasks.extensions.Context.openUri
import org.tasks.filters.CustomFilter
import org.tasks.filters.FilterCriteriaProvider
import org.tasks.filters.mapToSerializedString
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme
import javax.inject.Inject

@AndroidEntryPoint
class FilterSettingsActivity : BaseListSettingsActivity() {
    @Inject lateinit var filterDao: FilterDao
    @Inject lateinit var filterCriteriaProvider: FilterCriteriaProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private val viewModel: FilterSettingsViewModel by viewModels()

    override val defaultIcon = TasksIcons.FILTER_LIST

    private val editCriterionType: MutableState<String?> = mutableStateOf(null)
    private val newCriterionTypes: MutableState<List<CustomFilterCriterion>?> = mutableStateOf(null)
    private val newCriterionOptions: MutableState<CriterionInstance?> = mutableStateOf(null)

    override val filter: CustomFilter?
        get() = viewModel.viewState.value.filter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            filter?.let {
                selectedColor = it.tint
                selectedIcon.value =  it.icon
                textState.value = it.title ?: ""
            }
        }
        if (savedInstanceState != null) {
            intent.getStringExtra(EXTRA_TITLE)?.let { textState.value = it }
        }
        updateTheme()

        setContent {
            TasksTheme { ActivityContent() }
        }
    }

    private fun onDelete(index: Int) {
        viewModel.removeAt(index)
    }

    private fun newCriterion() {
        viewModel.setFabExtended(false)
        lifecycleScope.launch {
            newCriterionTypes.value = filterCriteriaProvider.all()
        }
    }

    override val toolbarTitle: String
        get() = if (isNew) getString(R.string.FLA_new_filter) else viewModel.viewState.value.filter?.title ?: ""

    override suspend fun save() {
        val newName = newName
        if (Strings.isNullOrEmpty(newName)) {
            errorState.value = getString(R.string.name_cannot_be_empty)
            return
        }

        if (hasChanges()) {
            val criteria = viewModel.viewState.value.criteria
            var f = Filter(
                id = filter?.id ?: 0L,
                title = newName,
                color = selectedColor,
                icon = selectedIcon.value,
                values = criteria.values,
                criterion = CriterionInstance.serialize(criteria),
                sql = criteria.sql,
                order = filter?.order ?: NO_ORDER,
            )
            if (f.criterion.isNullOrBlank()) {
                throw RuntimeException("Criterion cannot be empty")
            }
            if (isNew) {
                f = f.copy(
                    id = filterDao.insert(f)
                )
            } else {
                filterDao.update(f)
            }
            localBroadcastManager.broadcastRefresh()
            setResult(
                    Activity.RESULT_OK,
                    Intent(TaskListFragment.ACTION_RELOAD)
                            .putExtra(MainActivity.OPEN_FILTER, CustomFilter(f)))
        }
        finish()
    }

    private val newName: String
        get() = textState.value.trim { it <= ' ' }

    override fun hasChanges(): Boolean {
        val criteria = viewModel.viewState.value.criteria
        return if (isNew) {
            (!Strings.isNullOrEmpty(newName)
                    || selectedColor != 0 || selectedIcon.value?.isBlank() == false || criteria.size > 1)
        } else newName != filter!!.title
                || selectedColor != filter!!.tint
                || selectedIcon.value != filter!!.icon
                || CriterionInstance.serialize(criteria) != filter!!.criterion!!.trim()
                || criteria.values != filter!!.valuesForNewTasks
                || criteria.sql != filter!!.sql
    }

    override suspend fun delete() {
        viewModel.delete {
            setResult(
                Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED).putExtra(TOKEN_FILTER, filter))
            finish()
        }
    }

    private fun help() = openUri(R.string.url_filters)

    @Composable
    private fun ActivityContent ()
    {
        TasksTheme {
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            BaseSettingsContent(
                optionButton = {
                    if (isNew) {
                        IconButton(onClick = { help() }) {
                            Icon(imageVector = Icons.Outlined.Help, contentDescription = "")
                        }
                    } else DeleteButton(filter?.title ?: ""){ delete() }
                },
                fab = {
                    NewCriterionFAB(viewState.fabExtended) { newCriterion() }
                }
            ) {
                FilterCondition(
                    items = viewState.criteria,
                    onDelete = { index -> onDelete(index) },
                    doSwap = { from, to -> viewModel.move(from, to) },
                    onClick = { id -> editCriterionType.value = id }
                )
            }

            /** edit given criterion type (AND|OR|NOT) **/
            editCriterionType.value?.let { itemId ->
                val index = viewState.criteria.indexOfFirst { it.id == itemId }
                assert(index >= 0)
                val criterionInstance = remember (index) {
                    CriterionInstance(viewState.criteria[index])
                }
                if (criterionInstance.type != CriterionInstance.TYPE_UNIVERSE) {
                    SelectCriterionType(
                        title = criterionInstance.titleFromCriterion,
                        selected = when (criterionInstance.type) {
                            CriterionInstance.TYPE_INTERSECT -> 0
                            CriterionInstance.TYPE_ADD -> 1
                            else -> 2
                        },
                        types = listOf(
                            stringResource(R.string.custom_filter_and),
                            stringResource(R.string.custom_filter_or),
                            stringResource(R.string.custom_filter_not)
                        ),
                        help = { help() },
                        onCancel = { editCriterionType.value = null }
                    ) { selected ->
                        val type = when (selected) {
                            0 -> CriterionInstance.TYPE_INTERSECT
                            1 -> CriterionInstance.TYPE_ADD
                            else -> CriterionInstance.TYPE_SUBTRACT
                        }
                        if (criterionInstance.type != type) {
                            criterionInstance.type = type
                            viewModel.setCriterion(index, criterionInstance)
                        }
                        editCriterionType.value = null
                    }
                }
            } /* end (AND|OR|NOT) dialog */

            /** dialog to select new criterion category **/
            newCriterionTypes.value?.let  { list ->
                SelectFromList(
                    names = list.map(CustomFilterCriterion::getName),
                    onCancel = { newCriterionTypes.value = null },
                    onSelected = { which ->
                        val instance = CriterionInstance()
                        instance.criterion = list[which]
                        newCriterionTypes.value = null
                        if (instance.criterion is BooleanCriterion) {
                            viewModel.addCriteria(instance)
                        } else
                            newCriterionOptions.value = instance
                    }
                )
            } /* end dialog  */

            /** Show options menu for the given CriterionInstance  */
            newCriterionOptions.value?.let { instance ->

                when (instance.criterion) {
                    is MultipleSelectCriterion -> {
                        val multiSelectCriterion = instance.criterion as MultipleSelectCriterion
                        val list = multiSelectCriterion.entryTitles.toList()
                        SelectFromList(
                            names = list,
                            title = instance.criterion.name,
                            onCancel = { newCriterionOptions.value = null },
                            onSelected = { which ->
                                instance.selectedIndex = which
                                viewModel.addCriteria(instance)
                                newCriterionOptions.value = null
                            }
                        )
                    }

                    is TextInputCriterion -> {
                        val textInCriterion = instance.criterion as TextInputCriterion
                        InputTextOption (
                            title = textInCriterion.name,
                            onCancel = { newCriterionOptions.value = null },
                            onDone = { text ->
                                text.trim().takeIf{ it != "" }?. let { text ->
                                    instance.selectedText = text
                                    viewModel.addCriteria(instance)
                                }
                                newCriterionOptions.value = null
                            }
                        )
                    }

                    else -> assert(false) { "Unexpected Criterion type" }
                }
            } /* end given criteria options dialog */
        }
    } /* activityContent */

    companion object {
        const val TOKEN_FILTER = "token_filter"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CRITERIA = "extra_criteria"

        val List<CriterionInstance>.sql: String
            get() {
                val sql = StringBuilder(" WHERE ")
                for (instance in this) {
                    val value = instance.valueFromCriterion
                    when (instance.type) {
                        CriterionInstance.TYPE_ADD -> sql.append(" OR ")
                        CriterionInstance.TYPE_SUBTRACT -> sql.append(" AND NOT ")
                        CriterionInstance.TYPE_INTERSECT -> sql.append(" AND ")
                    }

                    // special code for all tasks universe
                    if (instance.type == CriterionInstance.TYPE_UNIVERSE || instance.criterion.sql == null) {
                        sql.append(activeAndVisible())
                    } else {
                        val subSql = instance.criterion.sql
                            .replace("?", UnaryCriterion.sanitize(value!!))
                            .trim()
                        sql.append(Task.ID).append(" IN (").append(subSql).append(")")
                    }
                }
                return sql.toString()
            }

        private val List<CriterionInstance>.values: String
            get() {
                val values: MutableMap<String, Any> = HashMap()
                for (instance in this) {
                    val value = instance.valueFromCriterion
                    if (instance.criterion.valuesForNewTasks != null
                        && instance.type == CriterionInstance.TYPE_INTERSECT) {
                        for ((key, value1) in instance.criterion.valuesForNewTasks) {
                            values[key.replace("?", value!!)] = value1.toString().replace("?", value)
                        }
                    }
                }
                return mapToSerializedString(values)
            }
    }
}