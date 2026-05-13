package org.tasks.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.tasks.kmp.formatNumber
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.done
import tasks.kmp.generated.resources.error_confirmation_failed
import tasks.kmp.generated.resources.error_network
import tasks.kmp.generated.resources.link_desktop
import tasks.kmp.generated.resources.link_desktop_step_1
import tasks.kmp.generated.resources.link_desktop_step_2
import tasks.kmp.generated.resources.link_desktop_step_3
import tasks.kmp.generated.resources.link_desktop_success
import tasks.kmp.generated.resources.google_play_subscription
import tasks.kmp.generated.resources.restore_purchases
import tasks.kmp.generated.resources.scan_qr_code
import tasks.kmp.generated.resources.upgrade_to_pro

sealed interface LinkDesktopState {
    data object Idle : LinkDesktopState
    data object Scanning : LinkDesktopState
    data object Confirming : LinkDesktopState
    data object Success : LinkDesktopState
    data class Error(val message: String) : LinkDesktopState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkDesktopScreen(
    onBack: () -> Unit,
    onScan: suspend () -> String?,
    onConfirm: suspend (code: String) -> Boolean,
) {
    var state by remember { mutableStateOf<LinkDesktopState>(LinkDesktopState.Idle) }
    val scope = rememberCoroutineScope()
    val networkErrorMessage = stringResource(Res.string.error_network)
    val confirmationErrorMessage = stringResource(Res.string.error_confirmation_failed)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
                title = {
                    Text(text = stringResource(Res.string.link_desktop))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = SettingsContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (state) {
                    is LinkDesktopState.Success -> {
                        Text(
                            text = stringResource(Res.string.link_desktop_success),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onBack) {
                            Text(stringResource(Res.string.done))
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(SettingsCardRadius),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SettingsContentPadding),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                            ) {
                                StepText(1, stringResource(Res.string.link_desktop_step_1))
                                StepText(
                                    2,
                                    stringResource(
                                        Res.string.link_desktop_step_2,
                                        stringResource(Res.string.upgrade_to_pro),
                                        stringResource(Res.string.restore_purchases),
                                    )
                                )
                                StepText(
                                    3,
                                    stringResource(
                                        Res.string.link_desktop_step_3,
                                        stringResource(Res.string.google_play_subscription),
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    state = LinkDesktopState.Scanning
                                    val code = onScan()
                                    if (code != null) {
                                        state = LinkDesktopState.Confirming
                                        try {
                                            val success = onConfirm(code)
                                            state = if (success) {
                                                LinkDesktopState.Success
                                            } else {
                                                LinkDesktopState.Error(confirmationErrorMessage)
                                            }
                                        } catch (e: java.io.IOException) {
                                            state = LinkDesktopState.Error(networkErrorMessage)
                                        } catch (e: Exception) {
                                            state = LinkDesktopState.Error(confirmationErrorMessage)
                                        }
                                    } else {
                                        state = LinkDesktopState.Idle
                                    }
                                }
                            },
                            enabled = state is LinkDesktopState.Idle || state is LinkDesktopState.Error,
                        ) {
                            Text(stringResource(Res.string.scan_qr_code))
                        }
                        if (state is LinkDesktopState.Error) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = (state as LinkDesktopState.Error).message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (state is LinkDesktopState.Confirming) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepText(number: Int, text: String) {
    Text(
        text = "${formatNumber(number)}. $text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
