package org.tasks.preferences.fragments

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.LocalBroadcastManager
import org.tasks.R
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.url_app_passwords
import tasks.kmp.generated.resources.url_sponsor
import tasks.kmp.generated.resources.url_tos
import org.tasks.analytics.Firebase
import org.tasks.auth.SignInActivity
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_NAME_YOUR_PRICE
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_SHOW_MORE_OPTIONS
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_SOURCE
import org.tasks.compose.accounts.AddAccountActivity
import org.tasks.compose.settings.TasksAccountScreen
import org.tasks.themes.TasksSettingsTheme
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.isPaymentRequired
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.Context.toast
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.jobs.WorkManager
import org.tasks.preferences.fragments.MainSettingsComposeFragment.Companion.REQUEST_TASKS_ORG
import org.tasks.themes.Theme
import org.tasks.utility.copyToClipboard
import javax.inject.Inject

@AndroidEntryPoint
class TasksAccount : Fragment() {

    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var theme: Theme
    @Inject lateinit var accountDataRepository: TasksAccountDataRepository

    private val viewModel: TasksAccountViewModel by viewModels()

    private val initialAccount: CaldavAccount
        get() = requireArguments().getParcelable(EXTRA_ACCOUNT)!!

    private var refreshTrigger = mutableIntStateOf(0)

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshTrigger.intValue++
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        viewModel.setAccountUuid(initialAccount.uuid!!)
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val account = state.account
            val subscription by inventory.subscription.observeAsState()

            // Trigger re-reads on refresh events
            val trigger by refreshTrigger

            LaunchedEffect(trigger) {
                viewModel.refreshAccount()
            }

            // Clear payment error when subscription arrives
            LaunchedEffect(subscription) {
                val purchase = subscription
                if (purchase?.isTasksSubscription == true &&
                    account?.error.isPaymentRequired()
                ) {
                    account?.let { caldavDao.update(it.copy(error = null)) }
                }
            }

            TasksAccountScreen(
                state = state,
                onSignIn = {
                    activity?.startActivityForResult(
                        Intent(activity, SignInActivity::class.java),
                        REQUEST_TASKS_ORG,
                    )
                },
                onSubscribe = {
                    startActivityForResult(
                        Intent(context, PurchaseActivity::class.java)
                            .putExtra(EXTRA_NAME_YOUR_PRICE, false)
                            .putExtra(EXTRA_SOURCE, "account_settings"),
                        REQUEST_PURCHASE,
                    )
                },
                onOpenSponsor = {
                    context?.openUri(runBlocking { org.jetbrains.compose.resources.getString(Res.string.url_sponsor) })
                },
                onMigrate = {
                    val currentAccount = account ?: return@TasksAccountScreen
                    workManager.migrateLocalTasks(currentAccount)
                    context?.toast(R.string.migrating_tasks)
                },
                onCopyEmail = {
                    state.inboundEmail?.let {
                        copyToClipboard(requireContext(), R.string.email_to_task_address, it)
                        firebase.logEvent(
                            R.string.event_settings_click,
                            R.string.param_type to "email_to_task_copy",
                        )
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
                    context?.openUri(runBlocking { org.jetbrains.compose.resources.getString(Res.string.url_app_passwords) })
                },
                onCopyField = { label, value ->
                    copyToClipboard(requireContext(), label, value)
                },
                onClearNewPassword = {
                    viewModel.clearNewPassword()
                },
                onRefreshPasswords = {
                    viewModel.refreshAccount()
                },
                onOpenHelp = {
                    context?.openUri(runBlocking { org.jetbrains.compose.resources.getString(Res.string.url_app_passwords) })
                },
                onAddAccount = {
                    startActivity(Intent(requireContext(), AddAccountActivity::class.java))
                },
                onModifySubscription = {
                    startActivity(
                        Intent(context, PurchaseActivity::class.java)
                            .putExtra(EXTRA_NAME_YOUR_PRICE, false)
                            .putExtra(EXTRA_SHOW_MORE_OPTIONS, true)
                            .putExtra(EXTRA_SOURCE, "account_settings"),
                    )
                },
                onCancelSubscription = {
                    inventory.unsubscribe(requireContext())
                },
                onLogout = {
                    lifecycleScope.launch {
                        withContext(NonCancellable) {
                            viewModel.logout(account ?: initialAccount)
                            inventory.updateTasksAccount()
                        }
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    }
                },
                onAcceptTos = {
                    viewModel.acceptTos(firebase.getTosVersion())
                },
                onViewTos = {
                    context?.openUri(runBlocking { org.jetbrains.compose.resources.getString(Res.string.url_tos) })
                },
                onDismissTos = {
                    viewModel.dismissTos()
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)
        viewModel.refreshAccount()
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? org.tasks.preferences.BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? android.view.View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onPause() {
        super.onPause()
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.content_background)
        (activity as? org.tasks.preferences.BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? android.view.View)?.setBackgroundColor(defaultColor)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PURCHASE) {
            if (resultCode == Activity.RESULT_OK) {
                lifecycleScope.launch {
                    billingClient.queryPurchases()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT = "extra_account"
        private const val REQUEST_PURCHASE = 10201

        fun newTasksAccountPreference(account: CaldavAccount): Fragment {
            val fragment = TasksAccount()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_ACCOUNT, account)
            }
            return fragment
        }
    }
}
