package org.tasks.preferences.fragments

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceCategory
import com.google.android.material.textfield.TextInputLayout
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.auth.SignInActivity
import org.tasks.billing.Inventory
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavDao
import org.tasks.jobs.WorkManager
import org.tasks.locale.Locale
import org.tasks.preferences.IconPreference
import org.tasks.preferences.fragments.MainSettingsFragment.Companion.REQUEST_TASKS_ORG
import org.tasks.ui.Toaster
import java.time.format.FormatStyle
import javax.inject.Inject

@AndroidEntryPoint
class TasksAccount : BaseAccountPreference() {

    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var toaster: Toaster
    @Inject lateinit var locale: Locale

    private val viewModel: TasksAccountViewModel by viewModels()

    private lateinit var caldavAccountLiveData: LiveData<CaldavAccount>

    val caldavAccount: CaldavAccount
        get() = caldavAccountLiveData.value ?: requireArguments().getParcelable(EXTRA_ACCOUNT)!!

    private val purchaseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lifecycleScope.launch {
                caldavAccount.let {
                    if (inventory.subscription?.isTasksSubscription == true
                            && it.isPaymentRequired()) {
                        it.error = null
                        caldavDao.update(it)
                    }
                    refreshUi(it)
                }
            }
        }
    }

    override fun getPreferenceXml() = R.xml.preferences_tasks

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        super.setupPreferences(savedInstanceState)

        caldavAccountLiveData = caldavDao.watchAccount(
                requireArguments().getParcelable<CaldavAccount>(EXTRA_ACCOUNT)!!.id
        )
        if (savedInstanceState == null) {
            viewModel.refreshPasswords(caldavAccount)
        }

        findPreference(R.string.upgrade_to_pro).setOnPreferenceClickListener {
            showPurchaseDialog(tasksPayment = true)
        }

        findPreference(R.string.button_unsubscribe).setOnPreferenceClickListener {
            inventory.unsubscribe(requireActivity())
        }

        findPreference(R.string.refresh_purchases).setOnPreferenceClickListener {
            billingClient.queryPurchases()
            false
        }

        findPreference(R.string.local_lists).setOnPreferenceClickListener {
            workManager.migrateLocalTasks(caldavAccount)
            toaster.longToast(R.string.migrating_tasks)
            false
        }

        if (isGitHubAccount) {
            findPreference(R.string.upgrade_to_pro).isVisible = false
            findPreference(R.string.button_unsubscribe).isVisible = false
            findPreference(R.string.refresh_purchases).isVisible = false
        }

        findPreference(R.string.generate_new_password).setOnPreferenceChangeListener { _, description ->
            viewModel.requestNewPassword(caldavAccount, description as String)
            false
        }
    }

    override suspend fun removeAccount() {
        // try to delete session from caldav.tasks.org
        taskDeleter.delete(caldavAccount)
        inventory.updateTasksSubscription()
    }

    override fun onResume() {
        super.onResume()
        caldavAccountLiveData.observe(this) { account ->
            account?.let { refreshUi(it) }
        }
        viewModel.appPasswords.observe(this) { passwords ->
            passwords?.let { refreshPasswords(passwords) }
        }
        viewModel.newPassword.observe(this) {
            it?.let {
                val view = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_app_password, null)
                setupTextField(view, R.id.url_layout, R.string.url, getString(R.string.tasks_caldav_url))
                setupTextField(view, R.id.user_layout, R.string.user, it.username)
                setupTextField(view, R.id.password_layout, R.string.password, it.password)
                dialogBuilder.newDialog()
                        .setView(view)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            viewModel.clearNewPassword()
                            viewModel.refreshPasswords(caldavAccount)
                        }
                        .setCancelable(false)
                        .setNeutralButton(R.string.help) { _, _ ->
                            startActivity(Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(getString(R.string.url_app_passwords)))
                            )
                        }
                        .show()
            }
        }
        localBroadcastManager.registerPurchaseReceiver(purchaseReceiver)
        localBroadcastManager.registerRefreshListReceiver(purchaseReceiver)
    }

    private fun setupTextField(v: View, layout: Int, labelRes: Int, value: String?) {
        with(v.findViewById<TextInputLayout>(layout)) {
            editText?.setText(value)
            setEndIconOnClickListener {
                val label = getString(labelRes)
                getSystemService(requireContext(), ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText(label, value))
                toaster.toast(R.string.copied_to_clipboard, label)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        localBroadcastManager.unregisterReceiver(purchaseReceiver)
    }

    private val isGitHubAccount: Boolean
        get() = caldavAccount.username?.startsWith("github") == true

    private fun refreshUi(account: CaldavAccount) {
        (findPreference(R.string.sign_in_with_google) as IconPreference).apply {
            if (account.error.isNullOrBlank()) {
                isVisible = false
                return@apply
            }
            isVisible = true
            when {
                account.isPaymentRequired() -> {
                    val subscription = inventory.subscription
                    if (isGitHubAccount) {
                        title = null
                        setSummary(R.string.insufficient_sponsorship)
                        if (BuildConfig.FLAVOR == "googleplay") {
                            onPreferenceClickListener = null
                        } else {
                            setOnPreferenceClickListener {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_sponsor))))
                                false
                            }
                        }
                    } else {
                        setOnPreferenceClickListener {
                            showPurchaseDialog(tasksPayment = true)
                        }
                        if (subscription == null || subscription.isTasksSubscription) {
                            setTitle(R.string.button_subscribe)
                            setSummary(R.string.your_subscription_expired)
                        } else {
                            setTitle(R.string.manage_subscription)
                            setSummary(R.string.insufficient_subscription)
                        }
                    }
                }
                account.isLoggedOut() -> {
                    setTitle(if (isGitHubAccount) {
                        R.string.sign_in_with_github
                    } else {
                        R.string.sign_in_with_google
                    })
                    setSummary(R.string.authentication_required)
                    setOnPreferenceClickListener {
                        activity?.startActivityForResult(
                                Intent(activity, SignInActivity::class.java)
                                        .putExtra(
                                                SignInActivity.EXTRA_SELECT_SERVICE,
                                                if (isGitHubAccount) 1 else 0
                                        ),
                                REQUEST_TASKS_ORG)
                        false
                    }
                }
                else -> {
                    this.title = null
                    this.summary = account.error
                    this.onPreferenceClickListener = null
                }
            }
            iconVisible = true
        }

        lifecycleScope.launch {
            val listCount = caldavDao.listCount(CaldavDao.LOCAL)
            val quantityString = resources.getQuantityString(R.plurals.list_count, listCount, listCount)
            findPreference(R.string.migrate).isVisible = listCount > 0
            findPreference(R.string.local_lists).summary =
                    getString(R.string.migrate_count, quantityString)
        }

        if (BuildConfig.FLAVOR == "generic") {
            return
        }
        val subscription = inventory.subscription
        findPreference(R.string.upgrade_to_pro).apply {
            title = getString(
                    if (subscription == null) {
                        R.string.button_subscribe
                    } else {
                        R.string.manage_subscription
                    }
            )
            summary = if (subscription == null) {
                null
            } else {
                val price = getString(
                        if (subscription.isMonthly) R.string.price_per_month else R.string.price_per_year,
                        (subscription.subscriptionPrice!! - .01).toString()
                )
                getString(R.string.current_subscription, price)
            }
        }
        findPreference(R.string.button_unsubscribe).isEnabled = inventory.subscription != null
    }

    private fun refreshPasswords(passwords: List<TasksAccountViewModel.AppPassword>) {
        findPreference(R.string.app_passwords_more_info).isVisible = passwords.isEmpty()
        val category = findPreference(R.string.app_passwords) as PreferenceCategory
        category.removeAll()
        passwords.forEach {
            val description = it.description ?: getString(R.string.app_password)
            category.addPreference(IconPreference(context).apply {
                layoutResource = R.layout.preference_icon
                iconVisible = true
                drawable = context?.getDrawable(R.drawable.ic_outline_delete_24px)
                tint = ContextCompat.getColor(requireContext(), R.color.icon_tint_with_alpha)
                title = description
                iconClickListener = View.OnClickListener { _ ->
                    dialogBuilder.newDialog()
                            .setTitle(R.string.delete_tag_confirmation, description)
                            .setMessage(R.string.app_password_delete_confirmation)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                viewModel.deletePassword(caldavAccount, it.id)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()

                }
                summary = """
                ${getString(R.string.app_password_created_at, formatString(it.createdAt))}
                ${getString(R.string.app_password_last_access, formatString(it.lastAccess) ?: getString(R.string.last_backup_never))}
            """.trimIndent()
            })
        }
    }

    private fun formatString(date: Long?): String? = date?.let {
        DateUtilities.getRelativeDay(
                requireContext(),
                date,
                locale.locale,
                FormatStyle.FULL,
                false,
                true
        )
    }

    companion object {
        private const val EXTRA_ACCOUNT = "extra_account"

        fun newTasksAccountPreference(account: CaldavAccount): Fragment {
            val fragment = TasksAccount()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_ACCOUNT, account)
            }
            return fragment
        }
    }
}