package org.tasks.compose.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.ui.AiCaptureState
import org.tasks.ui.ReviewItem
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.ai_add_n
import tasks.kmp.generated.resources.ai_input_hint
import tasks.kmp.generated.resources.ai_thinking
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.done

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCaptureSheet(
    state: AiCaptureState,
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggle: (Int) -> Unit,
    onOpenInEditor: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            when (state) {
                AiCaptureState.Input -> InputState(
                    input = input,
                    onInputChange = onInputChange,
                    onSubmit = onSubmit,
                )

                AiCaptureState.Loading -> LoadingState(onCancel = onDismiss)

                is AiCaptureState.Review -> ReviewState(
                    items = state.items,
                    onToggle = onToggle,
                    onOpenInEditor = onOpenInEditor,
                    onConfirm = onConfirm,
                    onCancel = onDismiss,
                )

                is AiCaptureState.Error -> ErrorState(
                    message = stringResource(state.messageRes),
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun InputState(
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // Requesting focus on composition is what raises the keyboard, which puts the GBoard mic
    // one tap away. No speech API or RECORD_AUDIO permission is involved.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    OutlinedTextField(
        value = input,
        onValueChange = onInputChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        label = { Text(stringResource(Res.string.ai_input_hint)) },
        singleLine = false,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Button(onClick = onSubmit, enabled = input.isNotBlank()) {
            Text(stringResource(Res.string.done))
        }
    }
}

@Composable
private fun LoadingState(onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(Res.string.ai_thinking),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onCancel) {
            Text(stringResource(Res.string.cancel))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewState(
    items: List<ReviewItem>,
    onToggle: (Int) -> Unit,
    onOpenInEditor: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val checked = items.count { it.checked }

    LazyColumn(
        modifier = Modifier.heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(items) { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenInEditor(index) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { onToggle(index) },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.task.title.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (item.chips.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item.chips.forEach { chip ->
                                    AssistChip(onClick = { }, label = { Text(chip) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onCancel) {
            Text(stringResource(Res.string.cancel))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onConfirm, enabled = checked > 0) {
            Text(stringResource(Res.string.ai_add_n, checked))
        }
    }
}

@Composable
private fun ErrorState(message: String, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onDismiss) {
            Text(stringResource(Res.string.cancel))
        }
    }
}
