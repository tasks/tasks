package org.tasks.compose.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProgressBar(showProgress: Boolean) {
    if (showProgress) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
