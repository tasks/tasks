package org.tasks.compose.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.tasks.billing.LinkResult
import org.tasks.billing.StatusResult
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.done
import tasks.kmp.generated.resources.error_create_link_failed
import tasks.kmp.generated.resources.link_desktop_success
import tasks.kmp.generated.resources.qr_code
import tasks.kmp.generated.resources.retry
import tasks.kmp.generated.resources.unlock_pro
import tasks.kmp.generated.resources.unlock_pro_instructions

sealed interface DesktopProState {
    data object Loading : DesktopProState
    data class ShowQr(val code: String, val expiresAt: Long) : DesktopProState
    data object Success : DesktopProState
    data class Error(val message: String) : DesktopProState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopProScreen(
    onBack: () -> Unit,
    onCreateLink: suspend () -> LinkResult?,
    onPollStatus: suspend (code: String) -> StatusResult?,
    onLinkSuccess: suspend (jwt: String, refreshToken: String, sku: String?, formattedPrice: String?) -> Unit,
) {
    var state by remember { mutableStateOf<DesktopProState>(DesktopProState.Loading) }
    var linkGeneration by remember { mutableIntStateOf(0) }
    val errorMessage = stringResource(Res.string.error_create_link_failed)

    // Create or refresh link
    LaunchedEffect(linkGeneration) {
        state = DesktopProState.Loading
        val result = onCreateLink()
        if (result != null) {
            state = DesktopProState.ShowQr(result.code, result.expiresAt)
        } else {
            state = DesktopProState.Error(errorMessage)
        }
    }

    // Poll for status, auto-renew on expiry
    val pollCode = (state as? DesktopProState.ShowQr)?.code
    LaunchedEffect(pollCode) {
        val qrState = state as? DesktopProState.ShowQr ?: return@LaunchedEffect
        var consecutiveErrors = 0
        while (true) {
            val now = currentTimeMillis() / 1000
            if (qrState.expiresAt in 1..now) {
                linkGeneration++
                return@LaunchedEffect
            }
            delay(if (consecutiveErrors > 0) 15_000L else 5_000L)
            val status = onPollStatus(qrState.code)
            if (status == null) {
                consecutiveErrors++
                continue
            }
            consecutiveErrors = 0
            if (status.status == "confirmed" && status.jwt != null && status.refreshToken != null) {
                onLinkSuccess(status.jwt, status.refreshToken, status.sku, status.formattedPrice)
                state = DesktopProState.Success
                break
            }
        }
    }

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
                    Text(text = stringResource(Res.string.unlock_pro))
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
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (val s = state) {
                    is DesktopProState.Loading -> {
                        CircularProgressIndicator()
                    }
                    is DesktopProState.ShowQr -> {
                        Text(
                            text = stringResource(Res.string.unlock_pro_instructions),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SettingsContentPadding),
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Image(
                            painter = rememberQrCodePainter(
                                data = s.code,
                                colors = QrColors(
                                    dark = QrBrush.solid(MaterialTheme.colorScheme.onSurface),
                                ),
                            ),
                            contentDescription = stringResource(Res.string.qr_code),
                            modifier = Modifier.size(256.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    is DesktopProState.Success -> {
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
                    is DesktopProState.Error -> {
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { linkGeneration++ }) {
                            Text(stringResource(Res.string.retry))
                        }
                    }
                }
            }
        }
    }
}
