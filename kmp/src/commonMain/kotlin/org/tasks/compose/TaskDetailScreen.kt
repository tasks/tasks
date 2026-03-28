package org.tasks.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.data.entity.Task
import org.tasks.kmp.org.tasks.themes.ColorProvider.priorityColor
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.no_due_date
import tasks.kmp.generated.resources.task_notes_hint
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskDetailScreen(
    title: String,
    notes: String,
    priority: Int,
    isCompleted: Boolean,
    dueDate: Long,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPriorityChange: (Int) -> Unit,
    onToggleComplete: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(Unit) {
        onDispose { onSave() }
    }

    val isDark = isSystemInDarkTheme()
    val priorityArgb = Color(priorityColor(priority, isDark))
    val transparent = Color.Transparent

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        // Title + completion checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = priorityArgb,
                    uncheckedColor = priorityArgb,
                ),
            )
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    fontWeight = MaterialTheme.typography.headlineSmall.fontWeight,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
                ),
                placeholder = {
                    Text(
                        "Title",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = transparent,
                    unfocusedBorderColor = transparent,
                ),
            )
        }

        HorizontalDivider()

        // Priority row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.width(8.dp))
            for (p in listOf(Task.Priority.NONE, Task.Priority.LOW, Task.Priority.MEDIUM, Task.Priority.HIGH)) {
                val pColor = Color(priorityColor(p, isDark))
                RadioButton(
                    selected = priority == p,
                    onClick = { onPriorityChange(p) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = pColor,
                        unselectedColor = pColor,
                    ),
                )
            }
        }

        HorizontalDivider()

        // Due date row (read-only)
        val formattedDate = remember(dueDate) {
            if (dueDate == 0L) null
            else DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                .format(Date(dueDate))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = formattedDate ?: stringResource(Res.string.no_due_date),
                style = MaterialTheme.typography.bodyLarge,
                color = if (formattedDate != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider()

        // Notes row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Notes,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp),
            )
            Spacer(Modifier.width(16.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        stringResource(Res.string.task_notes_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = transparent,
                    unfocusedBorderColor = transparent,
                ),
                minLines = 4,
            )
        }
    }
}
