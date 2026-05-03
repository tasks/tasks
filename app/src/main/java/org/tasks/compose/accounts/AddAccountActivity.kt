 package org.tasks.compose.accounts

import org.tasks.PlatformConfiguration
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import org.tasks.billing.PurchaseActivityViewModel.Companion.EXTRA_SOURCE
import org.tasks.caldav.CaldavSignInActivity
import org.tasks.etebase.EtebaseSignInActivity
import org.tasks.extensions.Context.openUri
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.microsoft.MicrosoftSignInViewModel
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class AddAccountActivity : ComponentActivity() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var tasksPreferences: TasksPreferences
    @Inject lateinit var configuration: PlatformConfiguration

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
        if (result.resultCode != Activity.RESULT_OK) {
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
                syncLauncher.launch(Intent(this, CaldavSignInActivity::class.java))
            Platform.ETEBASE ->
                syncLauncher.launch(Intent(this, EtebaseSignInActivity::class.java))
            else -> throw IllegalArgumentException()
        }
    }

    private fun doOpenUrl(platform: Platform) = openUrl(platform)

    private fun requirePurchase(platform: Platform, nameYourPrice: Boolean = true) {
        pendingPlatform = platform
        purchaseLauncher.launch(
            Intent(this, PurchaseActivity::class.java)
                .putExtra(EXTRA_NAME_YOUR_PRICE, nameYourPrice)
                .putExtra(EXTRA_FEATURE, platform.featureTitle)
                .putExtra(EXTRA_SOURCE, platform.name)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentTosVersion = firebase.getTosVersion()
            val acceptedTosVersion by tasksPreferences
                .flow(TasksPreferences.acceptedTosVersion, 0)
                .collectAsStateWithLifecycle(0)
            LaunchedEffect(Unit) {
                viewModel.accountAdded.collect {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            TasksSettingsTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                AddAccountScreenWrapper(
                    configuration = configuration,
                    hasTasksAccount = viewModel.hasTasksAccount,
                    hasPro = viewModel.hasPro,
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
