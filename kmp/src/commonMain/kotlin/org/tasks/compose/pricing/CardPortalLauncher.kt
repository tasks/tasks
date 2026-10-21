package org.tasks.compose.pricing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.settings.MessageDialog
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.error_network

@Composable
fun cardPortalLauncher(
    portalUrl: suspend () -> String?,
    openUrl: (String) -> Unit,
): () -> Unit {
    var unavailable by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (unavailable) {
        MessageDialog(
            message = stringResource(Res.string.error_network),
            onDismiss = { unavailable = false },
        )
    }
    return {
        scope.launch {
            val url = portalUrl()
            if (url == null) {
                unavailable = true
            } else {
                openUrl(url)
            }
        }
    }
}
