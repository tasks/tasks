package org.tasks.compose.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.settings
import org.tasks.compose.pickers.Icon
import org.tasks.compose.pickers.IconPickerDialog
import org.tasks.compose.settings.ColorPickerDialog
import org.tasks.compose.settings.PickerColor
import org.tasks.kmp.org.tasks.themes.ColorProvider
import org.tasks.themes.TasksIcons
import org.tasks.compose.components.TasksIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterDialogsContent(
    params: FilterDialogParams,
) {
    if (params.showDiscardDialog) {
        BasicAlertDialog(
            onDismissRequest = params.onDismissDiscard,
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
                        TextButton(onClick = params.onDismissDiscard) {
                            Text("Cancel")
                        }
                        TextButton(onClick = params.onDiscard) {
                            Text("Discard")
                        }
                    }
                }
            }
        }
    }

    if (params.showColorPicker) {
        ColorPickerDialog(
            hasPro = true,
            colors = params.pickerColors,
            onDismiss = params.onDismissColorPicker,
            onColorSelected = params.onColorSelected,
        )
    }

    if (params.showIconPicker) {
        IconPickerDialog(
            selectedIcon = null,
            onIconSelected = params.onIconSelected,
            onDismissRequest = params.onDismissIconPicker,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSettingsTopAppBar(
    filterId: String?,
    hasChanges: Boolean,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = {
                if (filterId != null && hasChanges) {
                    onBack()
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

@Composable
private fun FilterNameField(
    name: String,
    onNameChange: (String) -> Unit,
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
        textStyle = TextStyle(
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun ColorPickerRow(
    color: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
}

@Composable
private fun IconPickerRow(
    icon: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
}

@Composable
private fun SqlQueryField(
    sql: String,
    onSqlChange: (String) -> Unit,
) {
    Text(
        text = "SQL Query",
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    BasicTextField(
        value = sql,
        onValueChange = onSqlChange,
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun DeleteFilterButton(
    onDeleteClick: () -> Unit,
) {
    Spacer(modifier = Modifier.height(0.dp))
    Button(
        onClick = onDeleteClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Delete Filter")
    }
}

@Composable
internal fun FilterSettingsContent(
    params: FilterContentParams,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(params.innerPadding)
            .padding(16.dp)
    ) {
        FilterNameField(
            name = params.name,
            onNameChange = params.onNameChange,
        )

        HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

        ColorPickerRow(
            color = params.color,
            onClick = params.onColorClick,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        IconPickerRow(
            icon = params.icon,
            onClick = params.onIconClick,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SqlQueryField(
            sql = params.sql,
            onSqlChange = params.onSqlChange,
        )

        if (params.filterId != null) {
            DeleteFilterButton(onDeleteClick = params.onDeleteClick)
        }
    }
}

@Composable
internal fun FilterDiscardDialog(show: Boolean, onDismiss: () -> Unit, onDiscard: () -> Unit) {
    @OptIn(ExperimentalMaterial3Api::class)
    if (show) { BasicAlertDialog(onDismissRequest = onDismiss) { Text("Discard?"); Button(onClick = onDiscard) { Text("Discard") } } }
}

@Composable
internal fun FilterSettingsDialogs(
    params: FilterDialogParams,
) {
    FilterDiscardDialog(
        show = params.showDiscardDialog,
        onDismiss = params.onDismissDiscard,
        onDiscard = params.onDiscard,
    )
    if (params.showColorPicker) {
        ColorPickerDialog(
            hasPro = true,
            colors = params.pickerColors,
            onDismiss = params.onDismissColorPicker,
            onColorSelected = params.onColorSelected,
        )
    }
    if (params.showIconPicker) {
        IconPickerDialog(
            selectedIcon = null,
            onIconSelected = params.onIconSelected,
            onDismissRequest = params.onDismissIconPicker,
        )
    }
}
