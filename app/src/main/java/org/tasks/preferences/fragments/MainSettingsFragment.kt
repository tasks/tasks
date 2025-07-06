package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.auth.SignInActivity
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.billing.Purchase
import org.tasks.billing.PurchaseActivity
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.data.accountSettingsClass
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.prefIcon
import org.tasks.data.prefTitle
import org.tasks.etebase.EtebaseAccountSettingsActivity
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.Context.toast
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.IconPreference
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import org.tasks.preferences.PreferencesViewModel
import org.tasks.preferences.fragments.GoogleTasksAccount.Companion.newGoogleTasksAccountPreference
import org.tasks.preferences.fragments.MicrosoftAccount.Companion.newMicrosoftAccountPreference
import org.tasks.preferences.fragments.TasksAccount.Companion.newTasksAccountPreference
import org.tasks.sync.AddAccountDialog
import org.tasks.sync.AddAccountDialog.Companion.EXTRA_SELECTED
import org.tasks.sync.AddAccountDialog.Companion.newAccountDialog
import org.tasks.sync.AddAccountDialog.Platform
import org.tasks.sync.microsoft.MicrosoftSignInViewModel
import org.tasks.widget.AppWidgetManager
import javax.inject.Inject

@AndroidEntryPoint
class MainSettingsFragment : InjectingPreferenceFragment() {

    @Inject lateinit var appWidgetManager: AppWidgetManager
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var billingClient: BillingClient

    private val viewModel: PreferencesViewModel by activityViewModels()
    private val microsoftVM: MicrosoftSignInViewModel by viewModels()

    override fun getPreferenceXml() = R.xml.preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findPreference(R.string.add_account).setOnPreferenceClickListener { addAccount() }

        findPreference(R.string.name_your_price).setOnPreferenceClickListener {
            startActivity(Intent(context, PurchaseActivity::class.java))
            false
        }

        findPreference(R.string.button_unsubscribe).setOnPreferenceClickListener {
            inventory.unsubscribe(requireActivity())
        }

        findPreference(R.string.refresh_purchases).setOnPreferenceClickListener {
            lifecycleScope.launch {
                try {
                    billingClient.queryPurchases(throwError = true)
                    if (inventory.subscription.value == null) {
                        activity?.toast(R.string.no_google_play_subscription)
                    }
                } catch (e: Exception) {
                    activity?.toast(e.message)
                }
            }
            false
        }

        viewModel.lastBackup.observe(this) { updateBackupWarning() }
        viewModel.lastAndroidBackup.observe(this) { updateBackupWarning() }
        viewModel.lastDriveBackup.observe(this) { updateBackupWarning() }
        viewModel
            .caldavAccounts
            .onEach { refreshAccounts(it) }
            .launchIn(lifecycleScope)

        if (BuildConfig.FLAVOR == "generic") {
            remove(R.string.upgrade_to_pro)
        } else {
            inventory.subscription.observe(this) { refreshSubscription(it) }
        }

        parentFragmentManager.setFragmentResultListener(
            AddAccountDialog.ADD_ACCOUNT,
            this
        ) { _, result ->
            val platform =
                result.getSerializable(EXTRA_SELECTED) as? Platform
                    ?: return@setFragmentResultListener
            when (platform) {
                Platform.TASKS_ORG ->
                    startActivityForResult(
                        Intent(requireContext(), SignInActivity::class.java),
                        REQUEST_TASKS_ORG
                    )
                Platform.GOOGLE_TASKS ->
                    startActivityForResult(
                        Intent(requireContext(), GtasksLoginActivity::class.java),
                        REQUEST_GOOGLE_TASKS
                    )
                Platform.TODOIST ->
                    context?.openUri(R.string.url_todoist)
                Platform.MICROSOFT ->
                    microsoftVM.signIn(requireActivity())
                Platform.DAVX5 ->
                    context?.openUri(R.string.url_davx5)
                Platform.CALDAV ->
                    startActivityForResult(
                        Intent(requireContext(), CaldavAccountSettingsActivity::class.java),
                        REQUEST_CALDAV_SETTINGS
                    )
                Platform.ETESYNC ->
                    startActivityForResult(
                        Intent(requireContext(), EtebaseAccountSettingsActivity::class.java),
                        REQUEST_CALDAV_SETTINGS
                    )
                Platform.DECSYNC_CC ->
                    context?.openUri(R.string.url_decsync)

                Platform.LOCAL -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()

        updateBackupWarning()
        updateQuietHoursWarning()
        updateWidgetVisibility()
    }

    private fun refreshAccounts(caldavAccounts: List<CaldavAccount>) {
        val addAccount = findPreference(R.string.add_account)
        val index = preferenceScreen.indexOf(addAccount)
        var current = 0
        caldavAccounts.forEach {
            setup(it, if (current < index) {
                preferenceScreen.getPreference(current++) as IconPreference
            } else {
                preferenceScreen.insertAt(current++)
            })
        }
        preferenceScreen.removeAt(current, index - current)
        tintIcons(addAccount, requireContext().getColor(R.color.icon_tint_with_alpha))
    }

    private fun addAccount(): Boolean {
        lifecycleScope.launch {
            newAccountDialog(
                hasTasksAccount = viewModel.tasksAccount() != null,
                hasPro = inventory.hasPro,
            )
                .show(parentFragmentManager, FRAG_TAG_ADD_ACCOUNT)
        }
        return false
    }

    private fun updateWidgetVisibility() {
        findPreference(R.string.widget_settings).isVisible = appWidgetManager.widgetIds.isNotEmpty()
    }

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        requires(BuildConfig.DEBUG, R.string.debug)

        updateWidgetVisibility()
    }

    private fun updateBackupWarning() {
        val backupWarning =
                preferences.showBackupWarnings()
                        && (viewModel.usingPrivateStorage
                        || viewModel.staleLocalBackup
                        || viewModel.staleRemoteBackup)
        (findPreference(R.string.backup_BPr_header) as IconPreference).iconVisible = backupWarning
    }

    private fun updateQuietHoursWarning() {
        val pref = findPreference(R.string.notifications) as IconPreference
        setupErrorIcon(pref, hasError = false, hasWarning = preferences.isCurrentlyQuietHours)
        pref.iconVisible = preferences.isCurrentlyQuietHours
    }

    private fun setup(account: CaldavAccount, pref: IconPreference) {
        pref.setTitle(account.prefTitle)
        pref.summary = account.name
        pref.setIcon(account.prefIcon)
        if (account.isCaldavAccount || account.isLocalList) {
            tintIcons(pref, requireContext().getColor(R.color.icon_tint_with_alpha))
        }
        pref.setOnPreferenceClickListener {
            if (account.isTasksOrg) {
                (activity as MainPreferences).startPreference(
                    this,
                    newTasksAccountPreference(account),
                    getString(R.string.tasks_org)
                )
            } else if (account.isMicrosoft) {
                (activity as MainPreferences).startPreference(
                    this,
                    newMicrosoftAccountPreference(account),
                    getString(R.string.microsoft)
                )
            } else if (account.isGoogleTasks) {
                (activity as MainPreferences).startPreference(
                    this,
                    newGoogleTasksAccountPreference(account),
                    getString(R.string.gtasks_GPr_header)
                )
            } else {
                val intent = Intent(context, account.accountSettingsClass).apply {
                    putExtra(BaseCaldavAccountSettingsActivity.EXTRA_CALDAV_DATA, account)
                }
                startActivityForResult(intent, REQUEST_CALDAV_SETTINGS)
            }
            false
        }
        when {
            account.isTasksOrg -> {
                pref.setOnPreferenceClickListener {
                    (activity as MainPreferences).startPreference(
                            this,
                            newTasksAccountPreference(account),
                            getString(R.string.tasks_org)
                    )
                    false
                }
            }
        }
        setupErrorIcon(pref, account.hasError)
    }

    private fun setupErrorIcon(
            pref: IconPreference,
            hasError: Boolean,
            hasWarning: Boolean = false
    ) {
        pref.drawable = ContextCompat
                .getDrawable(requireContext(), when {
                    hasError -> R.drawable.ic_outline_error_outline_24px
                    hasWarning -> R.drawable.ic_outline_error_outline_24px
                    else -> R.drawable.ic_keyboard_arrow_right_24px
                })
                ?.mutate()
        pref.tint = context?.getColor(when {
            hasError -> R.color.overdue
            hasWarning -> org.tasks.kmp.R.color.orange_500
            else -> R.color.icon_tint_with_alpha
        })
    }

    private fun refreshSubscription(subscription: Purchase?) {
        findPreference(R.string.upgrade_to_pro).setTitle(if (subscription == null) {
            R.string.upgrade_to_pro
        } else {
            R.string.subscription
        })
        findPreference(R.string.name_your_price).apply {
            if (subscription == null) {
                title = getString(R.string.name_your_price)
                summary = null
            } else {
                val interval = if (subscription.isMonthly) {
                    R.string.price_per_month
                } else {
                    R.string.price_per_year
                }
                val price = (subscription.subscriptionPrice!! - .01).toString()
                title = getString(R.string.manage_subscription)
                summary = getString(R.string.current_subscription, getString(interval, price))
            }
        }
        findPreference(R.string.button_unsubscribe).isVisible = subscription != null
    }

    companion object {
        private const val FRAG_TAG_ADD_ACCOUNT = "frag_tag_add_account"
        const val REQUEST_CALDAV_SETTINGS = 10013
        const val REQUEST_GOOGLE_TASKS = 10014
        const val REQUEST_TASKS_ORG = 10016

        fun PreferenceScreen.removeAt(index: Int, count: Int = 1) {
            repeat(count) {
                removePreference(getPreference(index))
            }
        }

        fun PreferenceScreen.indexOf(pref: Preference): Int =
                0.until(preferenceCount).first { pref == getPreference(it) }

        fun PreferenceScreen.insertAt(index: Int): IconPreference {
            index.until(preferenceCount).forEach {
                getPreference(it).apply { order += 1 }
            }
            return IconPreference(context).apply {
                layoutResource = R.layout.preference_icon
                order = index
                iconVisible = true
                addPreference(this)
            }
        }
    }
}
