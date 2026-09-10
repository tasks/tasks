package org.tasks.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.tasks.PlatformConfiguration
import org.tasks.billing.PurchaseState
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.works_with_tasks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorksWithDetail(
    onNavigateBack: () -> Unit,
    onPricingClick: () -> Unit,
    onMcpSettingsClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val configuration = koinInject<PlatformConfiguration>()
    val purchaseState = koinInject<PurchaseState>()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.works_with_tasks)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            WorksWithScreen(
                configuration = configuration,
                hasSubscription = purchaseState.hasPro,
                onLinkClick = { uriHandler.openUri(it) },
                onPricingClick = onPricingClick,
                onMcpSettingsClick = onMcpSettingsClick,
            )
        }
    }
}
