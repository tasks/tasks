package org.tasks.presentation.screens

import android.app.Activity.RESULT_OK
import android.app.RemoteInput
import android.content.Intent
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.fillMaxRectangle
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import org.tasks.presentation.components.Card
import org.tasks.presentation.components.Checkbox

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun TaskEditScreen(
    uiState: UiState,
    setTitle: (String) -> Unit,
    save: () -> Unit,
) {
    if (uiState.loading) {
        Box(
            modifier = Modifier.fillMaxRectangle(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else {
        val columnState = rememberResponsiveColumnState(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        )
        val keyboardInputRequest = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val text =
                    result
                        .data
                        ?.let { RemoteInput.getResultsFromIntent(it) }
                        ?.getCharSequence("input")
                        ?: return@rememberLauncherForActivityResult
                setTitle(text.toString())
            }
        }
        ScreenScaffold(
            scrollState = columnState,
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                columnState = columnState,
            ) {
                item {
                    Text("New task")
                }
                item {
                    Card(
                        icon = {
                            Checkbox(
                                completed = uiState.completed,
                                repeating = uiState.repeating,
                                priority = uiState.priority,
                                toggleComplete = {},
                            )
                        },
                        content = {
                            Text(
                                text = uiState.title,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        },
                        onClick = {},
                    )
                }
                item {
                    Button(
                        onClick = { save() },
                        colors = ButtonDefaults.buttonColors(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save")
                    }
                }
            }
        }
        LaunchedEffect(Unit) {
            if (uiState.isNew) {
                val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                val remoteInputs: List<RemoteInput> = listOf(
                    RemoteInput
                        .Builder("input")
                        .setLabel("Enter title")
                        .setAllowFreeFormInput(true)
                        .wearableExtender {
                            setInputActionType(EditorInfo.IME_ACTION_DONE)
                        }
                        .build()
                )
                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                keyboardInputRequest.launch(intent)
            }
        }
    }
}