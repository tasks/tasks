package org.tasks.billing

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.PurchaseText.SubscriptionScreen
import org.tasks.extensions.Context.findActivity
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class PurchaseActivity : AppCompatActivity(), OnPurchasesUpdated {
    @Inject lateinit var theme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        theme.applyToContext(this)

        setContent {
            TasksTheme(theme = theme.themeBase.index) {
                BackHandler {
                    finish()
                }
                val viewModel: PurchaseActivityViewModel = viewModel()
                val state = viewModel.viewState.collectAsStateWithLifecycle().value
                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }
                SubscriptionScreen(
                    nameYourPrice = state.nameYourPrice,
                    sliderPosition = state.price,
                    github = state.isGithub,
                    subscribe = { price, isMonthly ->
                        context.findActivity()?.let { viewModel.purchase(it, price, isMonthly) }
                    },
                    setPrice = { viewModel.setPrice(it) },
                    setNameYourPrice = { viewModel.setNameYourPrice(it) },
                    onBack = { finish() },
                    skus = state.skus,
                    snackbarHostState = snackbarHostState,
                )
                LaunchedEffect(state.error) {
                    if (state.error?.isNotBlank() == true) {
                        snackbarHostState.showSnackbar(
                            message = state.error,
                            actionLabel = context.getString(R.string.dismiss),
                            duration = SnackbarDuration.Long,
                        )
                        viewModel.dismissError()
                    }
                }
            }
        }
    }

    override fun onPurchasesUpdated(success: Boolean) {
        if (success) {
            setResult(RESULT_OK)
            finish()
        }
    }
}
