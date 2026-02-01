package org.tasks.compose.accounts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.auth.SignInActivity
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_FEATURE
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_NAME_YOUR_PRICE
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.data.dao.CaldavDao
import org.tasks.etebase.EtebaseAccountSettingsActivity
import org.tasks.extensions.Context.openUri
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.microsoft.MicrosoftSignInViewModel
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class AddAccountActivity : ComponentActivity() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var tasksPreferences: TasksPreferences

    private val viewModel: AddAccountViewModel by viewModels()
    private val microsoftVM: MicrosoftSignInViewModel by viewModels()

    private var pendingPlatform: Platform? = null

    private val purchaseLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingPlatform?.let { platform ->
                pendingPlatform = null
                when (platform) {
                    Platform.TASKS_ORG,
                    Platform.CALDAV,
                    Platform.ETEBASE -> doSignIn(platform)
                    Platform.DAVX5, Platform.DECSYNC_CC -> doOpenUrl(platform)
                    else -> {}
                }
            }
        } else {
            pendingPlatform = null
        }
    }

    private val syncLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            result.data
                ?.getStringExtra(GtasksLoginActivity.EXTRA_ERROR)
                ?.let { /* ignore error, user can try again */ }
        }
    }

    private fun doSignIn(platform: Platform) {
        when (platform) {
            Platform.TASKS_ORG ->
                syncLauncher.launch(Intent(this, SignInActivity::class.java))
            Platform.GOOGLE_TASKS ->
                syncLauncher.launch(Intent(this, GtasksLoginActivity::class.java))
            Platform.MICROSOFT ->
                microsoftVM.signIn(this)
            Platform.CALDAV ->
                syncLauncher.launch(Intent(this, CaldavAccountSettingsActivity::class.java))
            Platform.ETEBASE ->
                syncLauncher.launch(Intent(this, EtebaseAccountSettingsActivity::class.java))
            else -> throw IllegalArgumentException()
        }
    }

    private fun doOpenUrl(platform: Platform) {
        viewModel.openUrl(this, platform)
    }

    private fun requirePurchase(platform: Platform, nameYourPrice: Boolean = true) {
        pendingPlatform = platform
        purchaseLauncher.launch(
            Intent(this, PurchaseActivity::class.java)
                .putExtra(EXTRA_NAME_YOUR_PRICE, nameYourPrice)
                .putExtra(EXTRA_FEATURE, platform.featureTitle)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.themeBase.set(this)

        setContent {
            val accounts by caldavDao
                .watchAccounts()
                .collectAsStateWithLifecycle(initialValue = emptyList())
            var initialAccountCount by remember { mutableStateOf<Int?>(null) }
            var hasTasksAccount by remember { mutableStateOf(inventory.hasTasksAccount) }
            val currentTosVersion = firebase.getTosVersion()
            val acceptedTosVersion by tasksPreferences
                .flow(TasksPreferences.acceptedTosVersion, 0)
                .collectAsStateWithLifecycle(0)
            LaunchedEffect(Unit) {
                inventory.updateTasksAccount()
                hasTasksAccount = inventory.hasTasksAccount
                initialAccountCount = caldavDao.getAccounts().size
            }
            LaunchedEffect(accounts, initialAccountCount) {
                if (initialAccountCount != null && accounts.size > initialAccountCount!!) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            TasksTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                AddAccountScreen(
                    hasTasksAccount = hasTasksAccount,
                    hasPro = inventory.hasPro,
                    needsConsent = acceptedTosVersion < currentTosVersion,
                    onBack = { finish() },
                    signIn = { platform ->
                        firebase.logEvent(
                            R.string.event_add_account,
                            R.string.param_source to "settings",
                            R.string.param_selection to platform
                        )
                        when (platform) {
                            Platform.TASKS_ORG -> {
                                if (inventory.hasTasksSubscription) doSignIn(platform) else requirePurchase(platform, nameYourPrice = false)
                            }
                            Platform.CALDAV, Platform.ETEBASE -> {
                                if (inventory.hasPro) doSignIn(platform) else requirePurchase(platform)
                            }
                            else -> doSignIn(platform)
                        }
                    },
                    openUrl = { platform ->
                        firebase.logEvent(
                            R.string.event_add_account,
                            R.string.param_source to "settings",
                            R.string.param_selection to platform.name
                        )
                        when (platform) {
                            Platform.DAVX5, Platform.DECSYNC_CC -> {
                                if (inventory.hasPro) doOpenUrl(platform) else requirePurchase(platform)
                            }
                            else -> doOpenUrl(platform)
                        }
                    },
                    openLegalUrl = { openUri(it) },
                    onConsent = {
                        tasksPreferences.set(TasksPreferences.acceptedTosVersion, currentTosVersion)
                    },
                    onNameYourPriceInfo = {
                        firebase.logEvent(R.string.event_onboarding_name_your_price)
                    },
                )
            }
        }
    }
}
