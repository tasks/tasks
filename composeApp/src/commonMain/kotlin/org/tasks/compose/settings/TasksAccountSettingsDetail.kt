package org.tasks.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import org.tasks.auth.SignInHandler
import org.tasks.compose.accounts.Platform
import org.tasks.viewmodel.TasksAccountViewModel
import org.tasks.viewmodel.TasksAccountViewModel.Companion.DEFAULT_TOS_VERSION
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.tasks_org
import tasks.kmp.generated.resources.url_app_passwords
import tasks.kmp.generated.resources.url_donate
import tasks.kmp.generated.resources.url_sponsor
import tasks.kmp.generated.resources.url_tos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksAccountSettingsDetail(
    pane: TasksAccountSettingsPane,
    onNavigateBack: () -> Unit,
    onAddAccountClick: () -> Unit,
) {
    val viewModel = koinViewModel<TasksAccountViewModel>()
    val signInHandler = koinInject<SignInHandler>()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(pane.account.uuid) {
        viewModel.setAccountUuid(pane.account.uuid!!)
        viewModel.refreshAccount()
    }

    val state by viewModel.state.collectAsState()
    val sponsorUrl = stringResource(Res.string.url_sponsor)
    val appPasswordsUrl = stringResource(Res.string.url_app_passwords)
    val tosUrl = stringResource(Res.string.url_tos)
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    scope.launch { signInHandler.signIn(Platform.TASKS_ORG) }
                },
                onSubscribe = {
                    uriHandler.openUri(donateUrl)
                },
                onOpenSponsor = {
                    uriHandler.openUri(sponsorUrl)
                },
                onMigrate = {
                    viewModel.migrateLocalTasks()
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
                onModifySubscription = {},
                onCancelSubscription = {},
                onLogout = {
                    scope.launch {
                        withContext(NonCancellable) {
                            viewModel.logout(state.account ?: pane.account)
                        }
                        onNavigateBack()
                    }
                },
                onAcceptTos = { viewModel.acceptTos(DEFAULT_TOS_VERSION) },
                onViewTos = {
                    uriHandler.openUri(tosUrl)
                },
                onDismissTos = { viewModel.dismissTos() },
            )
        }
    }
}
