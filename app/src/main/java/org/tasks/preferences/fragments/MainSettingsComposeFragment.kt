package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tasks.R
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.tasks_org
import org.tasks.auth.SignInActivity
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_FEATURE
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_NAME_YOUR_PRICE
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_SHOW_MORE_OPTIONS
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_SOURCE
import org.tasks.compose.accounts.AddAccountActivity
import org.tasks.compose.settings.AndroidMainSettingsScreen
import org.tasks.compose.settings.ManageSubscriptionSheetContent
import org.tasks.compose.settings.ProCardState
import org.tasks.compose.settings.SettingsDestination
import org.tasks.themes.TasksSettingsTheme
import org.tasks.data.entity.CaldavAccount
import org.tasks.extensions.Context.openUri
import android.view.View
import org.tasks.preferences.BasePreferences
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.MainSettingsViewModel
import org.tasks.preferences.Preferences
import org.tasks.preferences.PreferencesViewModel
import org.tasks.preferences.ProCardViewModel
import org.tasks.preferences.fragments.GoogleTasksAccount.Companion.newGoogleTasksAccountPreference
import org.tasks.preferences.fragments.CaldavAccountFragment.Companion.newCaldavAccountFragment
import org.tasks.preferences.fragments.EtebaseAccountFragment.Companion.newEtebaseAccountFragment
import org.tasks.preferences.fragments.LocalAccount.Companion.newLocalAccountPreference
import org.tasks.preferences.fragments.OpenTaskAccountFragment.Companion.newOpenTaskAccountFragment
import org.tasks.preferences.fragments.MicrosoftAccount.Companion.newMicrosoftAccountPreference
import org.tasks.preferences.fragments.TasksAccount.Companion.newTasksAccountPreference
import org.tasks.PlatformConfiguration
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.billing.LinkDesktopActivity
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class MainSettingsComposeFragment : Fragment() {

    @Inject lateinit var firebase: Firebase
    @Inject lateinit var theme: Theme
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var configuration: PlatformConfiguration
    @Inject lateinit var inventory: Inventory

    private val viewModel: MainSettingsViewModel by viewModels()
    private val proCardViewModel: ProCardViewModel by viewModels()
    private val preferencesViewModel: PreferencesViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            val filteredAccounts = proCardViewModel.filteredAccounts.collectAsStateWithLifecycle().value
            val proCardState = proCardViewModel.proCardState.collectAsStateWithLifecycle().value
            val environmentLabel = proCardViewModel.environmentLabel.collectAsStateWithLifecycle().value
            var showManageSheet by remember { mutableStateOf(false) }

            // Calculate backup warning from activity's PreferencesViewModel
            val showBackupWarning = preferences.showBackupWarnings() &&
                    (preferencesViewModel.usingPrivateStorage ||
                            preferencesViewModel.staleLocalBackup ||
                            preferencesViewModel.staleRemoteBackup)

            AndroidMainSettingsScreen(
                accounts = filteredAccounts,
                proCardState = proCardState,
                environmentLabel = environmentLabel,
                showBackupWarning = showBackupWarning,
                showWidgets = viewModel.supportsWidgets,
                onAccountClick = { account -> handleAccountClick(account) },
                onAddAccountClick = { addAccount() },
                onSettingsClick = { destination -> navigateToSettings(destination) },
                showDesktopLinking = configuration.supportsDesktopLinking && !inventory.hasTasksAccount,
                onLinkDesktopClick = { linkDesktop() },
                onProCardClick = {
                    val type = when (proCardState) {
                        is ProCardState.Subscribed -> "manage"
                        is ProCardState.Upgrade -> "subscribe"
                        is ProCardState.SignIn -> "sign_in"
                        is ProCardState.TasksOrgAccount -> "tasks_org"
                        is ProCardState.Donate -> "donate"
                    }
                    firebase.logEvent(
                        R.string.event_pro_card,
                        R.string.param_type to type,
                    )
                    if (proCardState is ProCardState.Subscribed) {
                        showManageSheet = true
                    } else {
                        handleProCardClick(proCardState)
                    }
                },
            )

            if (showManageSheet) {
                ManageSubscriptionSheet(
                    onDismiss = { showManageSheet = false },
                    onUpgrade = {
                        showManageSheet = false
                        firebase.logEvent(
                            R.string.event_pro_card,
                            R.string.param_type to "sheet_upgrade",
                        )
                        startActivity(
                            Intent(context, PurchaseActivity::class.java)
                                .putExtra(EXTRA_NAME_YOUR_PRICE, false)
                                .putExtra(EXTRA_SHOW_MORE_OPTIONS, false)
                                .putExtra(EXTRA_SOURCE, "settings")
                        )
                    },
                    onModify = {
                        showManageSheet = false
                        firebase.logEvent(
                            R.string.event_pro_card,
                            R.string.param_type to "sheet_modify",
                        )
                        startActivity(
                            Intent(context, PurchaseActivity::class.java)
                                .putExtra(EXTRA_NAME_YOUR_PRICE, true)
                                .putExtra(EXTRA_SHOW_MORE_OPTIONS, true)
                                .putExtra(EXTRA_SOURCE, "settings")
                        )
                    },
                    onCancel = {
                        showManageSheet = false
                        firebase.logEvent(
                            R.string.event_pro_card,
                            R.string.param_type to "sheet_cancel",
                        )
                        viewModel.unsubscribe(requireContext())
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferencesViewModel.updateBackups()
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.content_background)
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? View)?.setBackgroundColor(defaultColor)
        }
    }

    private fun handleProCardClick(state: ProCardState) {
        when (state) {
            is ProCardState.TasksOrgAccount -> handleAccountClick(state.account)
            is ProCardState.SignIn -> {
                startActivity(Intent(context, SignInActivity::class.java))
            }
            is ProCardState.Donate -> {
                requireContext().openUri(R.string.url_donate)
            }
            is ProCardState.Upgrade -> {
                startActivity(
                    Intent(context, PurchaseActivity::class.java)
                        .putExtra(EXTRA_NAME_YOUR_PRICE, false)
                        .putExtra(EXTRA_SHOW_MORE_OPTIONS, true)
                        .putExtra(EXTRA_FEATURE, R.string.tasks_org_account)
                        .putExtra(EXTRA_SOURCE, "settings")
                )
            }
            is ProCardState.Subscribed -> {
                // Handled by bottom sheet in onProCardClick
            }
        }
    }

    private fun handleAccountClick(account: CaldavAccount) {
        val activity = activity as? MainPreferences ?: return
        when {
            account.isTasksOrg -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    activity.startPreference(
                        newTasksAccountPreference(account),
                        org.jetbrains.compose.resources.getString(Res.string.tasks_org)
                    )
                }
            }
            account.isMicrosoft -> {
                activity.startPreference(
                    newMicrosoftAccountPreference(account),
                    getString(R.string.microsoft)
                )
            }
            account.isGoogleTasks -> {
                activity.startPreference(
                    newGoogleTasksAccountPreference(account),
                    getString(R.string.gtasks_GPr_header)
                )
            }
            account.isLocalList -> {
                activity.startPreference(
                    newLocalAccountPreference(account),
                    getString(R.string.local_lists)
                )
            }
            account.isCaldavAccount -> {
                activity.startPreference(
                    newCaldavAccountFragment(account),
                    getString(R.string.caldav)
                )
            }
            account.isEtebaseAccount -> {
                activity.startPreference(
                    newEtebaseAccountFragment(account),
                    getString(R.string.etesync)
                )
            }
            account.isOpenTasks -> {
                activity.startPreference(
                    newOpenTaskAccountFragment(account),
                    account.name ?: ""
                )
            }
            else -> {}
        }
    }

    private fun addAccount() {
        startActivityForResult(
            Intent(requireContext(), AddAccountActivity::class.java),
            REQUEST_ADD_ACCOUNT
        )
    }

    private fun linkDesktop() {
        if (inventory.hasPro) {
            startActivity(Intent(requireContext(), LinkDesktopActivity::class.java))
        } else {
            startActivity(
                Intent(requireContext(), PurchaseActivity::class.java)
                    .putExtra(EXTRA_NAME_YOUR_PRICE, true)
                    .putExtra(EXTRA_SHOW_MORE_OPTIONS, true)
                    .putExtra(EXTRA_SOURCE, "link_desktop")
            )
        }
    }

    private fun navigateToSettings(destination: SettingsDestination) {
        val activity = activity as? MainPreferences ?: return
        val fragment: Fragment = when (destination) {
            SettingsDestination.LookAndFeel -> LookAndFeel()
            SettingsDestination.Notifications -> Notifications()
            SettingsDestination.TaskDefaults -> TaskDefaults()
            SettingsDestination.TaskList -> TaskListPreferences()
            SettingsDestination.TaskEdit -> TaskEditPreferences()
            SettingsDestination.DateAndTime -> DateAndTime()
            SettingsDestination.NavigationDrawer -> NavigationDrawer()
            SettingsDestination.Backups -> Backups()
            SettingsDestination.Widgets -> Widgets()
            SettingsDestination.Advanced -> Advanced()
            SettingsDestination.HelpAndFeedback -> HelpAndFeedback()
            SettingsDestination.Debug -> Debug()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            activity.startPreference(
                fragment,
                org.jetbrains.compose.resources.getString(destination.titleRes)
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ManageSubscriptionSheet(
        onDismiss: () -> Unit,
        onUpgrade: () -> Unit,
        onModify: () -> Unit,
        onCancel: () -> Unit,
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(),
        ) {
            ManageSubscriptionSheetContent(
                onUpgrade = onUpgrade,
                onModify = onModify,
                onCancel = onCancel,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }

    companion object {
        const val REQUEST_CALDAV_SETTINGS = 10013
        const val REQUEST_GOOGLE_TASKS = 10014
        const val REQUEST_TASKS_ORG = 10016
        private const val REQUEST_ADD_ACCOUNT = 10017
    }
}
