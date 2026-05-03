package org.tasks.compose.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.tasks.kmp.formatNumber
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.LinkResult
import org.tasks.billing.StatusResult
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.app_settings
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.done
import tasks.kmp.generated.resources.error_create_link_failed
import tasks.kmp.generated.resources.error_contact_support
import tasks.kmp.generated.resources.error_github_verification_failed
import tasks.kmp.generated.resources.error_no_sponsorship
import tasks.kmp.generated.resources.error_sponsorship_delay
import tasks.kmp.generated.resources.github_sponsorship
import tasks.kmp.generated.resources.google_play_subscription
import tasks.kmp.generated.resources.ic_google
import tasks.kmp.generated.resources.ic_octocat
import tasks.kmp.generated.resources.link_desktop
import tasks.kmp.generated.resources.link_desktop_success
import tasks.kmp.generated.resources.qr_code
import tasks.kmp.generated.resources.restore_purchases
import tasks.kmp.generated.resources.retry
import tasks.kmp.generated.resources.scan_qr_code
import tasks.kmp.generated.resources.sponsor_on_github
import tasks.kmp.generated.resources.support_email
import tasks.kmp.generated.resources.unlock_pro_opening_browser
import tasks.kmp.generated.resources.unlock_pro_scan_heading
import tasks.kmp.generated.resources.unlock_pro_step_1
import tasks.kmp.generated.resources.unlock_pro_step_2
import tasks.kmp.generated.resources.unlock_pro_step_3

sealed interface DesktopProState {
    data object Loading : DesktopProState
    data class ShowQr(val code: String, val expiresAt: Long) : DesktopProState
    data object Success : DesktopProState
    data class Error(val message: String) : DesktopProState
}

sealed interface GitHubProState {
    data object Idle : GitHubProState
    data object OpeningBrowser : GitHubProState
    data object Success : GitHubProState
    data object NotSponsor : GitHubProState
    data object Failed : GitHubProState
}

private enum class Selection { None, GooglePlay, GitHub }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopProScreen(
    onBack: () -> Unit,
    onCreateLink: suspend () -> LinkResult?,
    onPollStatus: suspend (code: String) -> StatusResult?,
    onLinkSuccess: suspend (jwt: String, refreshToken: String, sku: String?, formattedPrice: String?) -> Unit,
    onGitHubSignIn: suspend () -> GitHubSponsorClient.VerifyResult,
    onOpenSponsorPage: () -> Unit,
    onGooglePlaySelected: () -> Unit = {},
    onGitHubSelected: () -> Unit = {},
    onNotSponsor: () -> Unit = {},
    onLinkError: () -> Unit = {},
    onGitHubSuccess: () -> Unit = {},
    onGitHubFailed: () -> Unit = {},
) {
    var selection by remember { mutableStateOf(Selection.None) }
    var qrState by remember { mutableStateOf<DesktopProState>(DesktopProState.Loading) }
    var gitHubState by remember { mutableStateOf<GitHubProState>(GitHubProState.Idle) }
    var linkGeneration by remember { mutableIntStateOf(0) }
    val errorMessage = stringResource(Res.string.error_create_link_failed)
    val scope = rememberCoroutineScope()

    // Create or refresh link when Google Play is selected
    LaunchedEffect(selection, linkGeneration) {
        if (selection != Selection.GooglePlay) return@LaunchedEffect
        qrState = DesktopProState.Loading
        val result = onCreateLink()
        if (result != null) {
            qrState = DesktopProState.ShowQr(result.code, result.expiresAt)
        } else {
            qrState = DesktopProState.Error(errorMessage)
            onLinkError()
        }
    }

    // Poll for status, auto-renew on expiry
    val pollCode = (qrState as? DesktopProState.ShowQr)?.code
    LaunchedEffect(pollCode) {
        val showQr = qrState as? DesktopProState.ShowQr ?: return@LaunchedEffect
        var consecutiveErrors = 0
        while (true) {
            val now = currentTimeMillis() / 1000
            if (showQr.expiresAt in 1..now) {
                linkGeneration++
                return@LaunchedEffect
            }
            delay(if (consecutiveErrors > 0) 15_000L else 5_000L)
            val status = onPollStatus(showQr.code)
            if (status == null) {
                consecutiveErrors++
                continue
            }
            consecutiveErrors = 0
            if (status.status == "confirmed" && status.jwt != null && status.refreshToken != null) {
                onLinkSuccess(status.jwt, status.refreshToken, status.sku, status.formattedPrice)
                qrState = DesktopProState.Success
                break
            }
        }
    }

    fun startGitHubSignIn() {
        gitHubState = GitHubProState.OpeningBrowser
        scope.launch {
            val result = onGitHubSignIn()
            gitHubState = when (result) {
                GitHubSponsorClient.VerifyResult.Success -> {
                    onGitHubSuccess()
                    GitHubProState.Success
                }
                GitHubSponsorClient.VerifyResult.NotSponsor -> {
                    onNotSponsor()
                    GitHubProState.NotSponsor
                }
                GitHubSponsorClient.VerifyResult.Failed -> {
                    onGitHubFailed()
                    GitHubProState.Failed
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (selection != Selection.None) {
                            selection = Selection.None
                            gitHubState = GitHubProState.Idle
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
                title = {
                    Text(text = stringResource(Res.string.restore_purchases))
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (selection) {
                Selection.None -> {
                    SelectionContent(
                        onGooglePlay = {
                            selection = Selection.GooglePlay
                            onGooglePlaySelected()
                        },
                        onGitHub = {
                            selection = Selection.GitHub
                            onGitHubSelected()
                            startGitHubSignIn()
                        },
                    )
                }
                Selection.GooglePlay -> {
                    QrTab(
                        state = qrState,
                        onBack = onBack,
                        onRetry = { linkGeneration++ },
                    )
                }
                Selection.GitHub -> {
                    Column(
                        modifier = Modifier.widthIn(max = 400.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GitHubTab(
                            state = gitHubState,
                            onBack = onBack,
                            onRetry = { startGitHubSignIn() },
                            onOpenSponsorPage = onOpenSponsorPage,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionContent(
    onGooglePlay: () -> Unit,
    onGitHub: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SettingsContentPadding),
    ) {
        if (maxWidth >= 500.dp) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SettingsContentPadding),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            ) {
                SelectionCard(
                    title = stringResource(Res.string.google_play_subscription),
                    icon = Res.drawable.ic_google,
                    onClick = onGooglePlay,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                SelectionCard(
                    title = stringResource(Res.string.github_sponsorship),
                    icon = Res.drawable.ic_octocat,
                    iconTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    onClick = onGitHub,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(SettingsContentPadding),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SelectionCard(
                    title = stringResource(Res.string.google_play_subscription),
                    icon = Res.drawable.ic_google,
                    onClick = onGooglePlay,
                    modifier = Modifier.fillMaxWidth(),
                )
                SelectionCard(
                    title = stringResource(Res.string.github_sponsorship),
                    icon = Res.drawable.ic_octocat,
                    iconTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    onClick = onGitHub,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SelectionCard(
    title: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(SettingsCardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = iconTint ?: Color.Unspecified,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun QrTab(
    state: DesktopProState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    when (val s = state) {
        is DesktopProState.Loading -> {
            CircularProgressIndicator()
        }
        is DesktopProState.ShowQr -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SettingsContentPadding),
            ) {
                if (maxWidth >= 500.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        QrCode(code = s.code)
                        Spacer(modifier = Modifier.width(32.dp))
                        QrInstructions()
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        QrCode(code = s.code)
                        Spacer(modifier = Modifier.height(24.dp))
                        QrInstructions()
                    }
                }
            }
        }
        is DesktopProState.Success -> {
            SuccessContent(onBack = onBack)
        }
        is DesktopProState.Error -> {
            Text(
                text = s.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.retry))
            }
        }
    }
}

@Composable
private fun QrCode(code: String) {
    Image(
        painter = rememberQrCodePainter(
            data = code,
            colors = QrColors(
                dark = QrBrush.solid(MaterialTheme.colorScheme.onSurface),
            ),
        ),
        contentDescription = stringResource(Res.string.qr_code),
        modifier = Modifier.size(256.dp),
    )
}

@Composable
private fun QrInstructions() {
    Column {
        Text(
            text = stringResource(Res.string.unlock_pro_scan_heading),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        StepText(1, stringResource(Res.string.unlock_pro_step_1))
        StepText(
            2,
            stringResource(
                Res.string.unlock_pro_step_2,
                stringResource(Res.string.app_settings),
                stringResource(Res.string.link_desktop),
            )
        )
        StepText(
            3,
            stringResource(
                Res.string.unlock_pro_step_3,
                stringResource(Res.string.scan_qr_code),
            )
        )
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

@Composable
private fun GitHubTab(
    state: GitHubProState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenSponsorPage: () -> Unit,
) {
    when (state) {
        is GitHubProState.Idle,
        is GitHubProState.OpeningBrowser -> {
            Text(
                text = stringResource(Res.string.unlock_pro_opening_browser),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
        }
        is GitHubProState.Success -> {
            SuccessContent(onBack = onBack)
        }
        is GitHubProState.NotSponsor -> {
            Card(
                shape = RoundedCornerShape(SettingsCardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SettingsContentPadding),
            ) {
                Column(
                    modifier = Modifier.padding(SettingsContentPadding),
                ) {
                    Text(
                        text = stringResource(Res.string.error_no_sponsorship),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.error_sponsorship_delay),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            Res.string.error_contact_support,
                            stringResource(Res.string.support_email),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(onClick = onRetry) {
                    Text(stringResource(Res.string.retry))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = onOpenSponsorPage) {
                    Text(stringResource(Res.string.sponsor_on_github))
                }
            }
        }
        is GitHubProState.Failed -> {
            Text(
                text = stringResource(Res.string.error_github_verification_failed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.retry))
            }
        }
    }
}

@Composable
private fun SuccessContent(onBack: () -> Unit) {
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
