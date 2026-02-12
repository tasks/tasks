package org.tasks.billing

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.PurchaseScreen
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class PurchaseActivity : AppCompatActivity() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var inventory: Inventory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TasksSettingsTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                PurchaseScreen(
                    onBack = { finish() },
                    onPurchased = {
                        setResult(RESULT_OK)
                        finish()
                    },
                    onSignIn = {
                        setResult(RESULT_OK)
                        finish()
                    },
                    existingSubscriber = inventory.hasPro && !inventory.hasTasksSubscription,
                )
            }
        }
    }
}
