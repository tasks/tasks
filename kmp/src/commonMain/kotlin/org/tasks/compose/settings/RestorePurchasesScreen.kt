package org.tasks.compose.settings

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.TasksUrls
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.PurchaseEntitlement.StoreResult
import org.tasks.billing.UnlockSource
import org.tasks.compose.PlatformBackHandler
import org.tasks.billing.LinkResult
import org.tasks.billing.StatusResult
import org.tasks.kmp.formatNumber
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.app_settings
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.done
import tasks.kmp.generated.resources.error_contact_support
import tasks.kmp.generated.resources.error_create_link_failed
import tasks.kmp.generated.resources.error_github_verification_failed
import tasks.kmp.generated.resources.error_no_sponsorship
import tasks.kmp.generated.resources.error_sponsorship_delay
import tasks.kmp.generated.resources.error_encryption_failed
import tasks.kmp.generated.resources.card_subscription
import tasks.kmp.generated.resources.github_sponsorship
import tasks.kmp.generated.resources.google_play_subscription
import tasks.kmp.generated.resources.ic_google
import tasks.kmp.generated.resources.ic_octocat
import tasks.kmp.generated.resources.purchases_restored
import tasks.kmp.generated.resources.link_desktop
import tasks.kmp.generated.resources.link_desktop_success
import tasks.kmp.generated.resources.qr_code
import tasks.kmp.generated.resources.restore_purchases
import tasks.kmp.generated.resources.sign_in
import tasks.kmp.generated.resources.retry
import tasks.kmp.generated.resources.scan_qr_code
import tasks.kmp.generated.resources.sponsor_on_github
import tasks.kmp.generated.resources.unlock_pro_opening_browser
import tasks.kmp.generated.resources.unlock_pro_scan_heading
import tasks.kmp.generated.resources.unlock_pro_step_1
import tasks.kmp.generated.resources.unlock_pro_step_2
import tasks.kmp.generated.resources.unlock_pro_step_3

@Serializable
sealed interface DesktopProState {
    @Serializable
    data object Loading : DesktopProState

    @Serializable
    data class ShowQr(val code: String, val expiresAt: Long) : DesktopProState

    @Serializable
    data object Success : DesktopProState

    @Serializable
    data class Error(val failure: LinkFailure) : DesktopProState
}

@Serializable
enum class LinkFailure {
    CREATE_FAILED,
    ENCRYPTION_FAILED,
    SERVER_FAILED,
}

@Composable
private fun LinkFailure.message(): String = when (this) {
    LinkFailure.CREATE_FAILED -> stringResource(Res.string.error_create_link_failed)
    LinkFailure.ENCRYPTION_FAILED -> stringResource(Res.string.error_encryption_failed)
    LinkFailure.SERVER_FAILED ->
        stringResource(Res.string.error_contact_support, TasksUrls.SUPPORT_EMAIL)
}

data class DesktopLinkCallbacks(
    val onCreateLink: suspend () -> LinkResult?,
    val onPollStatus: suspend (code: String) -> StatusResult?,
    val onLinkSuccess: suspend (
        jwt: String,
        refreshToken: String,
        sku: String?,
        formattedPrice: String?,
    ) -> StoreResult,
)

private const val MAX_POLL_ERRORS = 5
private const val FALLBACK_LINK_TTL_SECONDS = 600L
private const val MAX_EXPIRED_LINKS = 2

internal val UnlockSourceSaver = jsonSaver<UnlockSource?>(
    tag = "RestorePurchases",
    what = "selection",
) { it }

internal val DesktopProStateSaver = jsonSaver<DesktopProState>(
    tag = "RestorePurchases",
    what = "link",
) { it ?: DesktopProState.Loading }

@Serializable
sealed interface GitHubProState {
    @Serializable
    data object Idle : GitHubProState

    @Serializable
    data object OpeningBrowser : GitHubProState

    @Serializable
    data object Success : GitHubProState

    @Serializable
    data object NotSponsor : GitHubProState

    @Serializable
    data class NotStored(val encryptionFailed: Boolean) : GitHubProState

    @Serializable
    data object Failed : GitHubProState
}

internal val GitHubProStateSaver = jsonSaver<GitHubProState>(
    tag = "RestorePurchases",
    what = "sponsor check",
) {
    when (it) {
        null, GitHubProState.OpeningBrowser -> GitHubProState.Idle
        else -> it
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestorePurchasesScreen(
    onBack: () -> Unit,
    sources: List<UnlockSource>,
    stripe: StripeRestoreCallbacks,
    desktopLink: DesktopLinkCallbacks? = null,
    onGitHubSignIn: suspend () -> GitHubSponsorClient.VerifyResult,
    onOpenSponsorPage: () -> Unit,
    onGooglePlaySelected: () -> Unit = {},
    onGitHubSelected: () -> Unit = {},
    onStripeSelected: () -> Unit = {},
    onNotSponsor: () -> Unit = {},
    onLinkError: () -> Unit = {},
    onGitHubSuccess: () -> Unit = {},
    onGitHubFailed: () -> Unit = {},
    successButtonText: String? = null,
) {
    var selection by rememberSaveable(stateSaver = UnlockSourceSaver) {
        mutableStateOf<UnlockSource?>(null)
    }
    val available = remember(sources, desktopLink) {
        sources.filter { it != UnlockSource.GOOGLE_PLAY || desktopLink != null }
    }
    var qrState by rememberSaveable(stateSaver = DesktopProStateSaver) {
        mutableStateOf<DesktopProState>(DesktopProState.Loading)
    }
    var gitHubState by rememberSaveable(stateSaver = GitHubProStateSaver) {
        mutableStateOf<GitHubProState>(GitHubProState.Idle)
    }
    var linkAttempt by rememberSaveable { mutableIntStateOf(0) }
    val tabState = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()

    fun createLink() {
        qrState = DesktopProState.Loading
        linkAttempt++
    }

    LaunchedEffect(available) {
        selection = selection?.takeIf { it in available }
    }

    // Create a link, poll it until it is scanned, replace it when it expires
    LaunchedEffect(selection, linkAttempt) {
        if (selection != UnlockSource.GOOGLE_PLAY) return@LaunchedEffect
        val link = desktopLink ?: return@LaunchedEffect
        var resume = qrState as? DesktopProState.ShowQr
        if (resume == null && qrState !is DesktopProState.Loading) return@LaunchedEffect
        var expiredLinks = 0
        links@ while (true) {
            val showQr = resume ?: run {
                qrState = DesktopProState.Loading
                link.onCreateLink()?.let { DesktopProState.ShowQr(it.code, it.expiresAt) }
            }
            if (showQr == null) {
                onLinkError()
                qrState = DesktopProState.Error(LinkFailure.CREATE_FAILED)
                return@LaunchedEffect
            }
            resume = null
            qrState = showQr
            val expiresAt = showQr.expiresAt.takeIf { it > 0 }
                ?: (currentTimeMillis() / 1000 + FALLBACK_LINK_TTL_SECONDS)
            var consecutiveErrors = 0
            var polled = false
            while (true) {
                if (currentTimeMillis() / 1000 >= expiresAt) {
                    if (polled) {
                        expiredLinks = 0
                    } else if (expiredLinks++ >= MAX_EXPIRED_LINKS) {
                        onLinkError()
                        qrState = DesktopProState.Error(LinkFailure.CREATE_FAILED)
                        return@LaunchedEffect
                    }
                    continue@links
                }
                delay(if (consecutiveErrors > 0) 15_000L else 5_000L)
                val status = link.onPollStatus(showQr.code)
                polled = true
                if (status == null) {
                    consecutiveErrors++
                    if (consecutiveErrors >= MAX_POLL_ERRORS) {
                        onLinkError()
                        qrState = DesktopProState.Error(LinkFailure.CREATE_FAILED)
                        return@LaunchedEffect
                    }
                    continue
                }
                consecutiveErrors = 0
                if (status.status == "confirmed" && status.jwt != null && status.refreshToken != null) {
                    val stored = try {
                        link.onLinkSuccess(status.jwt, status.refreshToken, status.sku, status.formattedPrice)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Logger.e(e) { "Failed to store entitlement" }
                        StoreResult.WRITE_FAILED
                    }
                    qrState = if (stored == StoreResult.STORED) {
                        DesktopProState.Success
                    } else {
                        onLinkError()
                        DesktopProState.Error(
                            if (stored == StoreResult.ENCRYPT_FAILED) {
                                LinkFailure.ENCRYPTION_FAILED
                            } else {
                                LinkFailure.SERVER_FAILED
                            }
                        )
                    }
                    return@LaunchedEffect
                }
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
                is GitHubSponsorClient.VerifyResult.NotStored -> {
                    onGitHubFailed()
                    GitHubProState.NotStored(result.encryptionFailed)
                }
                GitHubSponsorClient.VerifyResult.Failed -> {
                    onGitHubFailed()
                    GitHubProState.Failed
                }
            }
        }
    }

    fun back() {
        if (selection != null) {
            selection = null
            gitHubState = GitHubProState.Idle
        } else {
            onBack()
        }
    }

    PlatformBackHandler(enabled = true, onBack = { back() })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .union(WindowInsets.systemBars)
            .union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { back() }) {
                        SymbolIcon(
                            name = TasksIcons.ARROW_BACK,
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
            when (val current = selection?.takeIf { it in available }) {
                null -> {
                    SelectionContent(
                        sources = available,
                        onSelect = { source ->
                            selection = source
                            when (source) {
                                UnlockSource.GOOGLE_PLAY -> {
                                    onGooglePlaySelected()
                                    createLink()
                                }
                                UnlockSource.GITHUB -> {
                                    onGitHubSelected()
                                    startGitHubSignIn()
                                }
                                UnlockSource.STRIPE -> onStripeSelected()
                            }
                        },
                    )
                }
                else -> tabState.SaveableStateProvider(current.name) {
                    when (current) {
                        UnlockSource.GOOGLE_PLAY -> {
                            QrTab(
                                state = qrState,
                                onBack = onBack,
                                onRetry = { createLink() },
                                successButtonText = successButtonText,
                            )
                        }
                        UnlockSource.GITHUB -> {
                            Column(
                                modifier = Modifier.widthIn(max = 400.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                GitHubTab(
                                    state = gitHubState,
                                    onBack = onBack,
                                    onRetry = { startGitHubSignIn() },
                                    onOpenSponsorPage = onOpenSponsorPage,
                                    successButtonText = successButtonText,
                                )
                            }
                        }
                        UnlockSource.STRIPE -> {
                            Column(
                                modifier = Modifier.widthIn(max = 400.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                StripeTab(
                                    callbacks = stripe,
                                    onBack = onBack,
                                    successButtonText = successButtonText,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionContent(
    sources: List<UnlockSource>,
    onSelect: (UnlockSource) -> Unit,
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
                sources.forEach { source ->
                    UnlockSourceCard(
                        source = source,
                        onClick = { onSelect(source) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(SettingsContentPadding),
                modifier = Modifier.fillMaxWidth(),
            ) {
                sources.forEach { source ->
                    UnlockSourceCard(
                        source = source,
                        onClick = { onSelect(source) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlockSourceCard(
    source: UnlockSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (source) {
        UnlockSource.GOOGLE_PLAY -> SelectionCard(
            title = stringResource(Res.string.google_play_subscription),
            icon = Res.drawable.ic_google,
            onClick = onClick,
            modifier = modifier,
        )
        UnlockSource.GITHUB -> SelectionCard(
            title = stringResource(Res.string.github_sponsorship),
            icon = Res.drawable.ic_octocat,
            iconTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            onClick = onClick,
            modifier = modifier,
        )
        UnlockSource.STRIPE -> SelectionCard(
            title = stringResource(Res.string.card_subscription),
            onClick = onClick,
            modifier = modifier,
        ) {
            SymbolIcon(
                name = TasksIcons.CREDIT_CARD,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp),
            )
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
) = SelectionCard(title = title, onClick = onClick, modifier = modifier) {
    Icon(
        painter = painterResource(icon),
        contentDescription = title,
        tint = iconTint ?: Color.Unspecified,
        modifier = Modifier.size(48.dp),
    )
}

@Composable
private fun SelectionCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
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
            icon()
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
    successButtonText: String? = null,
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
            SuccessContent(
                onBack = onBack,
                message = stringResource(Res.string.link_desktop_success),
                buttonText = successButtonText,
            )
        }
        is DesktopProState.Error -> {
            Text(
                text = s.failure.message(),
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
    successButtonText: String? = null,
) {
    when (state) {
        is GitHubProState.Idle -> {
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.sign_in))
            }
        }
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
            SuccessContent(
                onBack = onBack,
                message = stringResource(Res.string.purchases_restored),
                buttonText = successButtonText,
            )
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
                            TasksUrls.SUPPORT_EMAIL,
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
        is GitHubProState.NotStored -> {
            Text(
                text = if (state.encryptionFailed) {
                    stringResource(Res.string.error_encryption_failed)
                } else {
                    stringResource(Res.string.error_contact_support, TasksUrls.SUPPORT_EMAIL)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.retry))
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
internal fun SuccessContent(
    onBack: () -> Unit,
    message: String,
    buttonText: String? = null,
) {
    Text(
        text = message,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onBack) {
        Text(buttonText ?: stringResource(Res.string.done))
    }
}
