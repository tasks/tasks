package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.billing.Purchase
import org.tasks.billing.PurchaseDialog
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.data.CaldavAccount
import org.tasks.data.GoogleTaskAccount
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.IconPreference
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import org.tasks.preferences.PreferencesViewModel
import org.tasks.preferences.fragments.GoogleTasksAccount.Companion.newGoogleTasksAccountPreference
import org.tasks.preferences.fragments.TasksAccount.Companion.newTasksAccountPreference
import org.tasks.sync.AddAccountDialog.Companion.newAccountDialog
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

    override fun getPreferenceXml() = R.xml.preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findPreference(R.string.add_account).setOnPreferenceClickListener { addAccount() }

        findPreference(R.string.name_your_price).setOnPreferenceClickListener {
            PurchaseDialog
                    .newPurchaseDialog()
                    .show(parentFragmentManager, PurchaseDialog.FRAG_TAG_PURCHASE_DIALOG)
            false
        }

        findPreference(R.string.button_unsubscribe).setOnPreferenceClickListener {
            inventory.unsubscribe(requireActivity())
        }

        findPreference(R.string.refresh_purchases).setOnPreferenceClickListener {
            billingClient.queryPurchases()
            false
        }

        viewModel.lastBackup.observe(this) { updateBackupWarning() }
        viewModel.lastAndroidBackup.observe(this) { updateBackupWarning() }
        viewModel.lastDriveBackup.observe(this) { updateBackupWarning() }
        viewModel.googleTaskAccounts.observe(this) { refreshAccounts() }
        viewModel.caldavAccounts.observe(this) { refreshAccounts() }
        if (BuildConfig.FLAVOR == "generic") {
            remove(R.string.upgrade_to_pro)
        } else {
            inventory.subscription.observe(this) { refreshSubscription(it) }
        }
    }

    override fun onResume() {
        super.onResume()

        updateBackupWarning()
        updateWidgetVisibility()
    }

    private fun refreshAccounts() {
        val caldavAccounts = viewModel.caldavAccounts.value ?: emptyList()
        val googleTaskAccounts = viewModel.googleTaskAccounts.value ?: emptyList()
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
        googleTaskAccounts.forEach {
            setup(it, if (current < index) {
                preferenceScreen.getPreference(current++) as IconPreference
            } else {
                preferenceScreen.insertAt(current++)
            })
        }
        preferenceScreen.removeAt(current, index - current)
        if (caldavAccounts.isEmpty() && googleTaskAccounts.isEmpty()) {
            addAccount.setTitle(R.string.not_signed_in)
            addAccount.setIcon(R.drawable.ic_outline_cloud_off_24px)
        } else {
            addAccount.setTitle(R.string.add_account)
            addAccount.setIcon(R.drawable.ic_outline_add_24px)
        }
        tintIcons(addAccount, requireContext().getColor(R.color.icon_tint_with_alpha))
    }

    private fun addAccount(): Boolean {
        val hasTasksAccount = viewModel.tasksAccount() != null
        newAccountDialog(this@MainSettingsFragment, REQUEST_ADD_ACCOUNT, hasTasksAccount)
                .show(parentFragmentManager, FRAG_TAG_ADD_ACCOUNT)
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

    private fun setup(account: CaldavAccount, pref: IconPreference) {
        pref.setTitle(account.prefTitle)
        pref.summary = account.name
        pref.setIcon(account.prefIcon)
        if (account.isCaldavAccount) {
            tintIcons(pref, requireContext().getColor(R.color.icon_tint_with_alpha))
        }
        pref.setOnPreferenceClickListener {
            if (account.isTasksOrg) {
                (activity as MainPreferences).startPreference(
                        this,
                        newTasksAccountPreference(account),
                        getString(R.string.tasks_org)
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
        setupErrorIcon(pref, account.error)
    }

    private fun setup(account: GoogleTaskAccount, pref: IconPreference) {
        pref.setTitle(R.string.gtasks_GPr_header)
        pref.setIcon(R.drawable.ic_google)
        pref.summary = account.account
        setupErrorIcon(pref, account.error)
        pref.setOnPreferenceClickListener {
            (activity as MainPreferences).startPreference(
                    this,
                    newGoogleTasksAccountPreference(account),
                    account.account!!
            )
            false
        }
    }

    private fun setupErrorIcon(pref: IconPreference, error: String?) {
        val hasError = !error.isNullOrBlank()
        pref.drawable = ContextCompat
                .getDrawable(requireContext(), if (hasError) {
                    R.drawable.ic_outline_error_outline_24px
                } else {
                    R.drawable.ic_keyboard_arrow_right_24px
                })
                ?.mutate()
        pref.tint = context?.getColor(if (hasError) {
            R.color.overdue
        } else {
            R.color.icon_tint_with_alpha
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
        private const val REQUEST_ADD_ACCOUNT = 10015
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
