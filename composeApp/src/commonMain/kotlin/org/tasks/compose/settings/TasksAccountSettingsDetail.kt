package org.tasks.compose.settings

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.auth.OAuthProvider
import org.tasks.auth.SignInHandler
import org.tasks.billing.EntitlementProvider
import org.tasks.billing.StripePurchases
import org.tasks.compose.pricing.cardPortalLauncher
import org.tasks.caldav.PurchaseTokenInUseException
import org.tasks.compose.accounts.Platform
import org.tasks.TasksUrls
import org.tasks.viewmodel.TasksAccountViewModel
import org.tasks.viewmodel.TasksAccountViewModel.Companion.DEFAULT_TOS_VERSION
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.tasks_org
import tasks.kmp.generated.resources.url_app_passwords
import tasks.kmp.generated.resources.url_donate
import tasks.kmp.generated.resources.url_sponsor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksAccountSettingsDetail(
    pane: TasksAccountSettingsPane,
    onNavigateBack: () -> Unit,
    onAddAccountClick: () -> Unit,
    onModifySubscription: () -> Unit,
) {
    val viewModel = koinViewModel<TasksAccountViewModel>()
    val signInHandler = koinInject<SignInHandler>()
    val stripePurchases = koinInject<StripePurchases>()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val openCardPortal = cardPortalLauncher(
        portalUrl = { stripePurchases.portalUrl() },
        openUrl = { uriHandler.openUri(it) },
    )
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(pane.uuid) {
        viewModel.setAccountUuid(pane.uuid)
        viewModel.refreshAccount()
    }

    val state by viewModel.state.collectAsState()
    var conflictingAccount by remember { mutableStateOf<String?>(null) }
    if (!state.loaded) return
    val sponsorUrl = stringResource(Res.string.url_sponsor)
    val appPasswordsUrl = stringResource(Res.string.url_app_passwords)
    val donateUrl = stringResource(Res.string.url_donate)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.account?.name?.takeIf { it.isNotBlank() }
                            ?: stringResource(Res.string.tasks_org)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            name = TasksIcons.ARROW_BACK,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TasksAccountScreen(
                state = state,
                onSignIn = {
                    val provider = when {
                        state.isGithub -> OAuthProvider.GITHUB
                        state.isApple -> OAuthProvider.APPLE
                        else -> OAuthProvider.GOOGLE
                    }
                    scope.launch {
                        try {
                            signInHandler.signIn(Platform.TASKS_ORG, provider)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: PurchaseTokenInUseException) {
                            conflictingAccount = e.existingAccount
                        } catch (e: Exception) {
                            Logger.e(e) { "Tasks.org re-authentication failed" }
                        }
                    }
                },
                onSubscribe = {
                    uriHandler.openUri(donateUrl)
                },
                onOpenSponsor = {
                    uriHandler.openUri(sponsorUrl)
                },
                onCopyEmail = {
                    state.inboundEmail?.let {
                        clipboardManager.setText(AnnotatedString(it))
                    }
                },
                onRegenerateEmail = {
                    viewModel.regenerateInboundEmail()
                },
                onSelectCalendar = { calendarUri ->
                    viewModel.setInboundCalendar(calendarUri)
                },
                onDeletePassword = { id, _ ->
                    viewModel.deletePassword(id)
                },
                onGeneratePassword = { description ->
                    viewModel.requestNewPassword(description)
                },
                onOpenAppPasswordsInfo = {
                    uriHandler.openUri(appPasswordsUrl)
                },
                onCopyField = { _, value ->
                    clipboardManager.setText(AnnotatedString(value))
                },
                onClearNewPassword = {
                    viewModel.clearNewPassword()
                },
                onRefreshPasswords = {
                    viewModel.refreshAccount()
                },
                onOpenHelp = {
                    uriHandler.openUri(appPasswordsUrl)
                },
                onAddAccount = onAddAccountClick,
                onModifySubscription = onModifySubscription,
                onCancelSubscription = {
                    when (state.subscriptionProvider) {
                        EntitlementProvider.STRIPE -> openCardPortal()
                        EntitlementProvider.GITHUB_SPONSOR -> uriHandler.openUri(sponsorUrl)
                        EntitlementProvider.PLAY, null ->
                            uriHandler.openUri(TasksUrls.GOOGLE_PLAY_SUBSCRIPTIONS)
                    }
                },
                onLogout = {
                    scope.launch {
                        withContext(NonCancellable) {
                            viewModel.logout()
                        }
                        onNavigateBack()
                    }
                },
                onAcceptTos = { viewModel.acceptTos(DEFAULT_TOS_VERSION) },
                onViewTos = {
                    uriHandler.openUri(TasksUrls.TOS)
                },
                onDismissTos = { viewModel.dismissTos() },
            )
            conflictingAccount?.let { existingAccount ->
                MessageDialog(
                    message = conflictingAccountMessage(existingAccount),
                    onDismiss = { conflictingAccount = null },
                )
            }
        }
    }
}
