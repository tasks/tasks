package org.tasks.compose.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.tasks.compose.pickers.Icon
import org.tasks.compose.pickers.IconPickerDialog
import org.tasks.compose.settings.ColorPickerDialog
import org.tasks.compose.settings.PickerColor
import org.tasks.data.dao.FilterDao
import org.tasks.data.entity.Filter
import org.tasks.kmp.org.tasks.themes.ColorProvider
import org.tasks.themes.TasksIcons
import org.tasks.compose.components.TasksIcon
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSettingsScreen(
    filterId: String?,
    onBack: () -> Unit,
    onSave: (String, String?) -> Unit,
) {
    val filterDao = koinInject<FilterDao>()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var sql by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(0) }
    var icon by remember { mutableStateOf(TasksIcons.FILTER_LIST) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var saveCompleted by remember { mutableStateOf(false) }
    var originalName by remember { mutableStateOf("") }
    var originalSql by remember { mutableStateOf("") }
    var originalColor by remember { mutableStateOf(0) }
    var originalIcon by remember { mutableStateOf(TasksIcons.FILTER_LIST) }

    // Convert ThemeColor to PickerColor
    val pickerColors = remember {
        ColorProvider.PRESET_COLORS.map { colorValue ->
            PickerColor(
                originalColor = colorValue,
                primaryColor = colorValue,
                colorOnPrimary = if (colorValue == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
                isFree = true,
            )
        }
    }

    // Load existing filter
    LaunchedEffect(filterId) {
        filterId?.toLongOrNull()?.let { id ->
            val filter = filterDao.getById(id)
            name = filter?.title ?: ""
            sql = filter?.sql ?: ""
            color = filter?.color ?: 0
            icon = filter?.icon ?: TasksIcons.FILTER_LIST
            originalName = name
            originalSql = sql
            originalColor = color
            originalIcon = icon
        }
    }

    val hasChanges = name != originalName ||
            sql != originalSql ||
            color != originalColor ||
            icon != originalIcon

    LaunchedEffect(saveCompleted) {
        if (saveCompleted) {
            saveCompleted = false
            onBack()
        }
    }

    val onSaveClick = createOnSaveClick(
        scope = scope,
        params = FilterSaveParams(
            filterDao = filterDao,
            filterId = filterId,
            name = name,
            sql = sql,
            color = color,
            icon = icon,
            onSave = onSave,
            onSaveCompleted = { saveCompleted = true },
        ),
    )

    if (showDiscardDialog) {
        BasicAlertDialog(
            onDismissRequest = { showDiscardDialog = false },
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Discard changes?",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "You have unsaved changes. Are you sure you want to discard them?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showDiscardDialog = false }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            showDiscardDialog = false
                            onBack()
                        }) {
                            Text("Discard")
                        }
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            hasPro = true,
            colors = pickerColors,
            onDismiss = { showColorPicker = false },
            onColorSelected = { pickerColor ->
                color = pickerColor.originalColor
                showColorPicker = false
            },
        )
    }

    if (showIconPicker) {
        IconPickerDialog(
            selectedIcon = null,
            onIconSelected = { selectedIcon ->
                icon = selectedIcon ?: ""
                showIconPicker = false
            },
            onDismissRequest = { showIconPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (filterId != null && hasChanges) {
                            showDiscardDialog = true
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                title = { Text(if (filterId == null) "New Filter" else stringResource(Res.string.settings)) },
                actions = {
                    TextButton(onClick = onSaveClick) { Text("Save") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Filter Name",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

            // Color Picker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showColorPicker = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    color = if (color == 0) MaterialTheme.colorScheme.primary else Color(color),
                    shape = CircleShape,
                ) {}
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Icon Picker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showIconPicker = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TasksIcon(
                    label = icon,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "SQL Query",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BasicTextField(
                value = sql,
                onValueChange = { sql = it },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp)
            )

            if (filterId != null) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { /* TODO: Delete filter */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Filter")
                }
            }
        }
    }
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

@Composable
private fun FilterDiscardDialog(show: Boolean, onDismiss: () -> Unit, onDiscard: () -> Unit) {
    @OptIn(ExperimentalMaterial3Api::class)
    if (show) { BasicAlertDialog(onDismissRequest = onDismiss) { Text("Discard?"); Button(onClick = onDiscard) { Text("Discard") } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSettingsDialogs(
    showDiscardDialog: Boolean,
    onDismissDiscard: () -> Unit,
    onDiscard: () -> Unit,
    showColorPicker: Boolean,
    onDismissColorPicker: () -> Unit,
    onColorSelected: (PickerColor) -> Unit,
    showIconPicker: Boolean,
    onDismissIconPicker: () -> Unit,
    onIconSelected: (String?) -> Unit,
    pickerColors: List<PickerColor>,
) {
    FilterDiscardDialog(
        show = showDiscardDialog,
        onDismiss = onDismissDiscard,
        onDiscard = onDiscard,
    )
    if (showColorPicker) {
        ColorPickerDialog(
            hasPro = true,
            colors = pickerColors,
            onDismiss = onDismissColorPicker,
            onColorSelected = onColorSelected,
        )
    }
    if (showIconPicker) {
        IconPickerDialog(
            selectedIcon = null,
            onIconSelected = onIconSelected,
            onDismissRequest = onDismissIconPicker,
        )
    }
}

data class FilterSettingsContentState(
    val name: String,
    val onNameChange: (String) -> Unit,
    val sql: String,
    val onSqlChange: (String) -> Unit,
    val color: Int,
    val onColorClick: () -> Unit,
    val icon: String,
    val onIconClick: () -> Unit,
    val filterId: String?,
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
