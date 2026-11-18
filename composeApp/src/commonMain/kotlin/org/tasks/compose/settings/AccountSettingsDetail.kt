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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.PlatformConfiguration
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.compose.accounts.Platform
import org.tasks.compose.accounts.SignInErrorDialog
import org.tasks.compose.accounts.SignInProgressDialog
import org.tasks.compose.accounts.supports
import org.tasks.data.entity.CaldavAccount
import org.tasks.viewmodel.GoogleTasksAccountViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.sign_in_with_google

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountSettingsDetail(
    accountId: Long,
    defaultName: StringResource,
    onNavigateBack: () -> Unit,
    platform: Platform,
    loginHint: (CaldavAccount) -> String?,
    signInTitle: StringResource = Res.string.sign_in_with_google,
) {
    val viewModel = koinViewModel<GoogleTasksAccountViewModel>()
    val signInAgainViewModel = koinViewModel<SignInAgainViewModel>(key = "sign-in-again-$accountId")
    val signInState by signInAgainViewModel.signInState.collectAsState()
    val reporting = koinInject<Reporting>()
    val configuration = koinInject<PlatformConfiguration>()
    val canSignIn = configuration.supports(platform)
    LaunchedEffect(accountId) { viewModel.setAccountId(accountId) }
    val state by viewModel.state.collectAsState()
    if (!state.loaded) return
    val accountName = state.account?.name ?: stringResource(defaultName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(accountName) },
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
            GoogleTasksAccountScreen(
                error = state.error,
                isUnauthorized = state.isUnauthorized,
                accountName = accountName,
                onSignIn = if (canSignIn) {
                    {
                        signInAgainViewModel.signIn(
                            platform = platform,
                            loginHint = state.account?.let(loginHint),
                        )
                    }
                } else {
                    null
                },
                onDelete = { viewModel.delete(onNavigateBack) },
                signInTitle = signInTitle,
            )
            SignInErrorDialog(
                signInState = signInState,
                onDismiss = { signInAgainViewModel.dismissError() },
                reporting = reporting,
                source = AnalyticsEvents.SOURCE_SIGN_IN_AGAIN,
                onPaymentRequired = null,
            )
            SignInProgressDialog(
                signInState = signInState,
                onCancel = { signInAgainViewModel.cancel() },
            )
        }
    }
}
