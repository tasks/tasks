package org.tasks.compose.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.PlatformBackHandler
import org.tasks.data.entity.Task
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.due_date
import tasks.kmp.generated.resources.repeat_type_completion_capitalized
import tasks.kmp.generated.resources.repeat_type_completion_description
import tasks.kmp.generated.resources.repeat_type_due_description
import tasks.kmp.generated.resources.repeats_from

private val SheetHorizontalPadding = 24.dp
private val OptionVerticalPadding = 16.dp
private const val SelectedBackgroundAlpha = 0.08f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatFromPickerSheet(
    repeatFrom: @Task.RepeatFrom Int,
    onSelected: (@Task.RepeatFrom Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dismiss = rememberDismissWithAnimation(sheetState)
    PlatformBackHandler(enabled = true, onBack = { dismiss(onDismiss) })
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                text = stringResource(Res.string.repeats_from),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = SheetHorizontalPadding,
                    end = SheetHorizontalPadding,
                    bottom = OptionVerticalPadding,
                ),
            )
            RepeatFromOption(
                title = stringResource(Res.string.due_date),
                description = stringResource(Res.string.repeat_type_due_description),
                selected = repeatFrom == Task.RepeatFrom.DUE_DATE,
                onClick = { dismiss { onSelected(Task.RepeatFrom.DUE_DATE) } },
            )
            RepeatFromOption(
                title = stringResource(Res.string.repeat_type_completion_capitalized),
                description = stringResource(Res.string.repeat_type_completion_description),
                selected = repeatFrom == Task.RepeatFrom.COMPLETION_DATE,
                onClick = { dismiss { onSelected(Task.RepeatFrom.COMPLETION_DATE) } },
            )
            Spacer(modifier = Modifier.height(OptionVerticalPadding))
        }
    }
}

@Composable
private fun RepeatFromOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) primary.copy(alpha = SelectedBackgroundAlpha) else Color.Transparent)
            .padding(horizontal = SheetHorizontalPadding, vertical = OptionVerticalPadding),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = primary,
                modifier = Modifier
                    .padding(start = OptionVerticalPadding)
                    .size(24.dp),
            )
        }
    }
}
