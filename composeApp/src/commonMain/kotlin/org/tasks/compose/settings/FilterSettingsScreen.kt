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

    val onSaveClick: () -> Unit = {
        scope.launch {
            val updatedFilter = filterId?.toLongOrNull()?.let { id -> filterDao.getById(id) }
            if (filterId == null) {
                filterDao.insert(
                    Filter(
                        title = name,
                        sql = sql,
                        color = color,
                        icon = icon,
                    )
                )
            } else if (updatedFilter != null) {
                filterDao.update(
                    Filter(
                        id = updatedFilter.id,
                        title = name,
                        sql = sql,
                        values = updatedFilter.values,
                        criterion = updatedFilter.criterion,
                        color = color,
                        icon = icon,
                        order = updatedFilter.order,
                    )
                )
            }
            if (filterId == null || updatedFilter != null) {
                onSave(name, sql)
                saveCompleted = true
            }
        }
    }

    FilterDiscardDialog(
        show = showDiscardDialog,
        onDismiss = { showDiscardDialog = false },
        onDiscard = { showDiscardDialog = false; onBack() },
    )
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
    Scaffold(topBar = {
        FilterSettingsTopBar(
            filterId = filterId,
            hasChanges = hasChanges,
            onBack = onBack,
            onShowDiscard = { showDiscardDialog = true },
            onSaveClick = onSaveClick,
        )
    }) { innerPadding ->
        FilterSettingsContent(
            name = name,
            onNameChange = { name = it },
            sql = sql,
            onSqlChange = { sql = it },
            color = color,
            onColorClick = { showColorPicker = true },
            icon = icon,
            onIconClick = { showIconPicker = true },
            filterId = filterId,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDiscardDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
) {
    if (!show) return
    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
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
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = onDiscard) { Text("Discard") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSettingsTopBar(
    filterId: String?,
    hasChanges: Boolean,
    onBack: () -> Unit,
    onShowDiscard: () -> Unit,
    onSaveClick: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = {
                if (filterId != null && hasChanges) {
                    onShowDiscard()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSettingsContent(
    name: String,
    onNameChange: (String) -> Unit,
    sql: String,
    onSqlChange: (String) -> Unit,
    color: Int,
    onColorClick: () -> Unit,
    icon: String,
    onIconClick: () -> Unit,
    filterId: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            onValueChange = onNameChange,
            textStyle = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onColorClick() }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                color = if (color == 0) MaterialTheme.colorScheme.primary else Color(color),
                shape = CircleShape,
            ) {}
            Spacer(modifier = Modifier.width(16.dp))
            Text("Color", style = MaterialTheme.typography.bodyLarge)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onIconClick() }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TasksIcon(label = icon, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Icon", style = MaterialTheme.typography.bodyLarge)
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
            onValueChange = onSqlChange,
            textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 8.dp)
        )
        if (filterId != null) {
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { /* TODO: Delete filter */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete Filter") }
        }
    }
}
