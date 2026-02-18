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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.auth.SignInActivity
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_NAME_YOUR_PRICE
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_SHOW_MORE_OPTIONS
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_SOURCE
import org.tasks.compose.accounts.AddAccountActivity
import org.tasks.compose.settings.CalendarItem
import org.tasks.compose.settings.TasksAccountScreen
import org.tasks.themes.TasksSettingsTheme
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.isPaymentRequired
import org.tasks.data.entity.CaldavAccount.Companion.isTosRequired
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.Context.toast
import org.tasks.fcm.PushTokenManager
import org.tasks.jobs.WorkManager
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.fragments.MainSettingsComposeFragment.Companion.REQUEST_TASKS_ORG
import org.tasks.sync.SyncSource
import org.tasks.themes.Theme
import org.tasks.utility.copyToClipboard
import javax.inject.Inject

@AndroidEntryPoint
class TasksAccount : Fragment() {

    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var tasksPreferences: TasksPreferences
    @Inject lateinit var pushTokenManager: PushTokenManager
    @Inject lateinit var billingClient: BillingClient
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var theme: Theme

    private val viewModel: TasksAccountViewModel by viewModels()

    private val initialAccount: CaldavAccount
        get() = requireArguments().getParcelable(EXTRA_ACCOUNT)!!

    private val isGithub: Boolean
        get() = initialAccount.username?.startsWith("github") == true

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
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            val account by caldavDao.watchAccount(initialAccount.id)
                .collectAsStateWithLifecycle(initialValue = initialAccount)
            val subscription by inventory.subscription.observeAsState()
            val newPassword by viewModel.newPassword.collectAsStateWithLifecycle()
            val appPasswords by viewModel.appPasswords.collectAsStateWithLifecycle()
            val inboundEmail by viewModel.inboundEmail.collectAsStateWithLifecycle()
            val inboundCalendarUri by viewModel.inboundCalendar.collectAsStateWithLifecycle()

            // Derive calendar name reactively
            val calendarName by remember {
                caldavDao.subscribeToCalendars()
                    .map { calendars ->
                        calendars.filter { it.account == initialAccount.uuid }
                    }
                    .combine(viewModel.inboundCalendar) { calendars, calendarUri ->
                        calendars.find { it.calendarUri == calendarUri }?.name
                    }
            }.collectAsStateWithLifecycle(initialValue = null)

            // Local list count
            var localListCount by remember { mutableStateOf(0) }
            var localListSummary by remember { mutableStateOf("") }

            // Trigger re-reads on refresh events
            val trigger by refreshTrigger

            LaunchedEffect(trigger) {
                viewModel.refreshAccount(initialAccount)
                val localAccount = caldavDao.getAccounts(CaldavAccount.TYPE_LOCAL).firstOrNull()
                val count = localAccount?.uuid?.let { caldavDao.listCount(it) } ?: 0
                localListCount = count
                val quantityString = resources.getQuantityString(
                    R.plurals.list_count, count, count
                )
                localListSummary = getString(R.string.migrate_count, quantityString)
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

            // TOS dialog
            var showTosDialog by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(account?.error) {
                if (account?.isTosRequired() == true && !showTosDialog) {
                    showTosDialog = true
                }
            }

            // Calendar items for the dialog
            val calendars by remember {
                caldavDao.subscribeToCalendars()
                    .map { cals ->
                        cals.filter { it.account == initialAccount.uuid && !it.readOnly() }
                            .map { CalendarItem(it.name ?: it.uuid ?: "", it.calendarUri) }
                    }
            }.collectAsStateWithLifecycle(initialValue = emptyList())

            TasksAccountScreen(
                account = account,
                isGithub = isGithub,
                hasSubscription = subscription != null,
                isTasksSubscription = subscription?.isTasksSubscription == true,
                localListCount = localListCount,
                localListSummary = localListSummary,
                inboundEmail = inboundEmail,
                inboundCalendarName = calendarName,
                appPasswords = appPasswords,
                newPassword = newPassword,
                calendars = calendars,
                inboundCalendarUri = inboundCalendarUri,
                showTosDialog = showTosDialog,
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
                    context?.openUri(R.string.url_sponsor)
                },
                onMigrate = {
                    val currentAccount = account ?: return@TasksAccountScreen
                    workManager.migrateLocalTasks(currentAccount)
                    context?.toast(R.string.migrating_tasks)
                },
                onCopyEmail = {
                    inboundEmail?.let {
                        copyToClipboard(requireContext(), R.string.email_to_task_address, it)
                        firebase.logEvent(
                            R.string.event_settings_click,
                            R.string.param_type to "email_to_task_copy",
                        )
                    }
                },
                onRegenerateEmail = {
                    val currentAccount = account ?: return@TasksAccountScreen
                    viewModel.regenerateInboundEmail(currentAccount)
                },
                onSelectCalendar = { calendarUri ->
                    val currentAccount = account ?: return@TasksAccountScreen
                    viewModel.setInboundCalendar(currentAccount, calendarUri)
                },
                onDeletePassword = { id, _ ->
                    val currentAccount = account ?: return@TasksAccountScreen
                    viewModel.deletePassword(currentAccount, id)
                },
                onGeneratePassword = { description ->
                    val currentAccount = account ?: return@TasksAccountScreen
                    viewModel.requestNewPassword(currentAccount, description)
                },
                onOpenAppPasswordsInfo = {
                    context?.openUri(R.string.url_app_passwords)
                },
                onCopyField = { labelRes, value ->
                    copyToClipboard(requireContext(), labelRes, value)
                },
                onClearNewPassword = {
                    viewModel.clearNewPassword()
                },
                onRefreshPasswords = {
                    val currentAccount = account ?: return@TasksAccountScreen
                    viewModel.refreshAccount(currentAccount)
                },
                onOpenHelp = {
                    context?.openUri(R.string.url_app_passwords)
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
                            val acct = account ?: initialAccount
                            pushTokenManager.unregisterToken(acct)
                            taskDeleter.delete(acct)
                            inventory.updateTasksAccount()
                        }
                        activity?.onBackPressed()
                    }
                },
                onAcceptTos = {
                    showTosDialog = false
                    lifecycleScope.launch {
                        val currentTosVersion = firebase.getTosVersion()
                        tasksPreferences.set(
                            TasksPreferences.acceptedTosVersion,
                            currentTosVersion,
                        )
                        account?.let {
                            caldavDao.update(it.copy(error = null))
                        }
                        workManager.sync(SyncSource.ACCOUNT_ADDED)
                    }
                },
                onViewTos = {
                    context?.openUri(R.string.url_tos)
                },
                onDismissTos = {
                    showTosDialog = false
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)
        viewModel.refreshAccount(initialAccount)
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
