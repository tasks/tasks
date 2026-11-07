package org.tasks.billing

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.PlatformConfiguration
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.analytics.logCloudOnboarding
import org.tasks.auth.SignInActivity
import org.tasks.data.dao.CaldavDao
import org.tasks.preferences.TasksPreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class PurchaseActivity : AppCompatActivity() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var tasksPreferences: TasksPreferences
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var configuration: PlatformConfiguration
    @Inject lateinit var stripePurchases: StripePurchases
    @Inject lateinit var subscriptionProvider: SubscriptionProvider
    @JvmField @Inject var storeBillingProvider: StoreBillingProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ),
            navigationBarStyle = if (theme.themeBase.isDarkTheme(this)) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            },
        )

        val nameYourPrice =
            intent?.getBooleanExtra(EXTRA_NAME_YOUR_PRICE, true) != false
        if (savedInstanceState == null) {
            firebase.logEvent(
                R.string.event_showed_purchase_dialog,
                R.string.param_source to intent?.getStringExtra(EXTRA_SOURCE).orEmpty(),
            )
        }

        setContent {
            TasksSettingsTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                val signInLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        purchaseComplete()
                    }
                }
                Paywall(
                    nameYourPrice = nameYourPrice,
                    configuration = configuration,
                    inventory = inventory,
                    subscriptionProvider = subscriptionProvider,
                    stripePurchases = stripePurchases,
                    storeBillingProvider = storeBillingProvider,
                    firebase = firebase,
                    onBack = { finish() },
                    onPurchased = { purchaseComplete() },
                    onSignIn = {
                        signInLauncher.launch(
                            Intent(this@PurchaseActivity, SignInActivity::class.java)
                        )
                    },
                )
            }
        }
    }

    private fun purchaseComplete() {
        lifecycleScope.launch {
            inventory.updateTasksAccount()
            maybeTriggerCloudOnboarding(
                inventory.hasTasksSubscription,
                caldavDao,
                tasksPreferences,
                firebase::logCloudOnboarding,
            )
            setResult(RESULT_OK)
            finish()
        }
    }

    companion object {
        const val EXTRA_NAME_YOUR_PRICE = "nameYourPrice"
        const val EXTRA_FEATURE = "feature"
        const val EXTRA_SOURCE = "source"
    }
}
