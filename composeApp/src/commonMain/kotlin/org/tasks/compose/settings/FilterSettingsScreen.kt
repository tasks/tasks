package org.tasks.compose.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.tasks.compose.settings.PickerColor
import org.tasks.data.dao.FilterDao
import org.tasks.data.entity.Filter
import org.tasks.kmp.org.tasks.themes.ColorProvider
import org.tasks.themes.TasksIcons

class FilterSettingsState {
    var name: String = ""
    var sql: String = ""
    var color: Int = 0
    var icon: String = TasksIcons.FILTER_LIST
    var showDiscardDialog: Boolean = false
    var showColorPicker: Boolean = false
    var showIconPicker: Boolean = false
    var saveCompleted: Boolean = false
    var originalName: String = ""
    var originalSql: String = ""
    var originalColor: Int = 0
    var originalIcon: String = TasksIcons.FILTER_LIST

    val hasChanges: Boolean
        get() = name != originalName || sql != originalSql || color != originalColor || icon != originalIcon
}

@Composable
private fun rememberFilterSettingsState(
    filterId: String?,
    filterDao: FilterDao,
): FilterSettingsState {
    val state = remember { FilterSettingsState() }

    // Load existing filter
    LaunchedEffect(filterId) {
        filterId?.toLongOrNull()?.let { id ->
            val filter = filterDao.getById(id)
            state.name = filter?.title ?: ""
            state.sql = filter?.sql ?: ""
            state.color = filter?.color ?: 0
            state.icon = filter?.icon ?: TasksIcons.FILTER_LIST
            state.originalName = state.name
            state.originalSql = state.sql
            state.originalColor = state.color
            state.originalIcon = state.icon
        }
    }

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            state.saveCompleted = false
        }
    }

    return state
}

@Composable
private fun rememberPickerColors(): List<PickerColor> {
    return remember {
        ColorProvider.PRESET_COLORS.map { colorValue ->
            PickerColor(
                originalColor = colorValue,
                primaryColor = colorValue,
                colorOnPrimary = if (colorValue == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
                isFree = true,
            )
        }
    }
}

@Composable
private fun renderFilterDialogs(
    state: FilterSettingsState,
    onBack: () -> Unit,
    pickerColors: List<PickerColor>,
) {
    FilterDialogsContent(
        params = FilterDialogParams(
            showDiscardDialog = state.showDiscardDialog,
            onDismissDiscard = { state.showDiscardDialog = false },
            onDiscard = {
                state.showDiscardDialog = false
                onBack()
            },
            showColorPicker = state.showColorPicker,
            onDismissColorPicker = { state.showColorPicker = false },
            onColorSelected = { pickerColor ->
                state.color = pickerColor.originalColor
                state.showColorPicker = false
            },
            showIconPicker = state.showIconPicker,
            onDismissIconPicker = { state.showIconPicker = false },
            onIconSelected = { selectedIcon ->
                state.icon = selectedIcon ?: ""
                state.showIconPicker = false
            },
            pickerColors = pickerColors,
        )
    )
}

@Composable
private fun renderFilterScaffold(
    filterId: String?,
    state: FilterSettingsState,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            FilterSettingsTopAppBar(
                filterId = filterId,
                hasChanges = state.hasChanges,
                onBack = {
                    if (filterId != null && state.hasChanges) {
                        state.showDiscardDialog = true
                    } else {
                        onBack()
                    }
                },
                onSaveClick = onSaveClick,
            )
        }
    ) { innerPadding ->
        FilterSettingsContent(
            params = FilterContentParams(
                innerPadding = innerPadding,
                name = state.name,
                onNameChange = { state.name = it },
                sql = state.sql,
                onSqlChange = { state.sql = it },
                color = state.color,
                onColorClick = { state.showColorPicker = true },
                icon = state.icon,
                onIconClick = { state.showIconPicker = true },
                filterId = filterId,
                onDeleteClick = { /* TODO: Delete filter */ },
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSettingsScreen(
    filterId: String?,
    onBack: () -> Unit,
    onSave: (String, String?) -> Unit,
) {
    val filterDao = koinInject<FilterDao>()
    val scope = rememberCoroutineScope()
    val state = rememberFilterSettingsState(filterId, filterDao)
    val pickerColors = rememberPickerColors()

    val onSaveClick = createOnSaveClick(
        scope = scope,
        params = FilterSaveParams(
            filterDao = filterDao,
            filterId = filterId,
            name = state.name,
            sql = state.sql,
            color = state.color,
            icon = state.icon,
            onSave = onSave,
            onSaveCompleted = { state.saveCompleted = true },
        ),
    )

    renderFilterDialogs(state, onBack, pickerColors)

    renderFilterScaffold(filterId, state, onBack, onSaveClick)

}

private fun createOnSaveClick(
    scope: CoroutineScope,
    params: FilterSaveParams,
): () -> Unit = {
    {
        scope.launch {
            val updatedFilter = params.filterId?.toLongOrNull()?.let { id -> params.filterDao.getById(id) }
            if (params.filterId == null) {
                params.filterDao.insert(
                    Filter(
                        title = params.name,
                        sql = params.sql,
                        color = params.color,
                        icon = params.icon,
                    )
                )
            } else if (updatedFilter != null) {
                params.filterDao.update(
                    Filter(
                        id = updatedFilter.id,
                        title = params.name,
                        sql = params.sql,
                        values = updatedFilter.values,
                        criterion = updatedFilter.criterion,
                        color = params.color,
                        icon = params.icon,
                        order = updatedFilter.order,
                    )
                )
            }
            if (params.filterId == null || updatedFilter != null) {
                params.onSave(params.name, params.sql)
                params.onSaveCompleted()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
data class FilterDialogParams(
    val showDiscardDialog: Boolean,
    val onDismissDiscard: () -> Unit,
    val onDiscard: () -> Unit,
    val showColorPicker: Boolean,
    val onDismissColorPicker: () -> Unit,
    val onColorSelected: (PickerColor) -> Unit,
    val showIconPicker: Boolean,
    val onDismissIconPicker: () -> Unit,
    val onIconSelected: (String?) -> Unit,
    val pickerColors: List<PickerColor>,
)

data class FilterContentParams(
    val innerPadding: PaddingValues,
    val name: String,
    val onNameChange: (String) -> Unit,
    val sql: String,
    val onSqlChange: (String) -> Unit,
    val color: Int,
    val onColorClick: () -> Unit,
    val icon: String,
    val onIconClick: () -> Unit,
    val filterId: String?,
    val onDeleteClick: () -> Unit,
)

data class FilterSaveParams(
    val filterDao: FilterDao,
    val filterId: String?,
    val name: String,
    val sql: String,
    val color: Int,
    val icon: String,
    val onSave: (String, String?) -> Unit,
    val onSaveCompleted: () -> Unit,
)
