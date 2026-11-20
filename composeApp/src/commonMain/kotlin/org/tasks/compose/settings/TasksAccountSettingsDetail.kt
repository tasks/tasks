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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.auth.OAuthProvider
import org.tasks.auth.tasksOrgProvider
import org.tasks.billing.EntitlementProvider
import org.tasks.billing.StripePurchases
import org.tasks.compose.pricing.cardPortalLauncher
import org.tasks.compose.accounts.Platform
import org.tasks.compose.accounts.SignInErrorDialog
import org.tasks.compose.accounts.SignInProgressDialog
import org.tasks.TasksUrls
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.viewmodel.TasksAccountViewModel
import org.tasks.viewmodel.TasksAccountViewModel.Companion.DEFAULT_TOS_VERSION
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.tasks_org
import tasks.kmp.generated.resources.url_app_passwords
import tasks.kmp.generated.resources.url_sponsor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksAccountSettingsDetail(
    pane: TasksAccountSettingsPane,
    onNavigateBack: () -> Unit,
    onAddAccountClick: () -> Unit,
    onModifySubscription: () -> Unit,
    onSubscribe: () -> Unit,
) {
    val viewModel = koinViewModel<TasksAccountViewModel>()
    val signInAgainViewModel = koinViewModel<SignInAgainViewModel>(key = "sign-in-again-${pane.uuid}")
    val reporting = koinInject<Reporting>()
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
    }

    val state by viewModel.state.collectAsState()
    val signInState by signInAgainViewModel.signInState.collectAsState()
    val account = state.account?.takeIf { it.uuid == pane.uuid }
    LaunchedEffect(account?.uuid, account?.password) {
        if (account != null) {
            viewModel.refreshAccount()
        }
    }
    if (!state.loaded) return
    val sponsorUrl = stringResource(Res.string.url_sponsor)
    val appPasswordsUrl = stringResource(Res.string.url_app_passwords)

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
                    signInAgainViewModel.signIn(
                        platform = Platform.TASKS_ORG,
                        provider = state.account?.tasksOrgProvider ?: OAuthProvider.GOOGLE,
                        loginHint = state.account?.name,
                    )
                },
                onSubscribe = onSubscribe,
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
            SignInErrorDialog(
                signInState = signInState,
                onDismiss = { signInAgainViewModel.dismissError() },
                onRetry = { signInAgainViewModel.retry() },
                reporting = reporting,
                source = AnalyticsEvents.SOURCE_SIGN_IN_AGAIN,
                onPaymentRequired = onSubscribe,
            )
            SignInProgressDialog(
                signInState = signInState,
                onCancel = { signInAgainViewModel.cancel() },
            )
        }
    }
}
