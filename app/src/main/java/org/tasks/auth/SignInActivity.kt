package org.tasks.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.jetbrains.compose.resources.stringResource as kmpStringResource
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.compose.settings.conflictingAccountMessage
import org.tasks.compose.settings.signedInAsOtherMessage
import org.tasks.resources.getString
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.url_sponsor
import tasks.kmp.generated.resources.wrong_account
import org.tasks.R
import org.tasks.TasksApplication.Companion.IS_GOOGLE_PLAY
import org.tasks.auth.SignInViewModel.Outcome
import org.tasks.auth.SignInViewModel.State
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivity.Companion.EXTRA_NAME_YOUR_PRICE
import org.tasks.billing.PurchaseActivity.Companion.EXTRA_SOURCE
import org.tasks.data.dao.CaldavDao
import org.tasks.extensions.Context.openUri
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.ok
import tasks.kmp.generated.resources.sign_in
import javax.inject.Inject

@AndroidEntryPoint
class SignInActivity : ComponentActivity() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var caldavDao: CaldavDao

    private val viewModel: SignInViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is State.Finished -> finish(state.outcome)
                        is State.SubscriptionRequired ->
                            if (IS_GOOGLE_PLAY && !state.purchasing) {
                                startPurchase()
                            }
                        else -> {}
                    }
                }
            }
        }

        lifecycleScope.launch {
            val target = resolveSignIn(requestedProvider())
            val autoSelect = target.provider
            if (autoSelect != null) {
                setContent {
                    TasksTheme(
                        theme = theme.themeBase.index,
                        primary = theme.themeColor.primaryColor,
                    ) {
                        ErrorDialogs()
                    }
                }
                viewModel.signIn(autoSelect, target.loginHint)
            } else {
                setContent {
                    TasksTheme(
                        theme = theme.themeBase.index,
                        primary = theme.themeColor.primaryColor,
                    ) {
                        ErrorDialogs()
                        if (viewModel.state.collectAsStateWithLifecycle().value == State.Idle) {
                            Dialog(onDismissRequest = { finish() }) {
                                org.tasks.compose.SignInProviderDialog(
                                    onSelected = { provider ->
                                        viewModel.signIn(provider.oauthProvider, loginHint = null)
                                    },
                                    onHelp = {
                                        openUri(R.string.help_url_sync)
                                        finish()
                                    },
                                    onCancel = { finish() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun resolveSignIn(provider: OAuthProvider?): TasksOrgSignIn =
        caldavDao.resolveTasksOrgSignIn(
            provider = provider,
            account = intent.getStringExtra(EXTRA_ACCOUNT)?.let { caldavDao.getAccountByUuid(it) },
        )

    private fun retry(provider: OAuthProvider) {
        lifecycleScope.launch {
            val target = resolveSignIn(provider)
            viewModel.retry(target.provider ?: provider, target.loginHint)
        }
    }

    private fun requestedProvider(): OAuthProvider? =
        intent.getStringExtra(EXTRA_PROVIDER)
            ?.let { name -> org.tasks.compose.SignInProvider.entries.firstOrNull { it.name == name } }
            ?.oauthProvider

    private val org.tasks.compose.SignInProvider.oauthProvider: OAuthProvider
        get() = when (this) {
            org.tasks.compose.SignInProvider.GOOGLE -> OAuthProvider.GOOGLE
            org.tasks.compose.SignInProvider.APPLE -> OAuthProvider.APPLE
            org.tasks.compose.SignInProvider.GITHUB -> OAuthProvider.GITHUB
        }

    @Composable
    private fun ErrorDialogs() {
        when (val state = viewModel.state.collectAsStateWithLifecycle().value) {
            is State.WrongAccount ->
                WrongAccountDialog(
                    message = conflictingAccountMessage(state.maskedEmail),
                    onDismiss = { finish() },
                )
            is State.DifferentAccount ->
                WrongAccountDialog(
                    message = signedInAsOtherMessage(state.existingAccount),
                    onDismiss = { finish() },
                    onRetry = { retry(state.provider) },
                )
            is State.SubscriptionRequired ->
                if (!IS_GOOGLE_PLAY) {
                    SubscriptionRequiredDialog(
                        isGitHub = state.provider == OAuthProvider.GITHUB,
                        onDismiss = { finish() },
                    )
                }
            else -> {}
        }
    }

    @Composable
    private fun SubscriptionRequiredDialog(
        isGitHub: Boolean,
        onDismiss: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    stringResource(
                        if (isGitHub) R.string.github_sponsors else R.string.subscription
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (isGitHub)
                            R.string.sponsorship_required_fdroid
                        else
                            R.string.subscription_required_fdroid
                    )
                )
            },
            dismissButton = if (isGitHub) {
                {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            } else null,
            confirmButton = {
                TextButton(onClick = {
                    if (isGitHub) {
                        openUri(runBlocking { getString(Res.string.url_sponsor) })
                    }
                    onDismiss()
                }) {
                    Text(
                        stringResource(
                            if (isGitHub) R.string.github_sponsor else R.string.ok
                        )
                    )
                }
            },
        )
    }

    @Composable
    private fun WrongAccountDialog(
        message: String,
        onDismiss: () -> Unit,
        onRetry: (() -> Unit)? = null,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(kmpStringResource(Res.string.wrong_account))
            },
            text = {
                Text(message)
            },
            confirmButton = {
                if (onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Text(kmpStringResource(Res.string.sign_in))
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text(kmpStringResource(Res.string.ok))
                    }
                }
            },
            dismissButton = onRetry?.let {
                {
                    TextButton(onClick = onDismiss) {
                        Text(kmpStringResource(Res.string.cancel))
                    }
                }
            },
        )
    }

    private fun startPurchase() {
        startActivityForResult(
            Intent(this, PurchaseActivity::class.java)
                .putExtra(EXTRA_NAME_YOUR_PRICE, false)
                .putExtra(EXTRA_SOURCE, "sign_in"),
            RC_PURCHASE
        )
        viewModel.purchaseStarted()
    }

    private fun finish(outcome: Outcome) {
        when (outcome) {
            Outcome.Created -> setResult(RESULT_OK)
            Outcome.Cancelled -> returnError(getString(R.string.authorization_cancelled))
            is Outcome.Failed -> returnError(outcome.message)
            Outcome.Superseded -> {}
        }
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != RC_PURCHASE) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        if (inventory.subscription.value?.isTasksSubscription != true) {
            finish()
            return
        }
        viewModel.retryAfterPurchase()
    }

    private fun returnError(message: String?) {
        setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, message))
    }

    companion object {
        const val EXTRA_ERROR = "extra_error"
        const val EXTRA_PROVIDER = "extra_provider"
        const val EXTRA_ACCOUNT = "extra_account"
        private const val RC_PURCHASE = 101
    }
}