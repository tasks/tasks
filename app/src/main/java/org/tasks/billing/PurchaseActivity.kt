package org.tasks.billing

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.PlatformConfiguration
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class PurchaseActivity : AppCompatActivity() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var configuration: PlatformConfiguration
    @Inject lateinit var stripePurchases: StripePurchases
    @Inject lateinit var subscriptionProvider: SubscriptionProvider
    @Inject lateinit var completion: PaywallCompletion
    @JvmField @Inject var storeBillingProvider: StoreBillingProvider? = null
    @JvmField @Inject var gitHubSponsorClient: GitHubSponsorClient? = null

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
        val source = intent?.getStringExtra(EXTRA_SOURCE).orEmpty()
        if (savedInstanceState == null) {
            firebase.logEvent(
                R.string.event_showed_purchase_dialog,
                R.string.param_source to source,
            )
        }

        setContent {
            TasksSettingsTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                Paywall(
                    nameYourPrice = nameYourPrice,
                    source = source,
                    configuration = configuration,
                    inventory = inventory,
                    subscriptionProvider = subscriptionProvider,
                    stripePurchases = stripePurchases,
                    storeBillingProvider = storeBillingProvider,
                    gitHubSponsorClient = gitHubSponsorClient,
                    firebase = firebase,
                    completion = completion,
                    onBack = { finish() },
                    onPurchased = {
                        setResult(RESULT_OK)
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_NAME_YOUR_PRICE = "nameYourPrice"
        const val EXTRA_SOURCE = "source"
    }
}
