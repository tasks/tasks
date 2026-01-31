package org.tasks.compose

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tasks.R
import org.tasks.billing.PurchaseActivityViewModel
import org.tasks.compose.PurchaseText.SubscriptionScreen
import org.tasks.extensions.Context.findActivity

@Composable
fun PurchaseScreen(
    onBack: () -> Unit,
    onPurchased: () -> Unit = onBack,
) {
    BackHandler { onBack() }
    val viewModel: PurchaseActivityViewModel = hiltViewModel()
    val state = viewModel.viewState.collectAsStateWithLifecycle().value
    LaunchedEffect(state.purchased) {
        if (state.purchased) {
            onPurchased()
        }
    }
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
        onBack = onBack,
        skus = state.skus,
        snackbarHostState = snackbarHostState,
    )
    val dismissLabel = stringResource(R.string.dismiss)
    LaunchedEffect(state.error) {
        if (state.error?.isNotBlank() == true) {
            snackbarHostState.showSnackbar(
                message = state.error,
                actionLabel = dismissLabel,
                duration = SnackbarDuration.Long,
            )
            viewModel.dismissError()
        }
    }
}
