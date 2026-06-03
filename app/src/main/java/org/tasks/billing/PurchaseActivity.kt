package org.tasks.billing

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.analytics.Firebase
import org.tasks.analytics.logCloudOnboarding
import org.tasks.auth.SignInActivity
import org.tasks.compose.PurchaseScreen
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

        setContent {
            TasksSettingsTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                val signInLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        setResult(RESULT_OK)
                        finish()
                    }
                }
                PurchaseScreen(
                    onBack = { finish() },
                    onPurchased = {
                        lifecycleScope.launch {
                            maybeTriggerCloudOnboarding(inventory, caldavDao, tasksPreferences, firebase::logCloudOnboarding)
                            setResult(RESULT_OK)
                            finish()
                        }
                    },
                    onSignIn = {
                        signInLauncher.launch(
                            Intent(this@PurchaseActivity, SignInActivity::class.java)
                        )
                    },
                    existingSubscriber = inventory.hasPro && !inventory.hasTasksSubscription,
                    hasTasksAccount = inventory.hasTasksAccount,
                )
            }
        }
    }
}
